package com.partituresforall.partitures.models.responses

import java.time.LocalDateTime

data class SheetResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val pdfReference: String,
    val isPublic: Boolean,
    val ownerId: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)