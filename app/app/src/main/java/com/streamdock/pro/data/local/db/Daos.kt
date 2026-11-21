package com.streamdock.pro.data.local.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction

data class WorkspaceWithProfilesAndButtons(
    @Embedded val workspace: WorkspaceEntity,
    @Relation(
        entity = ProfileEntity::class,
        parentColumn = "clientUuid",
        entityColumn = "workspaceId",
    )
    val profiles: List<ProfileWithButtons>,
)

data class ProfileWithButtons(
    @Embedded val profile: ProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "profileId",
    )
    val buttons: List<ButtonEntity>,
)

@Dao
interface WorkspaceDao {
    @Transaction
    @Query("SELECT * FROM workspaces WHERE clientUuid = :uuid")
    suspend fun getWorkspaceFull(uuid: String): WorkspaceWithProfilesAndButtons?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButtons(buttons: List<ButtonEntity>)

    @Query("DELETE FROM buttons WHERE profileId IN (SELECT id FROM profiles WHERE workspaceId = :workspaceId)")
    suspend fun deleteAllButtonsForWorkspace(workspaceId: String)

    @Transaction
    suspend fun saveFullWorkspace(
        workspace: WorkspaceEntity,
        profiles: List<ProfileEntity>,
        buttons: List<ButtonEntity>,
    ) {
        insertWorkspace(workspace)
        insertProfiles(profiles)
        deleteAllButtonsForWorkspace(workspace.clientUuid)
        insertButtons(buttons)
    }

    @Query("DELETE FROM workspaces")
    suspend fun clearAll()

    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun deleteProfile(profileId: String)

    @Query("DELETE FROM buttons WHERE profileId = :profileId AND position = :position")
    suspend fun deleteButton(profileId: String, position: Int)
}
