package com.grandiamuhammad3096.assessment03.ui.screen

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grandiamuhammad3096.assessment03.database.local.PhotoDb
import com.grandiamuhammad3096.assessment03.database.repo.PhotoRepository
import com.grandiamuhammad3096.assessment03.model.Photo
import com.grandiamuhammad3096.assessment03.network.ApiStatus
import com.grandiamuhammad3096.assessment03.network.PhotoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PhotoRepository(
        dao = PhotoDb.get(app).photoDao(),
        api = PhotoApi.service,
        app = app
    )

    /** UI observe data dari Room (offline‑first) */
    val photos: StateFlow<List<Photo>> = repo.streamPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Status & error sederhana */
    private val _status = MutableStateFlow(ApiStatus.SUCCESS)
    val status: StateFlow<ApiStatus> = _status
    private val _error = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _error

    fun useCacheOnly() {
        // UI tetap bisa menampilkan data dari Flow Room
        _status.value = ApiStatus.SUCCESS
    }

    fun retrieveData(userEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (userEmail.isBlank()) {
                _status.value = ApiStatus.SUCCESS
                return@launch
            }
            _status.value = ApiStatus.LOADING
            try {
                repo.syncFromServer(userEmail)
                _status.value = ApiStatus.SUCCESS
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) {
                    _status.value = ApiStatus.SUCCESS // tetap tampilkan cache
                } else {
                    _status.value = ApiStatus.FAILED
                    _error.value = "Gagal sync: ${e.code()}"
                }
            } catch (e: Exception) {
                Log.d("MainViewModel", "Sync Failure: ${e.message}")
                _status.value = ApiStatus.FAILED
                _error.value = e.message
            }
        }
    }

    /** Unggah antrean lokal kalau user sudah login. */
    fun syncPending(userEmail: String) {
        if (userEmail.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.syncPending(userEmail)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /** CREATE offline (selalu bisa walau belum login). */
    fun createLocalPhoto(title: String, description: String, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.createLocal(title, description, bitmap)
            } catch (e: Exception) {
                _error.value = "Gagal menyimpan lokal: ${e.message}"
            }
        }
    }

    /** EDIT offline (bisa sebelum login). */
    fun editLocalPhoto(localId: String, title: String, description: String, bitmap: Bitmap?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.editLocal(localId, title, description, bitmap)
            } catch (e: Exception) {
                _error.value = "Gagal mengubah: ${e.message}"
            }
        }
    }

    /** DELETE offline. */
    fun deleteLocal(localId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.deleteLocal(localId)
            } catch (e: Exception) {
                _error.value = "Gagal menghapus: ${e.message}"
            }
        }
    }

    fun clearMessage() { _error.value = null }
}


//
//    fun retrieveData(userEmail: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            if (userEmail.isBlank()) {
//                _status.value = ApiStatus.SUCCESS
//                return@launch
//            }
//            _status.value = ApiStatus.LOADING
//            try {
//                repo.syncFromServer(userEmail)
//                _status.value = ApiStatus.SUCCESS
//            } catch (e: retrofit2.HttpException) {
//                if (e.code() == 401) {
//                    // belum login / tidak authorized → jangan ganggu UI
//                    // biarkan data Room yang sudah ada tetap tampil
//                    _status.value = ApiStatus.SUCCESS
//                    // opsional: tandai agar UI bisa menampilkan banner “Silakan login”
//                    // _needsLogin.value = true
//                } else {
//                    _status.value = ApiStatus.FAILED
//                    _error.value = "Gagal sync: ${e.code()}"
//                }
//            } catch (e: java.io.IOException) {
//                // offline → gunakan cache
//                _status.value = ApiStatus.SUCCESS
//            } catch (e: Exception) {
//                Log.d("MainViewModel", "Sync Failure: ${e.message}")
//                // Tetap SUCCESS agar UI menampilkan cache Room jika ada
//                _status.value = ApiStatus.FAILED
//                _error.value = e.message
//            }
//        }
//    }
//
//    fun createPhoto(userEmail: String, title: String, description: String, bitmap: Bitmap) {
//        if (userEmail.isBlank()) return
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                val res = PhotoApi.service.createPhoto(
//                    userEmail.takeIf { it.isNotBlank() },
//                    title.toTextBody(),
//                    description.toTextBody(),
//                    bitmap.toMultipartBody("photo")
//                )
//                if (res.status == "success") repo.syncFromServer(userEmail)
//                else throw Exception(res.message)
//            } catch (e: retrofit2.HttpException) {
//                if (e.code() != 401) _error.value = "Error: ${e.message()}"
//            } catch (e: Exception) {
//                Log.d("MainViewModel", "Create Failure: ${e.message}")
//                _error.value = "Error: ${e.message}"
//            }
//        }
//    }
//
//    fun updatePhotoAll(
//        userEmail: String,
//        id: String,
//        newTitle: String?,
//        newDesc: String?,
//        newBitmap: Bitmap?
//    ) {
//        if (userEmail.isBlank()) return
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                val idBody = id.toTextBody()
//                val titleBody: RequestBody? = newTitle?.takeIf { it.isNotBlank() }?.toTextBody()
//                val descBody: RequestBody? = newDesc?.takeIf { it.isNotBlank() }?.toTextBody()
//                val photoPart: MultipartBody.Part? = newBitmap?.toMultipartBody("photo")
//
//                val res = PhotoApi.service.updatePhotoAll(
//                    userEmail.takeIf { it.isNotBlank() },
//                    idBody,
//                    titleBody,
//                    descBody,
//                    photoPart
//                )
//                if (res.status == "success") {
//                    repo.syncFromServer(userEmail)
//                } else throw Exception(res.message)
//            } catch (e: retrofit2.HttpException) {
//                if (e.code() != 401) _error.value = "Error: ${e.message()}"
//            } catch (e: Exception) {
//                Log.d("MainViewModel", "Update Failure: ${e.message}")
//                _error.value = "Error: ${e.message}"
//            }
//        }
//    }
//
//    fun deleteData(userEmail: String, id: String) {
//        if (userEmail.isBlank()) return
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                val res = PhotoApi.service.deletePhoto(userEmail.takeIf { it.isNotBlank() }, id)
//                if (res.status == "success") {
//                    repo.syncFromServer(userEmail)
//                } else throw Exception(res.message)
//            } catch (e: retrofit2.HttpException) {
//                if (e.code() != 401) _error.value = "Error: ${e.message()}"
//            } catch (e: Exception) {
//                Log.d("MainViewModel", "Delete Failure: ${e.message}")
//                _error.value = "Gagal menghapus: ${e.message}"
//            }
//        }
//    }
//
//    private fun String.toTextBody(): RequestBody =
//        this.toRequestBody("text/plain".toMediaTypeOrNull())
//
//    private fun Bitmap.toMultipartBody(fieldName: String): MultipartBody.Part {
//        val stream = ByteArrayOutputStream()
//        compress(Bitmap.CompressFormat.JPEG, 85, stream)
//        val bytes = stream.toByteArray()
//        val req = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, bytes.size)
//        return MultipartBody.Part.createFormData(fieldName, "image.jpg", req)
//    }