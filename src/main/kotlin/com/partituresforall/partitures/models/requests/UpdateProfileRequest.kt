package com.partituresforall.partitures.models.requests
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class UpdateProfileRequest(
    val name: String?,
    val bio: String?,
)