package com.streamdock.pro.data.repository

import android.util.Log
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.streamdock.pro.core.AppConfig
import com.streamdock.pro.core.GsonProvider
import com.streamdock.pro.data.local.db.AppDatabase
import com.streamdock.pro.data.local.db.ButtonEntity
import com.streamdock.pro.data.local.db.ProfileEntity
import com.streamdock.pro.data.local.db.WorkspaceEntity
import com.streamdock.pro.data.models.Button as ButtonModel
import com.streamdock.pro.data.models.Profile
import com.streamdock.pro.data.models.Workspace
import com.streamdock.pro.network.dto.ActionTriggerPayload
import com.streamdock.pro.network.dto.MessageTypes
import com.streamdock.pro.network.dto.WsEnvelope
import com.streamdock.pro.network.websocket.WebSocketClient
import com.streamdock.pro.network.websocket.WebSocketClient.ConnectionState
import com.streamdock.pro.ui.components.IconMapper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gson ignora la nulabilidad de Kotlin (instancia con Unsafe sin pasar por el constructor),
 * así que un campo declarado como no nulo puede llegar null desde el servidor o WinUI.
 */
private fun <T> T.asNullable(): T? = this

class StreamDockRepository(
    val clientUuid: String,
    database: AppDatabase,
) {
    private val _workspace = MutableStateFlow<Workspace?>(null)
    val workspace: StateFlow<Workspace?> = _workspace.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _obsScenes = MutableStateFlow<List<String>>(emptyList())
    val obsScenes: StateFlow<List<String>> = _obsScenes.asStateFlow()

    private val gson = GsonProvider.gson
    private val dao = database.workspaceDao()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Error en operación del repositorio", throwable)
    }
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    /** Cola serial para que todas las operaciones en Room se ejecuten en orden. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val dbDispatcher = Dispatchers.IO.limitedParallelism(1)

    /** Se completa cuando termina la carga inicial desde Room, para no competir con `register_ack`. */
    private val initialLoadDone = CompletableDeferred<Unit>()

    @Volatile private var webSocketClient: WebSocketClient? = null
    @Volatile private var connectionGeneration = 0
    @Volatile private var manualDisconnect = false
    @Volatile private var lastIp: String? = null
    @Volatile private var lastPort: Int = AppConfig.SERVER_PORT
    private var reconnectJob: Job? = null

    init {
        repositoryScope.launch(dbDispatcher) {
            try {
                loadFromDb()
            } finally {
                initialLoadDone.complete(Unit)
            }
        }
    }

    // ==========================================
    // Persistencia local
    // ==========================================
    private suspend fun loadFromDb() {
        val full = dao.getWorkspaceFull(clientUuid) ?: return
        val wsRows = full.workspace.gridRows.takeIf { it > 0 } ?: DEFAULT_ROWS
        val wsCols = full.workspace.gridCols.takeIf { it > 0 } ?: DEFAULT_COLS
        val profiles = full.profiles.map { p ->
            Profile(
                id = p.profile.id,
                name = p.profile.name,
                autoSwitchProcess = p.profile.autoSwitchProcess,
                isDefault = p.profile.isDefault,
                iconName = p.profile.iconName,
                backgroundColor = p.profile.backgroundColor,
                gridRows = p.profile.gridRows.takeIf { it > 0 } ?: wsRows,
                gridCols = p.profile.gridCols.takeIf { it > 0 } ?: wsCols,
            )
        }
        val buttons = full.profiles.flatMap { p ->
            p.buttons.map { b ->
                ButtonModel(
                    id = b.id,
                    profileId = b.profileId,
                    position = b.position,
                    label = b.label,
                    iconName = b.iconName,
                    iconTint = b.iconTint,
                    backgroundColor = b.backgroundColor,
                    fontSize = b.fontSize,
                    actionType = b.actionType,
                    actionValue = b.actionValue,
                    isActiveState = b.isActiveState,
                )
            }
        }
        val local = sanitize(
            Workspace(
                clientUuid = full.workspace.clientUuid,
                version = full.workspace.version,
                activeProfileId = full.workspace.activeProfileId,
                gridRows = wsRows,
                gridCols = wsCols,
                profiles = profiles,
                buttons = buttons,
            ),
        )
        _workspace.compareAndSet(null, local)
    }

    private fun saveToDb(workspace: Workspace) {
        repositoryScope.launch(dbDispatcher) {
            val profileIds = workspace.profiles.map { it.id }.toSet()
            val workspaceEntity = WorkspaceEntity(
                clientUuid = workspace.clientUuid,
                version = workspace.version,
                activeProfileId = workspace.activeProfileId,
                gridRows = workspace.gridRows,
                gridCols = workspace.gridCols,
            )
            val profileEntities = workspace.profiles.map { p ->
                ProfileEntity(
                    id = p.id,
                    workspaceId = workspace.clientUuid,
                    name = p.name,
                    autoSwitchProcess = p.autoSwitchProcess,
                    isDefault = p.isDefault,
                    iconName = p.iconName,
                    backgroundColor = p.backgroundColor,
                    gridRows = p.gridRows,
                    gridCols = p.gridCols,
                )
            }
            // Filtrar botones huérfanos para no violar la FOREIGN KEY de Room.
            val buttonEntities = workspace.buttons
                .filter { it.profileId in profileIds }
                .map { b ->
                    ButtonEntity(
                        id = b.id,
                        profileId = b.profileId,
                        position = b.position,
                        label = b.label,
                        iconName = b.iconName,
                        iconTint = b.iconTint,
                        backgroundColor = b.backgroundColor,
                        fontSize = b.fontSize,
                        actionType = b.actionType,
                        actionValue = b.actionValue,
                        isActiveState = b.isActiveState,
                    )
                }
            dao.saveFullWorkspace(workspaceEntity, profileEntities, buttonEntities)
        }
    }

    fun deleteProfile(profileId: String) {
        repositoryScope.launch(dbDispatcher) {
            dao.deleteProfile(profileId)
        }
    }

    fun deleteButton(profileId: String, position: Int) {
        val current = _workspace.value ?: return
        val compacted = compactProfileButtons(current, profileId, removedPosition = position)
        updateWorkspace(compacted)
    }

    fun moveButton(profileId: String, fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return
        val current = _workspace.value ?: return
        val profile = current.profiles.find { it.id == profileId } ?: return
        val totalCells = (profile.gridRows.coerceAtLeast(1)) * (profile.gridCols.coerceAtLeast(1))
        if (fromPosition !in 0 until totalCells || toPosition !in 0 until totalCells) return

        val otherProfilesButtons = current.buttons.filter { it.profileId != profileId }
        val byPos = current.buttons
            .filter { it.profileId == profileId }
            .associateBy { it.position }
            .toMutableMap()

        val sourceBtn = byPos[fromPosition] ?: ButtonModel(
            id = "btn-$profileId-$fromPosition",
            profileId = profileId,
            position = fromPosition,
            label = "",
            iconName = "none",
            backgroundColor = DEFAULT_PROFILE_BG,
            actionType = "NONE",
            actionValue = "",
        )
        val targetBtn = byPos[toPosition]

        byPos[toPosition] = sourceBtn.copy(
            id = "btn-$profileId-$toPosition",
            position = toPosition,
        )
        if (targetBtn != null) {
            byPos[fromPosition] = targetBtn.copy(
                id = "btn-$profileId-$fromPosition",
                position = fromPosition,
            )
        } else {
            byPos.remove(fromPosition)
        }

        val updatedProfileButtons = byPos.values.sortedBy { it.position }
        updateWorkspace(
            current.copy(
                version = current.version + 1,
                buttons = otherProfilesButtons + updatedProfileButtons,
            ),
        )
    }

    /**
     * Elimina el botón en `removedPosition` y reacomoda (compacta) todos los botones configurados
     * del perfil en posiciones consecutivas `0, 1, 2, ...` para que no queden espacios vacíos entre ellos.
     */
    private fun compactProfileButtons(
        workspace: Workspace,
        profileId: String,
        removedPosition: Int? = null,
    ): Workspace {
        val otherProfilesButtons = workspace.buttons.filter { it.profileId != profileId }
        val configuredButtons = workspace.buttons
            .filter { it.profileId == profileId && it.position != removedPosition && isButtonConfigured(it) }
            .sortedBy { it.position }
            .mapIndexed { newPos, btn ->
                btn.copy(
                    id = "btn-$profileId-$newPos",
                    position = newPos,
                )
            }

        return workspace.copy(
            version = workspace.version + 1,
            buttons = otherProfilesButtons + configuredButtons,
        )
    }

    private fun isButtonConfigured(b: ButtonModel): Boolean {
        val hasAction = b.actionType.isNotBlank() && b.actionType != "NONE"
        val hasLabel = b.label.isNotBlank()
        val hasIcon = b.iconName.isNotBlank() && b.iconName != "none" && b.iconName != "default"
        val bg = b.backgroundColor?.uppercase()
        val hasCustomColor = !bg.isNullOrBlank() && bg != "#1A1B26" && bg != "#1E1E2E"
        return hasAction || hasLabel || hasIcon || hasCustomColor
    }

    fun updateWorkspace(workspace: Workspace) {
        _workspace.value = workspace
        saveToDb(workspace)
        syncToServer(workspace)
    }

    private fun applyRemoteWorkspace(workspace: Workspace) {
        _workspace.value = workspace
        saveToDb(workspace)
    }

    // ==========================================
    // Mensajes entrantes del servidor Rust
    // ==========================================
    private fun handleIncomingMessage(envelope: WsEnvelope) {
        repositoryScope.launch {
            // Esperar a que termine la lectura inicial de Room antes de comparar con lo que envía el servidor.
            initialLoadDone.await()
            processEnvelope(envelope)
        }
    }

    private fun processEnvelope(envelope: WsEnvelope) {
        val type = envelope.type.asNullable() ?: return

        // El servidor Rust hace broadcast a TODOS los sockets conectados: si el mensaje trae un
        // uuid distinto al nuestro, va dirigido a otro dispositivo y debemos ignorarlo.
        val targetUuid = envelope.uuid.asNullable()
        if (!targetUuid.isNullOrEmpty() && targetUuid != clientUuid) {
            return
        }

        val payload = envelope.payload ?: return
        try {
            when (type) {
                MessageTypes.REGISTER_ACK -> {
                    val wsJson = if (payload.has("workspace")) payload.get("workspace") else payload
                    val incoming = parseWorkspace(wsJson) ?: return
                    val current = _workspace.value

                    // El servidor Rust guarda en RAM: al reiniciarse crea un workspace vacío
                    // ("Default Profile", version=1, buttons=[]). Si nosotros ya tenemos datos en
                    // Room, subimos nuestra copia al servidor en vez de borrarla con el default vacío.
                    val incomingIsFreshServerDefault =
                        incoming.version <= 1L && incoming.buttons.isEmpty() && incoming.profiles.size <= 1
                    val localHasData = current != null && (current.buttons.isNotEmpty() || current.profiles.size > 1)

                    if (current == null) {
                        applyRemoteWorkspace(incoming)
                    } else if (localHasData && (incomingIsFreshServerDefault || current.version > incoming.version)) {
                        syncToServer(current)
                    } else if (incoming.version >= current.version) {
                        applyRemoteWorkspace(pruneReplacedProfiles(current, incoming))
                    } else {
                        syncToServer(current)
                    }
                }

                MessageTypes.DB_SYNC, MessageTypes.DB_SYNC_ACK -> {
                    when {
                        // Caso 1: Workspace completo (enviado por WinUI o retransmitido por Rust)
                        payload.has("workspace") -> {
                            val incoming = parseWorkspace(payload.get("workspace")) ?: return
                            val current = _workspace.value
                            // Si es exactamente el eco de nuestro propio envío, no hace falta reescribir la DB.
                            if (current != null && incoming == current) return

                            val merged = if (current != null) {
                                // WinUI envía `version = 1` fijo al crear perfiles: garantizamos que la versión
                                // avance para mantener coherencia con próximas sincronizaciones.
                                val nextVersion = maxOf(incoming.version, current.version)
                                pruneReplacedProfiles(current, incoming.copy(version = nextVersion))
                            } else {
                                incoming
                            }
                            applyRemoteWorkspace(merged)
                        }
                        // Caso 2: Botón individual (actualización o eliminación desde WinUI/Rust)
                        payload.has("button") -> {
                            val updatedBtn = parseButton(payload.get("button")) ?: return
                            val current = _workspace.value ?: return
                            val targetProfileId = updatedBtn.profileId.takeIf { id ->
                                current.profiles.any { it.id == id }
                            } ?: current.activeProfileId

                            val action = payload.get("action")?.takeIf { it.isJsonPrimitive }?.asString
                            val isDeleted = action == "delete_button" ||
                                (updatedBtn.actionType == "NONE" && updatedBtn.label.isBlank() && updatedBtn.iconName == "none")

                            val updatedWs = if (isDeleted) {
                                compactProfileButtons(current, targetProfileId, removedPosition = updatedBtn.position)
                            } else {
                                val normalizedBtn = updatedBtn.copy(profileId = targetProfileId)
                                val exists = current.buttons.any {
                                    it.profileId == targetProfileId && it.position == normalizedBtn.position
                                }
                                val updatedButtons = if (exists) {
                                    current.buttons.map { b ->
                                        if (b.profileId == targetProfileId && b.position == normalizedBtn.position) {
                                            normalizedBtn
                                        } else {
                                            b
                                        }
                                    }
                                } else {
                                    current.buttons + normalizedBtn
                                }
                                current.copy(
                                    version = current.version + 1,
                                    buttons = updatedButtons,
                                )
                            }
                            applyRemoteWorkspace(updatedWs)
                        }
                    }
                }

                MessageTypes.OBS_SCENES_LIST -> {
                    val scenesJson = payload.get("scenes")
                    val scenes = if (scenesJson != null && scenesJson.isJsonArray) {
                        gson.fromJson(scenesJson, Array<String?>::class.java)?.filterNotNull()
                    } else {
                        null
                    }
                    _obsScenes.value = scenes.orEmpty()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando mensaje '$type'", e)
        }
    }

    /**
     * Si el workspace remoto eliminó perfiles que existían localmente, los borra también de Room
     * (de lo contrario `insertProfiles` con REPLACE dejaría los perfiles viejos en la base de datos).
     * Además conserva las dimensiones de cuadrícula de cada perfil si el servidor no las envía
     * (en Rust `Profile` no tiene `grid_rows`/`grid_cols`).
     */
    private fun pruneReplacedProfiles(previous: Workspace, incoming: Workspace): Workspace {
        val incomingIds = incoming.profiles.map { it.id }.toSet()
        previous.profiles.forEach { old ->
            if (old.id !in incomingIds) deleteProfile(old.id)
        }
        val previousById = previous.profiles.associateBy { it.id }
        val mergedProfiles = incoming.profiles.map { p ->
            val prev = previousById[p.id]
            if (prev != null) {
                p.copy(gridRows = prev.gridRows, gridCols = prev.gridCols)
            } else {
                p
            }
        }
        return incoming.copy(profiles = mergedProfiles)
    }

    private fun parseWorkspace(json: JsonElement?): Workspace? {
        if (json == null || !json.isJsonObject) return null
        val obj = json.asJsonObject
        val raw = gson.fromJson(obj, Workspace::class.java) ?: return null

        // En Rust `Profile` no incluye `grid_rows`/`grid_cols`, por lo que al deserializar con Gson
        // quedan con el valor del constructor por defecto o 0. Si el JSON del perfil no los trae,
        // heredan las dimensiones del Workspace (`grid_rows = 4, grid_cols = 5` en el servidor).
        val wsRows = raw.gridRows.takeIf { it > 0 } ?: DEFAULT_ROWS
        val wsCols = raw.gridCols.takeIf { it > 0 } ?: DEFAULT_COLS
        val profilesJson = obj.getAsJsonArray("profiles")

        val fixedProfiles = raw.profiles.asNullable().orEmpty().mapIndexedNotNull { index, p ->
            val pObj = profilesJson?.getOrNull(index)?.takeIf { it.isJsonObject }?.asJsonObject
            val hasRows = pObj?.has("grid_rows") == true
            val hasCols = pObj?.has("grid_cols") == true
            p.asNullable()?.copy(
                gridRows = if (hasRows) p.gridRows else wsRows,
                gridCols = if (hasCols) p.gridCols else wsCols,
            )
        }
        return sanitize(raw.copy(gridRows = wsRows, gridCols = wsCols, profiles = fixedProfiles))
    }

    private fun com.google.gson.JsonArray.getOrNull(index: Int): JsonElement? =
        if (index in 0 until size()) get(index) else null

    private fun parseButton(json: JsonElement?): ButtonModel? {
        if (json == null || !json.isJsonObject) return null
        val raw = gson.fromJson(json, ButtonModel::class.java) ?: return null
        return sanitizeButton(raw)
    }

    /** Normaliza un workspace: rellena nulos, corrige dimensiones inválidas y vincula botones. */
    private fun sanitize(raw: Workspace): Workspace {
        val wsRows = raw.gridRows.takeIf { it > 0 } ?: DEFAULT_ROWS
        val wsCols = raw.gridCols.takeIf { it > 0 } ?: DEFAULT_COLS

        val profiles = raw.profiles.asNullable().orEmpty().mapNotNull { p ->
            val id = p.asNullable()?.id.asNullable()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Profile(
                id = id,
                name = p.name.asNullable()?.takeIf { it.isNotBlank() } ?: "Página",
                autoSwitchProcess = p.autoSwitchProcess,
                isDefault = p.isDefault,
                iconName = p.iconName.asNullable()?.takeIf { it.isNotBlank() } ?: "home",
                backgroundColor = p.backgroundColor.asNullable()?.takeIf { it.isNotBlank() } ?: DEFAULT_PROFILE_BG,
                gridRows = p.gridRows.takeIf { it > 0 } ?: wsRows,
                gridCols = p.gridCols.takeIf { it > 0 } ?: wsCols,
            )
        }
        val profileIds = profiles.map { it.id }.toSet()
        val fallbackProfileId = profiles.firstOrNull()?.id

        val buttons = raw.buttons.asNullable().orEmpty()
            .mapNotNull { b -> b.asNullable()?.let { sanitizeButton(it, fallbackProfileId) } }
            .filter { it.profileId in profileIds }

        val activeProfileId = raw.activeProfileId.asNullable()
            ?.takeIf { id -> id in profileIds }
            ?: fallbackProfileId
            ?: ""

        return Workspace(
            clientUuid = clientUuid,
            version = raw.version.coerceAtLeast(1L),
            activeProfileId = activeProfileId,
            gridRows = wsRows,
            gridCols = wsCols,
            profiles = profiles,
            buttons = buttons,
        )
    }

    private fun sanitizeButton(b: ButtonModel, fallbackProfileId: String? = null): ButtonModel? {
        val pos = b.position.coerceIn(0, 255) // En Rust `position` es `u8` (0..255)
        val profileId = b.profileId.asNullable()?.takeIf { it.isNotBlank() }
            ?: fallbackProfileId
            ?: return null
        val id = b.id.asNullable()?.takeIf { it.isNotBlank() } ?: "btn-$profileId-$pos"
        val actionType = b.actionType.asNullable()?.takeIf { it.isNotBlank() } ?: "NONE"
        val actionValue = b.actionValue.asNullable() ?: ""
        val rawIcon = b.iconName.asNullable()?.trim().orEmpty()
        val resolvedIcon = if (rawIcon.isEmpty() || rawIcon == "default" || rawIcon == "none") {
            IconMapper.inferIconNameForAction(actionType, actionValue) ?: rawIcon.ifEmpty { "none" }
        } else {
            rawIcon
        }
        return ButtonModel(
            id = id,
            profileId = profileId,
            position = pos,
            label = b.label.asNullable() ?: "",
            iconName = resolvedIcon,
            iconTint = b.iconTint,
            backgroundColor = b.backgroundColor,
            fontSize = b.fontSize?.coerceIn(1, 255), // En Rust `font_size` es `Option<u8>`
            actionType = actionType,
            actionValue = actionValue,
            isActiveState = b.isActiveState,
        )
    }

    // ==========================================
    // Mensajes salientes
    // ==========================================
    private fun syncToServer(workspace: Workspace) {
        val envelope = WsEnvelope(
            type = MessageTypes.DB_SYNC,
            uuid = clientUuid,
            timestamp = System.currentTimeMillis(),
            payload = JsonObject().apply {
                add("workspace", gson.toJsonTree(workspace))
            },
        )
        webSocketClient?.sendEnvelope(envelope)
    }

    fun requestObsScenes() {
        val envelope = WsEnvelope(
            type = MessageTypes.OBS_GET_SCENES,
            uuid = clientUuid,
            timestamp = System.currentTimeMillis(),
            payload = JsonObject(),
        )
        webSocketClient?.sendEnvelope(envelope)
    }

    fun sendAction(profileId: String, position: Int, actionType: String, actionValue: String) {
        val payload = ActionTriggerPayload(
            clientUuid = clientUuid,
            profileId = profileId,
            position = position.coerceIn(0, 255),
            actionType = actionType,
            actionValue = actionValue,
        )
        webSocketClient?.sendAction(payload)
    }

    // ==========================================
    // Gestión de la conexión
    // ==========================================
    @Synchronized
    fun connect(ip: String? = null, port: Int = AppConfig.SERVER_PORT) {
        manualDisconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        lastIp = ip
        lastPort = port
        openConnection()
    }

    @Synchronized
    private fun openConnection() {
        webSocketClient?.disconnect()

        val generation = ++connectionGeneration
        val targetIp = lastIp ?: AppConfig.SERVER_IP
        val serverUrl = "ws://$targetIp:$lastPort/ws"

        val client = WebSocketClient(
            clientUuid,
            onConnectionStateChanged = { state ->
                if (generation == connectionGeneration) onClientStateChanged(state)
            },
            onMessageReceived = { envelope ->
                if (generation == connectionGeneration) handleIncomingMessage(envelope)
            },
        )
        webSocketClient = client
        client.connect(serverUrl)
    }

    private fun onClientStateChanged(state: ConnectionState) {
        _connectionState.value = state
        if ((state == ConnectionState.ERROR || state == ConnectionState.DISCONNECTED) && !manualDisconnect) {
            scheduleReconnect()
        }
    }

    @Synchronized
    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        val generation = connectionGeneration
        reconnectJob = repositoryScope.launch {
            delay(AppConfig.RECONNECT_DELAY_MS)
            if (!manualDisconnect && generation == connectionGeneration) {
                openConnection()
            }
        }
    }

    @Synchronized
    fun disconnect() {
        manualDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        connectionGeneration++
        webSocketClient?.disconnect()
        webSocketClient = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    companion object {
        private const val TAG = "StreamDockRepository"
        /** Coincide con `Workspace::new_default` en Rust y con la cuadrícula de 20 botones (5x4) de WinUI. */
        const val DEFAULT_ROWS = 5
        const val DEFAULT_COLS = 4
        const val DEFAULT_PROFILE_BG = "#1A1B26"
    }
}
