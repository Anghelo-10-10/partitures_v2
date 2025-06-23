package com.partituresforall.partitures.models.requests

data class UpdateSheetRequest(
    val title: String?,
    val description: String?,
    val isPublic: Boolean?
)