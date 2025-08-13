package com.partituresforall.partitures.models.requests

data class AdvanceSearchRequest(
    val searchTerm: String? = null,
    val artist: String? = null,
    val genre: String? = null,
    val instrument: String? = null,
    val sortBy: String? = "recent"
)