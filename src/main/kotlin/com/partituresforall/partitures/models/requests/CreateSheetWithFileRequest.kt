package com.partituresforall.partitures.models.requests

data class CreateSheetWithFileRequest(
    val title: String,
    val description: String?,
    val artist: String,
    val genre: String,
    val instrument: String,
    val isPublic: Boolean = false,
    val ownerId: Long
)
