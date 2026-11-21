package com.streamdock.pro.network.dto

import com.google.gson.JsonObject

data class WsEnvelope(
    val type: String,
    val uuid: String?,
    val timestamp: Long? = System.currentTimeMillis(),
    val payload: JsonObject? = null
)

// Tipos de mensaje constantes
object MessageTypes {
    const val REGISTER_CLIENT = "register_client"
    const val REGISTER_ACK = "register_ack"
    const val ACTION = "action"
    const val DB_SYNC = "db_sync"
    const val DB_SYNC_ACK = "db_sync_ack"
    
    // OBS Specific
    const val OBS_GET_SCENES = "obs_get_scenes"
    const val OBS_SCENES_LIST = "obs_scenes_list"
}

data class ActionTriggerPayload(
    val clientUuid: String,
    val profileId: String,
    val position: Int,
    val actionType: String,
    val actionValue: String
)
