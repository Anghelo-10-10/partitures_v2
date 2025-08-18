package com.partituresforall.partitures.models.requests
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class UpdateUserRequest(
    val name: String?,
    val email: String?,
    val password: String?
)