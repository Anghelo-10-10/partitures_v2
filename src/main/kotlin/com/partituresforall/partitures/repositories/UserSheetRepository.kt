package com.partituresforall.partitures.repositories

import com.partituresforall.partitures.models.entities.UserSheet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserSheetRepository : JpaRepository<UserSheet, Long> {
    // ===== MÉTODOS EXISTENTES =====
    fun findByUserIdAndSheetId(userId: Long, sheetId: Long): UserSheet?
    fun findByUserIdAndIsFavorite(userId: Long, isFavorite: Boolean): List<UserSheet>
    fun findByUserIdAndIsOwner(userId: Long, isOwner: Boolean): List<UserSheet>
    fun existsByUserIdAndSheetIdAndIsFavorite(userId: Long, sheetId: Long, isFavorite: Boolean): Boolean
    fun findBySheetIdAndIsOwner(sheetId: Long, isOwner: Boolean): UserSheet?
    fun deleteBySheetId(sheetId: Long)

    // ===== 🚀 NUEVOS MÉTODOS OPTIMIZADOS =====

    /**
     * ✅ CRÍTICO: Obtiene todos los owners de múltiples sheets en UNA SOLA QUERY
     * Reemplaza las N queries individuales por una sola query batch
     *
     * @param sheetIds Lista de IDs de sheets
     * @return Map donde key=sheetId, value=ownerId
     */
    @Query("""
        SELECT us.sheet.id, us.user.id 
        FROM UserSheet us 
        WHERE us.sheet.id IN :sheetIds AND us.isOwner = true
    """)
    fun findOwnerIdsBySheetIds(@Param("sheetIds") sheetIds: List<Long>): List<Array<Any>>

    /**
     * ✅ ALTERNATIVA: Usando proyección con interface para mayor claridad de tipos
     */
    interface SheetOwnerProjection {
        fun getSheetId(): Long
        fun getOwnerId(): Long
    }

    @Query("""
        SELECT us.sheet.id as sheetId, us.user.id as ownerId 
        FROM UserSheet us 
        WHERE us.sheet.id IN :sheetIds AND us.isOwner = true
    """)
    fun findSheetOwners(@Param("sheetIds") sheetIds: List<Long>): List<SheetOwnerProjection>

    /**
     * ✅ BONUS: Para métodos que también necesitan optimización
     */
    @Query("""
        SELECT us 
        FROM UserSheet us 
        JOIN FETCH us.user 
        JOIN FETCH us.sheet 
        WHERE us.sheet.id IN :sheetIds AND us.isOwner = true
    """)
    fun findOwnersWithSheetsBySheetIds(@Param("sheetIds") sheetIds: List<Long>): List<UserSheet>
}