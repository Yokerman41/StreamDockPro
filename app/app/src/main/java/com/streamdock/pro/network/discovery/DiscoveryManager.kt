package com.streamdock.pro.network.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.streamdock.pro.core.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class DiscoveryManager(context: Context) {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredServices = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServices: StateFlow<List<DiscoveredServer>> = _discoveredServices.asStateFlow()

    /** Estado global del descubrimiento (controlado sólo por start/stop, no por callbacks de NSD). */
    private val isRunning = AtomicBoolean(false)
    private var discoveryScope: CoroutineScope? = null

    /**
     * Se crea un listener nuevo en cada arranque: NsdManager no permite reutilizar un listener
     * hasta que se confirma su parada (asíncrona), lo que hacía fallar "refrescar".
     */
    @Volatile
    private var activeDiscoveryListener: NsdManager.DiscoveryListener? = null

    private val isResolving = AtomicBoolean(false)
    private val resolveQueue = ConcurrentLinkedQueue<NsdServiceInfo>()

    data class DiscoveredServer(
        val name: String,
        val host: String,
        val port: Int,
    )

    private fun addOrUpdateServer(server: DiscoveredServer) {
        var isNew = false
        _discoveredServices.update { current ->
            val index = current.indexOfFirst { it.host == server.host && it.port == server.port }
            if (index >= 0) {
                isNew = false
                current.toMutableList().apply { set(index, server) }
            } else {
                isNew = true
                current + server
            }
        }
        if (isNew) Log.i(TAG, "✓ Discovered server: ${server.name} at ${server.host}:${server.port}")
    }

    // ==========================================
    // 1. mDNS / NSD Listener & Safe Resolver
    // ==========================================
    private fun createDiscoveryListener() = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "mDNS Discovery started for type: $regType")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(TAG, "mDNS Service found: ${service.serviceName}, Type=${service.serviceType}")
            val type = service.serviceType.lowercase()
            if (type.contains("streamdock") || type.contains("streamdeck")) {
                enqueueResolve(service)
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.d(TAG, "mDNS Service lost: ${service.serviceName}")
            _discoveredServices.update { list -> list.filter { it.name != service.serviceName } }
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "mDNS Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "mDNS Start Discovery failed: Error code $errorCode")
            // NsdManager ya descartó este listener: no hay que llamar a stopServiceDiscovery con él.
            if (activeDiscoveryListener === this) activeDiscoveryListener = null
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "mDNS Stop Discovery failed: Error code $errorCode")
        }
    }

    private fun enqueueResolve(serviceInfo: NsdServiceInfo) {
        resolveQueue.add(serviceInfo)
        processNextResolve()
    }

    @Suppress("DEPRECATION") // resolveService es la única opción por debajo de API 34
    private fun processNextResolve() {
        if (!isResolving.compareAndSet(false, true)) return

        val next = resolveQueue.poll()
        if (next == null || !isRunning.get()) {
            isResolving.set(false)
            return
        }

        try {
            nsdManager.resolveService(
                next,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                        isResolving.set(false)
                        processNextResolve()
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val hostAddress = serviceInfo.host?.hostAddress.orEmpty()
                        if (hostAddress.isNotEmpty() && hostAddress != "0.0.0.0") {
                            addOrUpdateServer(
                                DiscoveredServer(
                                    name = serviceInfo.serviceName,
                                    host = hostAddress,
                                    port = serviceInfo.port,
                                ),
                            )
                        }
                        isResolving.set(false)
                        processNextResolve()
                    }
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calling resolveService", e)
            isResolving.set(false)
            processNextResolve()
        }
    }

    // ==========================================
    // 2. UDP Broadcast Discovery Engine
    // ==========================================
    /**
     * Se usa UN solo socket para enviar los sondeos y recibir: así las respuestas directas del
     * servidor (dirigidas al puerto de origen del sondeo) llegan al mismo socket que escucha.
     */
    private fun startUdpBroadcastDiscovery(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val socket = openUdpSocket() ?: return@launch
            try {
                // Receptor: respuestas a los sondeos y beacons periódicos del servidor
                launch(Dispatchers.IO) { receiveLoop(socket) }

                // Emisor: sondeos periódicos
                val broadcastAddresses = getBroadcastAddresses()
                val probeMessage = PROBE_MESSAGE.toByteArray(Charsets.UTF_8)
                while (isActive) {
                    for (addr in broadcastAddresses) {
                        try {
                            socket.send(DatagramPacket(probeMessage, probeMessage.size, addr, DISCOVERY_PORT))
                        } catch (e: Exception) {
                            Log.v(TAG, "UDP probe to $addr failed: ${e.message}")
                        }
                    }
                    delay(PROBE_INTERVAL_MS)
                }
            } finally {
                socket.close() // Desbloquea el receive() del receptor
            }
        }
    }

    private fun openUdpSocket(): DatagramSocket? {
        // Preferimos el puerto de descubrimiento para recibir también los beacons broadcast.
        val preferred = DatagramSocket(null)
        try {
            preferred.reuseAddress = true
            preferred.broadcast = true
            preferred.soTimeout = RECEIVE_TIMEOUT_MS
            preferred.bind(InetSocketAddress(DISCOVERY_PORT))
            return preferred
        } catch (e: Exception) {
            preferred.close()
            Log.w(TAG, "Port $DISCOVERY_PORT busy, using ephemeral port (direct replies only)", e)
        }
        return try {
            DatagramSocket().apply {
                broadcast = true
                soTimeout = RECEIVE_TIMEOUT_MS
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not open UDP discovery socket", e)
            null
        }
    }

    private fun CoroutineScope.receiveLoop(socket: DatagramSocket) {
        val buffer = ByteArray(2048)
        while (isActive && !socket.isClosed) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                handleUdpPacket(packet)
            } catch (_: SocketTimeoutException) {
                // Sin datos: se vuelve a comprobar si sigue activo
            } catch (e: Exception) {
                if (!socket.isClosed) Log.w(TAG, "UDP receive error: ${e.message}")
            }
        }
    }

    private fun handleUdpPacket(packet: DatagramPacket) {
        val text = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
        if (!text.startsWith("{") || !text.endsWith("}")) return // p.ej. nuestro propio sondeo

        try {
            val json = JSONObject(text)
            if (json.optString("service") != "streamdock") return

            val senderHost = packet.address?.hostAddress.orEmpty()
            val advertisedHost = json.optString("host")
            val host = advertisedHost.takeUnless { it.isBlank() || it == "0.0.0.0" } ?: senderHost
            val name = json.optString("name", "Stream Dock Server")
            val port = json.optInt("port", AppConfig.SERVER_PORT)

            if (host.isNotEmpty()) addOrUpdateServer(DiscoveredServer(name, host, port))
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing UDP beacon JSON", e)
        }
    }

    @Suppress("DEPRECATION") // dhcpInfo: alternativa moderna requiere ConnectivityManager/LinkProperties
    private fun getBroadcastAddresses(): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()
        try {
            addresses.add(InetAddress.getByName("255.255.255.255"))
            val dhcp = wifiManager.dhcpInfo
            if (dhcp != null && dhcp.ipAddress != 0) {
                val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
                val quads = ByteArray(4) { k -> ((broadcast shr (k * 8)) and 0xFF).toByte() }
                addresses.add(InetAddress.getByAddress(quads))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating broadcast addresses", e)
        }
        return addresses.distinct()
    }

    // ==========================================
    // Lifecycle Management
    // ==========================================
    fun startDiscovery() {
        if (!isRunning.compareAndSet(false, true)) {
            Log.d(TAG, "Discovery already active")
            return
        }

        _discoveredServices.value = emptyList()
        resolveQueue.clear()
        isResolving.set(false)

        acquireMulticastLock()

        // 1. mDNS (listener nuevo en cada arranque)
        val listener = createDiscoveryListener()
        try {
            nsdManager.discoverServices(SERVICE_TYPE_PRIMARY, NsdManager.PROTOCOL_DNS_SD, listener)
            activeDiscoveryListener = listener
        } catch (e: Exception) {
            Log.e(TAG, "Error starting mDNS discovery", e)
        }

        // 2. UDP broadcast (independiente del resultado de mDNS)
        discoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scope ->
            startUdpBroadcastDiscovery(scope)
        }
    }

    fun stopDiscovery() {
        if (!isRunning.compareAndSet(true, false)) return

        discoveryScope?.cancel()
        discoveryScope = null
        resolveQueue.clear()

        activeDiscoveryListener?.let { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.w(TAG, "mDNS discovery stop error (likely already stopped): ${e.message}")
            }
        }
        activeDiscoveryListener = null
        releaseMulticastLock()
    }

    private fun acquireMulticastLock() {
        try {
            val lock = multicastLock ?: wifiManager.createMulticastLock("StreamDockDiscoveryLock").apply {
                // Sin conteo de referencias: un único release() siempre libera el lock.
                setReferenceCounted(false)
            }.also { multicastLock = it }
            lock.acquire()
            Log.d(TAG, "MulticastLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Could not acquire MulticastLock", e)
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "MulticastLock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MulticastLock", e)
        }
    }

    companion object {
        private const val TAG = "DiscoveryManager"
        private const val SERVICE_TYPE_PRIMARY = "_streamdock._tcp"
        private const val DISCOVERY_PORT = 8082
        private const val PROBE_MESSAGE = "STREAMDOCK_DISCOVER"
        private const val PROBE_INTERVAL_MS = 2000L
        private const val RECEIVE_TIMEOUT_MS = 1000
    }
}
