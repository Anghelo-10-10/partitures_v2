package com.partituresforall.partitures.models.requests
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AdvanceSearchRequest(
    val searchTerm: String? = null,
    val artist: String? = null,
    val genre: String? = null,
    val instrument: String? = null,
    val sortBy: String? = "recent"
)