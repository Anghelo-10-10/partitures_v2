package com.partituresforall.partitures.repositories

import com.partituresforall.partitures.models.entities.UserSheet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

// UserSheetRepository.kt
@Repository
interface UserSheetRepository : JpaRepository<UserSheet, Long> {
    fun findByUserIdAndSheetId(userId: Long, sheetId: Long): UserSheet?
    fun findByUserIdAndIsFavorite(userId: Long, isFavorite: Boolean): List<UserSheet>
    fun existsByUserIdAndSheetIdAndIsFavorite(userId: Long, sheetId: Long, isFavorite: Boolean): Boolean
    fun findBySheetIdAndIsOwner(sheetId: Long, isOwner: Boolean): UserSheet? // Para obtener el dueño
    fun deleteBySheetId(sheetId: Long)
}