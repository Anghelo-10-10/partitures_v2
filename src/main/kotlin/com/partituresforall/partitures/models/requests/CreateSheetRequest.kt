package com.partituresforall.partitures.models.requests
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CreateSheetRequest(
    val title: String,
    val description: String?,
    val pdfReference: String,
    val isPublic: Boolean = false,
    val ownerId: Long
)