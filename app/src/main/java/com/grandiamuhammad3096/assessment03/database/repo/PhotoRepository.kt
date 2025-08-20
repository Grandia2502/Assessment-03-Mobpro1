package com.grandiamuhammad3096.assessment03.database.repo

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore.Images.Media
import com.grandiamuhammad3096.assessment03.database.local.PhotoDao
import com.grandiamuhammad3096.assessment03.database.local.PhotoEntity
import com.grandiamuhammad3096.assessment03.database.local.SyncStatus
import com.grandiamuhammad3096.assessment03.model.Photo
import com.grandiamuhammad3096.assessment03.network.PhotoApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PhotoRepository(
    private val dao: PhotoDao,
    private val api: PhotoApiService,
    private val app: Application
) {
    /** Flow untuk UI (local-first). */
    fun streamPhotos(): Flow<List<Photo>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    /** CREATE offline — simpan file ke folder app + tulis row PENDING_CREATE. */
    suspend fun createLocal(title: String, desc: String, bitmap: Bitmap) {
        val localUri = saveBitmapToAppFiles(app, bitmap).toString()
        val entity = PhotoEntity(
            localId = UUID.randomUUID().toString(),
            title = title,
            description = desc,
            localUri = localUri,
            syncStatus = SyncStatus.PENDING_CREATE
        )
        dao.upsert(entity)
    }

    /** EDIT offline — perbarui judul/desc, opsional ganti file. */
    suspend fun editLocal(localId: String, title: String, desc: String, bitmap: Bitmap?) {
        val cur = dao.findById(localId) ?: return
        val newUri = bitmap?.let { saveBitmapToAppFiles(app, it).toString() } ?: cur.localUri
        val newStatus = if (cur.remoteId == null) SyncStatus.PENDING_CREATE else SyncStatus.PENDING_UPDATE
        dao.upsert(
            cur.copy(
                title = title,
                description = desc,
                localUri = newUri,
                syncStatus = newStatus,
                modifiedAt = System.currentTimeMillis(),
                lastError = null
            )
        )
    }

    /** DELETE offline — jika belum di-server, hapus lokal; jika sudah, tandai PENDING_DELETE. */
    suspend fun deleteLocal(localId: String) {
        val cur = dao.findById(localId) ?: return
        if (cur.remoteId == null) {
            dao.deleteLocal(localId)
        } else {
            dao.upsert(cur.copy(pendingDelete = true, syncStatus = SyncStatus.PENDING_DELETE))
        }
    }

    /** SYNC dari server → gabungkan ke Room (untuk user yang login). */
    suspend fun syncFromServer(userEmail: String) {
        if (userEmail.isBlank()) return
        val remotes = api.getPhotos(userEmail)
        val list = remotes.map { r ->
            // Remote → Entity (tidak menyentuh row PENDING_x)
            PhotoEntity(
                localId = "remote-${r.id}",       // key stabil untuk remote
                remoteId = r.id.toLongOrNull(),
                ownerEmail = userEmail,
                title = r.title,
                description = r.description ?: "",
                localUri = r.imageUrl,            // biar AsyncImage bisa load
                remoteUrl = r.imageUrl,
                syncStatus = SyncStatus.SYNCED,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                pendingDelete = false,
                lastError = null
            )
        }
        dao.upsertAll(list)
    }

    /** SYNC antrean lokal → server (panggil setelah login, atau manual). */
    suspend fun syncPending(userEmail: String) {
        if (userEmail.isBlank()) return
        val pending = dao.getPending()
        for (e in pending) {
            try {
                when {
                    e.pendingDelete -> {
                        e.remoteId?.let { api.deletePhoto(userEmail, it.toString()) }
                        dao.deleteLocal(e.localId)
                    }
                    e.remoteId == null -> {
                        // CREATE → upload file dari localUri
                        val bmp = loadBitmap(app.contentResolver, Uri.parse(e.localUri))
                        val res = api.createPhoto(
                            userEmail,
                            e.title.toTextBody(),
                            e.description.toTextBody(),
                            bmp.toMultipartBody("photo")
                        )
                        if (res.status == "success") {
                            // Ambil ulang list server → simpan sebagai SYNCED
                            syncFromServer(userEmail)
                            dao.markStatus(e.localId, SyncStatus.SYNCED, null)
                        } else {
                            dao.markStatus(e.localId, SyncStatus.FAILED, res.message)
                        }
                    }
                    else -> {
                        // UPDATE
                        val maybeBmp = runCatching {
                            loadBitmap(app.contentResolver, Uri.parse(e.localUri))
                        }.getOrNull()
                        val photoPart = maybeBmp?.toMultipartBody("photo")
                        val res = api.updatePhotoAll(
                            userEmail,
                            id = (e.remoteId).toString().toTextBody(),
                            title = e.title.toTextBody(),
                            description = e.description.toTextBody(),
                            photo = photoPart
                        )
                        if (res.status == "success") {
                            syncFromServer(userEmail)
                            dao.markStatus(e.localId, SyncStatus.SYNCED, null)
                        } else {
                            dao.markStatus(e.localId, SyncStatus.FAILED, res.message)
                        }
                    }
                }
            } catch (t: Throwable) {
                dao.markStatus(e.localId, SyncStatus.FAILED, t.message)
            }
        }
    }

    // ==== Helpers ====

    private fun saveBitmapToAppFiles(ctx: Application, bmp: Bitmap): Uri {
        val dir = File(ctx.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "local_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(file)
    }

    private fun loadBitmap(resolver: ContentResolver, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(src)
        } else {
            @Suppress("DEPRECATION")
            Media.getBitmap(resolver, uri)
        }
    }

    private fun String.toTextBody(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())

    private fun Bitmap.toMultipartBody(fieldName: String): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val bytes = stream.toByteArray()
        val req = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, bytes.size)
        return MultipartBody.Part.createFormData(fieldName, "image.jpg", req)
    }

    private fun PhotoEntity.toDomain(): Photo =
        Photo(
            id = localId, // pakai localId supaya offline item bisa di-edit/hapus juga
            title = title,
            description = description,
            imageUrl = remoteUrl ?: localUri
        )
}