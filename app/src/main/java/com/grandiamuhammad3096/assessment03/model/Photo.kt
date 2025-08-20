package com.grandiamuhammad3096.assessment03.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Photo(
    val id: String,         // ID hewan sesuai dokumentasi server galery hewan api
    val title: String,
    val description: String?,
    val imageUrl: String
)
