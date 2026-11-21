package com.streamdock.pro.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val clientUuid: String,
    val version: Long,
    val activeProfileId: String,
    val gridRows: Int,
    val gridCols: Int
)

@Entity(
    tableName = "profiles",
    foreignKeys = [
        ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["clientUuid"],
            childColumns = ["workspaceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workspaceId"])]
)
data class ProfileEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val autoSwitchProcess: String? = null,
    val isDefault: Boolean = false,
    val iconName: String,
    val backgroundColor: String,
    val gridRows: Int,
    val gridCols: Int
)

@Entity(
    tableName = "buttons",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class ButtonEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val position: Int,
    val label: String,
    val iconName: String,
    val iconTint: String? = null,
    val backgroundColor: String? = null,
    val fontSize: Int? = null,
    val actionType: String,
    val actionValue: String,
    val isActiveState: Boolean = false
)
