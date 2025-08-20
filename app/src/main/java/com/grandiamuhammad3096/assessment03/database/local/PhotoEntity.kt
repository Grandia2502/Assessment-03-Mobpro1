package com.grandiamuhammad3096.assessment03.database.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncStatus { SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE, FAILED }

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val localId: String,         // UUID lokal
    val remoteId: Long? = null,              // id dari server (jika sudah sinkron)
    val ownerEmail: String? = null,          // bisa null saat offline (belum login)
    val title: String,
    val description: String,
    val localUri: String,                    // file://… atau content://… disimpan lokal
    val remoteUrl: String? = null,           // URL http(s) dari server (jika sudah sinkron)
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val pendingDelete: Boolean = false,
    val lastError: String? = null
)

