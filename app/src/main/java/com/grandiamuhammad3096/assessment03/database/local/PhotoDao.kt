package com.grandiamuhammad3096.assessment03.database.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE pendingDelete = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<PhotoEntity>)

    @Query("SELECT * FROM photos WHERE localId = :id LIMIT 1")
    suspend fun findById(id: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE syncStatus != :ok OR pendingDelete = 1")
    suspend fun getPending(ok: SyncStatus = SyncStatus.SYNCED): List<PhotoEntity>

    @Query("UPDATE photos SET syncStatus=:status, lastError=:err WHERE localId=:id")
    suspend fun markStatus(id: String, status: SyncStatus, err: String? = null)

    @Query("DELETE FROM photos WHERE localId=:id")
    suspend fun deleteLocal(id: String)
}
