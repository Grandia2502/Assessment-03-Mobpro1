package com.grandiamuhammad3096.assessment03.network

import com.grandiamuhammad3096.assessment03.model.OpStatus
import com.grandiamuhammad3096.assessment03.model.Photo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

private const val BASE_URL = "http://10.84.14.36/gallery-backend/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

interface PhotoApiService {
    @GET("api/photos/list.php")
    suspend fun getPhotos(
        @Header("Authorization") userEmail: String?
    ): List<Photo>

    @Multipart
    @POST("api/photos/create.php")
    suspend fun createPhoto(
        @Header("Authorization") userEmail: String?,
        @Part("title") nama: RequestBody,
        @Part("description") namaLatin: RequestBody,
        @Part photo: MultipartBody.Part
    ): OpStatus

    @Multipart
    @POST("api/photos/update_all.php")
    suspend fun updatePhotoAll(
        @Header("Authorization") userEmail: String?,
        @Part("id") id: RequestBody,
        @Part("title") title: RequestBody?,         // opsional
        @Part("description") description: RequestBody?, // opsional
        @Part photo: MultipartBody.Part?            // opsional
    ): OpStatus

    @DELETE("api/photos/delete.php")
    suspend fun deletePhoto(
        @Header("Authorization") userEmail: String?,
        @Query("id") id: String
    ): OpStatus
}

object  PhotoApi {
    val service: PhotoApiService by lazy {
        retrofit.create(PhotoApiService::class.java)
    }

}

enum class ApiStatus { LOADING, SUCCESS, FAILED }
enum class CropTarget { CREATE, EDIT }