package com.partituresforall.partitures.services

import com.partituresforall.partitures.exceptions.exceptions.sheets.*
import com.partituresforall.partitures.exceptions.exceptions.users.*
import com.partituresforall.partitures.models.entities.Sheet
import com.partituresforall.partitures.models.entities.UserSheet
import com.partituresforall.partitures.models.requests.CreateSheetRequest
import com.partituresforall.partitures.models.requests.UpdateSheetRequest
import com.partituresforall.partitures.models.responses.SheetResponse
import com.partituresforall.partitures.repositories.SheetRepository
import com.partituresforall.partitures.repositories.UserRepository
import com.partituresforall.partitures.repositories.UserSheetRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SheetService(
    private val sheetRepository: SheetRepository,
    private val userRepository: UserRepository,
    private val userSheetRepository: UserSheetRepository
) {
    fun createSheet(request: CreateSheetRequest): SheetResponse {
        // Validar owner
        val owner = userRepository.findById(request.ownerId).orElseThrow {
            UserNotFoundException(request.ownerId)
        }

        // Validar PDF reference
        if (!request.pdfReference.endsWith(".pdf")) {
            throw InvalidPdfReferenceException(request.pdfReference)
        }

        val sheet = sheetRepository.save(
            Sheet(
                title = request.title,
                description = request.description,
                pdfReference = request.pdfReference,
                isPublic = request.isPublic
            )
        )

        userSheetRepository.save(
            UserSheet(
                user = owner,
                sheet = sheet,
                isOwner = true,
                isFavorite = false
            )
        )

        return SheetResponse(
            id = sheet.id,
            title = sheet.title,
            description = sheet.description,
            pdfReference = sheet.pdfReference,
            isPublic = sheet.isPublic,
            ownerId = owner.id,
            createdAt = sheet.createdAt,
            updatedAt = sheet.updatedAt
        )
    }

    fun getSheetById(id: Long): SheetResponse {
        val sheet = sheetRepository.findById(id).orElseThrow {
            SheetNotFoundException(id)
        }

        val owner = userSheetRepository.findBySheetIdAndIsOwner(id, true)?.user
            ?: throw SheetNotFoundException(id)

        return sheet.toResponse().copy(ownerId = owner.id)
    }

    fun updateSheet(id: Long, request: UpdateSheetRequest): SheetResponse {
        val sheet = sheetRepository.findById(id).orElseThrow {
            SheetNotFoundException(id)
        }

        // Actualizar campos sin verificación de owner
        request.title?.let { sheet.title = it }
        request.description?.let { sheet.description = it }
        request.isPublic?.let { sheet.isPublic = it }

        val owner = userSheetRepository.findBySheetIdAndIsOwner(id, true)?.user
            ?: throw SheetNotFoundException(id)

        return sheetRepository.save(sheet).toResponse().copy(ownerId = owner.id)
    }

    fun deleteSheet(id: Long) {
        if (!sheetRepository.existsById(id)) {
            throw SheetNotFoundException(id)
        }
        userSheetRepository.deleteBySheetId(id)
        sheetRepository.deleteById(id)
    }

    fun addToFavorites(userId: Long, sheetId: Long) {
        val user = userRepository.findById(userId).orElseThrow {
            UserNotFoundException(userId)
        }

        val sheet = sheetRepository.findById(sheetId).orElseThrow {
            SheetNotFoundException(sheetId)
        }

        val existingRelation = userSheetRepository.findByUserIdAndSheetId(userId, sheetId)

        if (existingRelation == null) {
            userSheetRepository.save(
                UserSheet(
                    user = user,
                    sheet = sheet,
                    isOwner = false,
                    isFavorite = true
                )
            )
        } else {
            existingRelation.isFavorite = true
            userSheetRepository.save(existingRelation)
        }
    }

    fun removeFromFavorites(userId: Long, sheetId: Long) {
        val relation = userSheetRepository.findByUserIdAndSheetId(userId, sheetId)
            ?: throw SheetNotFoundException(sheetId)

        if (relation.isOwner) {
            throw IllegalStateException("No se puede quitar de favoritos una partitura propia")
        }

        userSheetRepository.delete(relation)
    }

    fun getFavoriteSheets(userId: Long): List<SheetResponse> {
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException(userId)
        }

        return userSheetRepository.findByUserIdAndIsFavorite(userId, true)
            .map {
                it.sheet.toResponse().copy(
                    ownerId = userSheetRepository.findBySheetIdAndIsOwner(it.sheet.id, true)?.user?.id
                        ?: throw SheetNotFoundException(it.sheet.id)
                )
            }
    }

    fun isSheetFavorite(userId: Long, sheetId: Long): Boolean {
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException(userId)
        }
        if (!sheetRepository.existsById(sheetId)) {
            throw SheetNotFoundException(sheetId)
        }
        return userSheetRepository.existsByUserIdAndSheetIdAndIsFavorite(userId, sheetId, true)
    }

    private fun Sheet.toResponse() = SheetResponse(
        id = this.id,
        title = this.title,
        description = this.description,
        pdfReference = this.pdfReference,
        isPublic = this.isPublic,
        ownerId = 0L, // Se sobrescribe en los métodos que usan esta función
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}