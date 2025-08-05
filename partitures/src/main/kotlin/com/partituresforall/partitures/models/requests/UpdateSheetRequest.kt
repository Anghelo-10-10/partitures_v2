package com.partituresforall.partitures.models.requests

data class UpdateSheetRequest(
    val title: String?,
    val description: String?,
    val artist: String?,
    val genre: String?,
    val instrument: String?,
    val isPublic: Boolean?
)