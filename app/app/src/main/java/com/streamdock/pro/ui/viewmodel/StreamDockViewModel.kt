package com.streamdock.pro.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamdock.pro.data.local.ConnectionSettingsManager
import com.streamdock.pro.data.models.Button as ButtonModel
import com.streamdock.pro.data.models.Profile
import com.streamdock.pro.data.models.Workspace
import com.streamdock.pro.data.repository.StreamDockRepository
import com.streamdock.pro.network.discovery.DiscoveryManager
import com.streamdock.pro.network.websocket.WebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class StreamDockViewModel(
    application: Application,
    private val repository: StreamDockRepository,
) : AndroidViewModel(application) {

    val workspace: StateFlow<Workspace?> = repository.workspace
    val connectionState: StateFlow<WebSocketClient.ConnectionState> = repository.connectionState
    val obsScenes: StateFlow<List<String>> = repository.obsScenes
    val processes = repository.processes

    private val settingsManager = ConnectionSettingsManager(application)
    private val discoveryManager = DiscoveryManager(application)

    val discoveredServers = discoveryManager.discoveredServices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /** Observable: antes se leía de SharedPreferences en cada composición y la UI no se actualizaba. */
    private val _connectionMode = MutableStateFlow(settingsManager.getConnectionMode())
    val connectionMode: StateFlow<String> = _connectionMode.asStateFlow()

    private var scanTimeoutJob: Job? = null

    init {
        // El ViewModel sobrevive a la recreación de la Activity: se conecta una sola vez.
        connect()
    }

    fun connect() {
        val currentState = connectionState.value
        if (currentState == WebSocketClient.ConnectionState.CONNECTED ||
            currentState == WebSocketClient.ConnectionState.CONNECTING ||
            currentState == WebSocketClient.ConnectionState.REGISTERING
        ) {
            return
        }
        connectForCurrentMode()
    }

    private fun connectForCurrentMode() {
        if (_connectionMode.value == ConnectionSettingsManager.MODE_USB) {
            stopDiscovery()
            repository.connect() // Usa 127.0.0.1 (adb reverse)
        } else {
            settingsManager.getLastKnownIp()?.let { ip ->
                repository.connect(ip, settingsManager.getLastKnownPort())
            }
            startDiscovery()
        }
    }

    fun refreshScan() {
        if (_connectionMode.value == ConnectionSettingsManager.MODE_USB) {
            // En USB simplemente intentamos reconectar
            connect()
        } else {
            stopDiscovery()
            startDiscovery()
        }
    }

    fun setConnectionMode(mode: String) {
        if (mode == _connectionMode.value) return
        settingsManager.setConnectionMode(mode)
        _connectionMode.value = mode

        // Al cambiar de modo se abandona la conexión anterior (antes una conexión USB activa
        // seguía viva en modo WiFi y viceversa, porque connect() salía si ya estaba conectado).
        if (mode == ConnectionSettingsManager.MODE_WIFI && settingsManager.getLastKnownIp() == null) {
            repository.disconnect()
        }
        connectForCurrentMode()
    }

    fun connectToWifiServer(ip: String, port: Int) {
        settingsManager.setLastKnownServer(ip, port)
        repository.connect(ip, port)
    }

    fun connectToUsbServer() {
        repository.connect() // Siempre usa 127.0.0.1
    }

    private fun startDiscovery() {
        discoveryManager.startDiscovery()
        _isScanning.value = true
        // El escaneo se detiene solo; antes el indicador giraba indefinidamente y nunca se
        // mostraba el botón de "volver a buscar" ni el mensaje de "no se encontraron servidores".
        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            delay(SCAN_DURATION_MS)
            stopDiscovery()
        }
    }

    private fun stopDiscovery() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        _isScanning.value = false
        discoveryManager.stopDiscovery()
    }

    fun fetchObsScenes() {
        repository.requestObsScenes()
    }

    fun fetchProcesses() {
        repository.requestProcesses()
    }

    fun killProcess(pid: Long) {
        repository.killProcess(pid)
    }

    fun switchToScene(sceneName: String) {
        repository.sendAction(
            profileId = "quick_action",
            position = 0,
            actionType = ACTION_OBS_SCENE_SWITCH,
            actionValue = sceneName,
        )
    }

    fun triggerQuickAction(actionType: String, actionValue: String) {
        repository.sendAction(
            profileId = "quick_action",
            position = 0,
            actionType = actionType,
            actionValue = actionValue,
        )
    }

    fun createNewPage(name: String, rows: Int = DEFAULT_ROWS, cols: Int = DEFAULT_COLS) {
        val safeRows = rows.coerceIn(1, MAX_GRID_SIZE)
        val safeCols = cols.coerceIn(1, MAX_GRID_SIZE)
        val currentWorkspace = workspace.value
        val newProfileId = "prof-${UUID.randomUUID()}"
        val newProfile = Profile(
            id = newProfileId,
            name = name.trim(),
            isDefault = currentWorkspace?.profiles.isNullOrEmpty(),
            iconName = "apps",
            backgroundColor = StreamDockRepository.DEFAULT_PROFILE_BG,
            gridRows = safeRows,
            gridCols = safeCols,
        )

        // Generar botones para el nuevo perfil según las dimensiones elegidas
        val newButtons = (0 until (safeRows * safeCols)).map { pos -> emptyButton(newProfileId, pos) }

        val updatedWorkspace = currentWorkspace?.copy(
            version = currentWorkspace.version + 1,
            activeProfileId = newProfileId,
            gridRows = safeRows,
            gridCols = safeCols,
            profiles = currentWorkspace.profiles + newProfile,
            buttons = currentWorkspace.buttons + newButtons,
        ) ?: Workspace(
            clientUuid = repository.clientUuid,
            version = 1L,
            activeProfileId = newProfileId,
            gridRows = safeRows,
            gridCols = safeCols,
            profiles = listOf(newProfile),
            buttons = newButtons,
        )

        repository.updateWorkspace(updatedWorkspace)
    }

    fun selectPage(profileId: String) {
        val current = workspace.value ?: return
        if (current.activeProfileId == profileId) return
        if (current.profiles.none { it.id == profileId }) return
        repository.updateWorkspace(
            current.copy(
                version = current.version + 1,
                activeProfileId = profileId,
            ),
        )
    }

    fun deleteActiveProfile() {
        val currentWs = workspace.value ?: return
        val profileIdToDelete = currentWs.activeProfileId

        // No permitir borrar si es el único perfil
        if (currentWs.profiles.size <= 1) return

        val updatedProfiles = currentWs.profiles.filter { it.id != profileIdToDelete }
        val updatedButtons = currentWs.buttons.filter { it.profileId != profileIdToDelete }
        val nextProfileId = updatedProfiles.firstOrNull()?.id ?: ""

        val updatedWs = currentWs.copy(
            version = currentWs.version + 1,
            activeProfileId = nextProfileId,
            profiles = updatedProfiles,
            buttons = updatedButtons,
        )

        // Ambas operaciones se encolan en orden en la cola serial de la DB
        repository.deleteProfile(profileIdToDelete)
        repository.updateWorkspace(updatedWs)
    }

    fun updateButton(
        profileId: String,
        position: Int,
        label: String,
        icon: String,
        color: String,
        actionType: String,
        actionValue: String,
    ) {
        val currentWs = workspace.value ?: return
        if (currentWs.profiles.none { it.id == profileId }) return

        val exists = currentWs.buttons.any { it.profileId == profileId && it.position == position }
        val updatedButtons = if (exists) {
            currentWs.buttons.map { button ->
                if (button.profileId == profileId && button.position == position) {
                    button.copy(
                        label = label,
                        iconName = icon,
                        backgroundColor = color,
                        actionType = actionType,
                        actionValue = actionValue,
                    )
                } else {
                    button
                }
            }
        } else {
            // Antes la configuración de una celda sin botón previo se perdía en silencio.
            currentWs.buttons + emptyButton(profileId, position).copy(
                label = label,
                iconName = icon,
                backgroundColor = color,
                actionType = actionType,
                actionValue = actionValue,
            )
        }

        repository.updateWorkspace(
            currentWs.copy(
                version = currentWs.version + 1,
                buttons = updatedButtons,
            ),
        )
    }

    fun deleteButton(profileId: String, position: Int) {
        repository.deleteButton(profileId, position)
    }

    fun moveButton(profileId: String, fromPosition: Int, toPosition: Int) {
        repository.moveButton(profileId, fromPosition, toPosition)
    }

    fun onButtonPress(position: Int) {
        val currentWorkspace = workspace.value ?: return
        val activeProfileId = currentWorkspace.activeProfileId

        // Buscar la configuración real del botón en el perfil activo
        val button = currentWorkspace.buttons.find {
            it.profileId == activeProfileId && it.position == position
        } ?: return

        when (button.actionType) {
            ACTION_NONE -> Unit
            // La navegación entre páginas es local: no tiene sentido enviarla al servidor.
            ACTION_NAVIGATE -> {
                val target = button.actionValue.trim()
                val profile = currentWorkspace.profiles.find { it.id == target }
                    ?: currentWorkspace.profiles.find { it.name.equals(target, ignoreCase = true) }
                profile?.let { selectPage(it.id) }
            }
            else -> repository.sendAction(
                profileId = activeProfileId,
                position = position,
                actionType = button.actionType,
                actionValue = button.actionValue,
            )
        }
    }

    private fun emptyButton(profileId: String, position: Int) = ButtonModel(
        id = UUID.randomUUID().toString(),
        profileId = profileId,
        position = position,
        label = "",
        iconName = "none",
        actionType = ACTION_NONE,
        actionValue = "",
    )

    override fun onCleared() {
        super.onCleared()
        stopDiscovery() // Libera el MulticastLock y detiene los sockets UDP
        repository.disconnect()
    }

    companion object {
        const val ACTION_NONE = "NONE"
        const val ACTION_NAVIGATE = "NAVIGATE"
        const val ACTION_OBS_SCENE_SWITCH = "OBS_SCENE_SWITCH"
        const val DEFAULT_ROWS = StreamDockRepository.DEFAULT_ROWS
        const val DEFAULT_COLS = StreamDockRepository.DEFAULT_COLS
        const val MAX_GRID_SIZE = 10
        private const val SCAN_DURATION_MS = 15_000L
    }
}
