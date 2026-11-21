package com.streamdock.pro.data.models

import com.streamdock.pro.data.repository.StreamDockRepository

data class Button(
    val id: String,
    val profileId: String,
    val position: Int,
    val label: String,
    val iconName: String,
    val iconTint: String? = null,
    val backgroundColor: String? = null,
    val fontSize: Int? = null,
    val actionType: String,
    val actionValue: String,
    val isPressed: Boolean = false,
    val isActiveState: Boolean = false,
)

data class Profile(
    val id: String,
    val name: String,
    val autoSwitchProcess: String? = null,
    val isDefault: Boolean = false,
    val iconName: String,
    val backgroundColor: String,
    val gridRows: Int = StreamDockRepository.DEFAULT_ROWS,
    val gridCols: Int = StreamDockRepository.DEFAULT_COLS,
)

data class Workspace(
    val clientUuid: String,
    val version: Long,
    val activeProfileId: String,
    val gridRows: Int = StreamDockRepository.DEFAULT_ROWS,
    val gridCols: Int = StreamDockRepository.DEFAULT_COLS,
    val profiles: List<Profile>,
    val buttons: List<Button>,
)
