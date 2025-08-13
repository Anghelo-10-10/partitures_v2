package com.partituresforall.partitures.models.requests

data class CreateSheetRequest(
    val title: String,
    val description: String?,
    val pdfReference: String,
    val isPublic: Boolean = false,
    val ownerId: Long
)