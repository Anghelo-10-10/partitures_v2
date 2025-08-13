package com.partituresforall.partitures.models.responses

import java.time.LocalDateTime

data class SheetResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val artist: String,
    val genre: String,
    val instrument: String,
    val pdfFilename: String,
    val pdfSize: Long,
    val pdfSizeMB: String,
    val pdfContentType: String,
    val pdfDownloadUrl: String,
    val isPublic: Boolean,
    val ownerId: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)