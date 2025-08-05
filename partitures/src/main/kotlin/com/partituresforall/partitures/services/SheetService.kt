package com.partituresforall.partitures.services

import com.partituresforall.partitures.exceptions.exceptions.files.InvalidFileTypeException
import com.partituresforall.partitures.exceptions.exceptions.sheets.*
import com.partituresforall.partitures.exceptions.exceptions.users.*
import com.partituresforall.partitures.models.entities.Sheet
import com.partituresforall.partitures.models.entities.UserSheet
import com.partituresforall.partitures.models.requests.CreateSheetWithFileRequest
import com.partituresforall.partitures.models.requests.UpdateSheetRequest
import com.partituresforall.partitures.models.responses.SheetResponse
import com.partituresforall.partitures.repositories.SheetRepository
import com.partituresforall.partitures.repositories.UserRepository
import com.partituresforall.partitures.repositories.UserSheetRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import kotlin.math.round

@Service
@Transactional
class SheetService(
    private val sheetRepository: SheetRepository,
    private val userRepository: UserRepository,
    private val userSheetRepository: UserSheetRepository
) {

    fun createSheetWithFile(request: CreateSheetWithFileRequest, file: MultipartFile): SheetResponse {
        // Validar owner
        val owner = userRepository.findById(request.ownerId).orElseThrow {
            UserNotFoundException(request.ownerId)
        }

        // Validar archivo
        validateFile(file)

        val sheet = sheetRepository.save(
            Sheet(
                title = request.title,
                description = request.description,
                artist = request.artist,
                genre = request.genre,
                instrument = request.instrument,
                pdfContent = file.bytes,
                pdfFilename = file.originalFilename ?: "partitura.pdf",
                pdfSize = file.size,
                pdfContentType = file.contentType ?: "application/pdf",
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

        return sheet.toResponse().copy(ownerId = owner.id)
    }

    fun getSheetById(id: Long): SheetResponse {
        val sheet = sheetRepository.findById(id).orElseThrow {
            SheetNotFoundException(id)
        }

        val owner = userSheetRepository.findBySheetIdAndIsOwner(id, true)?.user
            ?: throw SheetNotFoundException(id)

        return sheet.toResponse().copy(ownerId = owner.id)
    }

    fun getSheetPdfContent(id: Long): ByteArray {
        val sheet = sheetRepository.findById(id).orElseThrow {
            SheetNotFoundException(id)
        }
        return sheet.pdfContent
    }

    fun updateSheet(id: Long, request: UpdateSheetRequest): SheetResponse {
        val sheet = sheetRepository.findById(id).orElseThrow {
            SheetNotFoundException(id)
        }

        val owner = userSheetRepository.findBySheetIdAndIsOwner(id, true)?.user
            ?: throw SheetNotFoundException(id)

        // Actualizar campos
        request.title?.let { sheet.title = it }
        request.description?.let { sheet.description = it }
        request.artist?.let { sheet.artist = it }
        request.genre?.let { sheet.genre = it }
        request.instrument?.let { sheet.instrument = it }
        request.isPublic?.let { sheet.isPublic = it }

        val updatedSheet = sheetRepository.save(sheet)
        return updatedSheet.toResponse().copy(ownerId = owner.id)
    }

    fun updateSheetFile(id: Long, file: MultipartFile): SheetResponse {
        val sheet = sheetRepository.findById(id).orElseThrow {
            SheetNotFoundException(id)
        }

        val owner = userSheetRepository.findBySheetIdAndIsOwner(id, true)?.user
            ?: throw SheetNotFoundException(id)

        // Validar nuevo archivo
        validateFile(file)

        // Actualizar contenido del PDF
        sheet.pdfContent = file.bytes
        sheet.pdfFilename = file.originalFilename ?: "partitura.pdf"
        sheet.pdfSize = file.size
        sheet.pdfContentType = file.contentType ?: "application/pdf"

        val updatedSheet = sheetRepository.save(sheet)
        return updatedSheet.toResponse().copy(ownerId = owner.id)
    }

    fun deleteSheet(id: Long) {
        if (!sheetRepository.existsById(id)) {
            throw SheetNotFoundException(id)
        }
        userSheetRepository.deleteBySheetId(id)
        sheetRepository.deleteById(id)
    }

    // ===== MÉTODOS DE BÚSQUEDA =====

    fun getPublicSheets(): List<SheetResponse> {
        return sheetRepository.findByIsPublic(true)
            .map { sheet ->
                val owner = userSheetRepository.findBySheetIdAndIsOwner(sheet.id, true)?.user
                    ?: throw SheetNotFoundException(sheet.id)
                sheet.toResponse().copy(ownerId = owner.id)
            }
    }

    fun searchSheets(searchTerm: String): List<SheetResponse> {
        return sheetRepository.searchPublicSheets(searchTerm)
            .map { sheet ->
                val owner = userSheetRepository.findBySheetIdAndIsOwner(sheet.id, true)?.user
                    ?: throw SheetNotFoundException(sheet.id)
                sheet.toResponse().copy(ownerId = owner.id)
            }
    }

    fun getSheetsByGenre(genre: String): List<SheetResponse> {
        return sheetRepository.findByIsPublicAndGenreIgnoreCase(true, genre)
            .map { sheet ->
                val owner = userSheetRepository.findBySheetIdAndIsOwner(sheet.id, true)?.user
                    ?: throw SheetNotFoundException(sheet.id)
                sheet.toResponse().copy(ownerId = owner.id)
            }
    }

    fun getSheetsByInstrument(instrument: String): List<SheetResponse> {
        return sheetRepository.findByIsPublicAndInstrumentIgnoreCase(true, instrument)
            .map { sheet ->
                val owner = userSheetRepository.findBySheetIdAndIsOwner(sheet.id, true)?.user
                    ?: throw SheetNotFoundException(sheet.id)
                sheet.toResponse().copy(ownerId = owner.id)
            }
    }

    fun getSheetsByArtist(artist: String): List<SheetResponse> {
        return sheetRepository.findByIsPublicAndArtistContainingIgnoreCase(true, artist)
            .map { sheet ->
                val owner = userSheetRepository.findBySheetIdAndIsOwner(sheet.id, true)?.user
                    ?: throw SheetNotFoundException(sheet.id)
                sheet.toResponse().copy(ownerId = owner.id)
            }
    }

    // Obtener filtros disponibles
    fun getAvailableGenres(): List<String> {
        return sheetRepository.findDistinctGenres()
    }

    fun getAvailableInstruments(): List<String> {
        return sheetRepository.findDistinctInstruments()
    }

    fun getAvailableArtists(): List<String> {
        return sheetRepository.findDistinctArtists()
    }

    fun getUserOwnedSheets(userId: Long): List<SheetResponse> {
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException(userId)
        }

        return userSheetRepository.findByUserIdAndIsOwner(userId, true)
            .map {
                it.sheet.toResponse().copy(ownerId = userId)
            }
    }

    // ===== MÉTODOS DE FAVORITOS =====

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
                val owner = userSheetRepository.findBySheetIdAndIsOwner(it.sheet.id, true)?.user
                    ?: throw SheetNotFoundException(it.sheet.id)

                it.sheet.toResponse().copy(ownerId = owner.id)
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

    // ===== VALIDACIONES Y UTILIDADES =====

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw InvalidFileTypeException("El archivo está vacío") as Throwable
        }

        val maxSize = 5 * 1024 * 1024L // 5MB
        if (file.size > maxSize) {
            throw InvalidFileTypeException("El archivo es demasiado grande. Máximo permitido: 5MB")
        }

        val contentType = file.contentType
        if (contentType != "application/pdf") {
            throw InvalidFileTypeException("Tipo de archivo no permitido. Solo se aceptan archivos PDF")
        }

        val originalFilename = file.originalFilename ?: ""
        if (!originalFilename.lowercase().endsWith(".pdf")) {
            throw InvalidFileTypeException("El archivo debe tener extensión .pdf")
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val mb = bytes.toDouble() / (1024 * 1024)
        return when {
            mb >= 1.0 -> "${round(mb * 100) / 100} MB"
            else -> "${round(bytes.toDouble() / 1024 * 100) / 100} KB"
        }
    }

    private fun Sheet.toResponse() = SheetResponse(
        id = this.id,
        title = this.title,
        description = this.description,
        artist = this.artist,
        genre = this.genre,
        instrument = this.instrument,
        pdfFilename = this.pdfFilename,
        pdfSize = this.pdfSize,
        pdfSizeMB = formatFileSize(this.pdfSize),
        pdfContentType = this.pdfContentType,
        pdfDownloadUrl = "/api/sheets/${this.id}/pdf",
        isPublic = this.isPublic,
        ownerId = 0L, // Se sobrescribe en los métodos
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}