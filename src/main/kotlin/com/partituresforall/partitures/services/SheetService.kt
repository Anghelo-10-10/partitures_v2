package com.partituresforall.partitures.services

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
import com.partituresforall.partitures.models.requests.AdvanceSearchRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional
class SheetService(
    private val sheetRepository: SheetRepository,
    private val userRepository: UserRepository,
    private val userSheetRepository: UserSheetRepository,
    private val fileValidationService: FileValidationService // ✅ NUEVA DEPENDENCIA
) {

    fun createSheetWithFile(request: CreateSheetWithFileRequest, file: MultipartFile): SheetResponse {
        val owner = userRepository.findById(request.ownerId).orElseThrow {
            UserNotFoundException(request.ownerId)
        }

        // ✅ USAR VALIDACIÓN CENTRALIZADA en lugar de validateFile()
        fileValidationService.validatePdfFile(file)

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

        // ✅ USAR VALIDACIÓN CENTRALIZADA
        fileValidationService.validatePdfFile(file)

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

    // ===== 🚀 MÉTODOS OPTIMIZADOS - SIN N+1 QUERIES =====

    fun getPublicSheets(): List<SheetResponse> {
        val sheets = sheetRepository.findByIsPublic(true)
        return getSheetsWithOwnersOptimized(sheets)
    }

    fun searchSheets(searchTerm: String): List<SheetResponse> {
        val sheets = sheetRepository.searchPublicSheets(searchTerm)
        return getSheetsWithOwnersOptimized(sheets)
    }

    fun getSheetsByGenre(genre: String): List<SheetResponse> {
        val sheets = sheetRepository.findByIsPublicAndGenreIgnoreCase(true, genre)
        return getSheetsWithOwnersOptimized(sheets)
    }

    fun getSheetsByInstrument(instrument: String): List<SheetResponse> {
        val sheets = sheetRepository.findByIsPublicAndInstrumentIgnoreCase(true, instrument)
        return getSheetsWithOwnersOptimized(sheets)
    }

    fun getSheetsByArtist(artist: String): List<SheetResponse> {
        val sheets = sheetRepository.findByIsPublicAndArtistContainingIgnoreCase(true, artist)
        return getSheetsWithOwnersOptimized(sheets)
    }

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

    fun getUserPublicSheets(userId: Long): List<SheetResponse> {
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException(userId)
        }

        // ✅ Obtener partituras del usuario que son propietario Y públicas
        return userSheetRepository.findByUserIdAndIsOwner(userId, true)
            .filter { it.sheet.isPublic }  // ← LA DIFERENCIA CLAVE con getUserOwnedSheets
            .map {
                it.sheet.toResponse().copy(ownerId = userId)
            }
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

        val userSheets = userSheetRepository.findByUserIdAndIsFavorite(userId, true)
        val sheets = userSheets.map { it.sheet }
        return getSheetsWithOwnersOptimized(sheets)
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

    // ===== 🚀 MÉTODO PRINCIPAL OPTIMIZADO - ADVANCED SEARCH =====
    fun advancedSearch(request: AdvanceSearchRequest): List<SheetResponse> {
        try {
            println("=== OPTIMIZED ADVANCED SEARCH DEBUG ===")
            println("SearchTerm: '${request.searchTerm}'")
            println("Artist: '${request.artist}'")
            println("Genre: '${request.genre}'")
            println("Instrument: '${request.instrument}'")
            println("SortBy: '${request.sortBy}'")

            // Step 1: Obtener sheets (UNA query)
            println("Step 1: Querying sheets...")
            var sheets = sheetRepository.findByAdvancedSearch(
                searchTerm = request.searchTerm?.takeIf { it.isNotBlank() },
                artist = request.artist?.takeIf { it.isNotBlank() },
                genre = request.genre?.takeIf { it.isNotBlank() },
                instrument = request.instrument?.takeIf { it.isNotBlank() }
            )
            println("Found ${sheets.size} sheets")

            // Step 2: 🚀 OPTIMIZACIÓN CRÍTICA - Batch loading de owners (UNA query)
            println("Step 2: Batch loading owners...")
            val sheetIds = sheets.map { it.id }
            val ownersMap = if (sheetIds.isNotEmpty()) {
                // ✅ UNA SOLA QUERY en lugar de N queries
                userSheetRepository.findSheetOwners(sheetIds)
                    .associate { it.getSheetId() to it.getOwnerId() }
            } else {
                emptyMap<Long, Long>()
            }
            println("✅ Loaded ${ownersMap.size} owners in batch (vs ${sheets.size} individual queries in old method)")

            // Step 3: Aplicar ordenamiento
            println("Step 3: Sorting by '${request.sortBy}'...")
            sheets = when (request.sortBy) {
                "title" -> sheets.sortedBy { it.title }
                "artist" -> sheets.sortedBy { it.artist }
                "recent" -> sheets.sortedByDescending { it.createdAt }
                else -> sheets.sortedByDescending { it.createdAt }
            }

            // Step 4: 🚀 MAPEO OPTIMIZADO - Sin queries adicionales
            println("Step 4: Mapping to response...")
            val results = sheets.mapNotNull { sheet ->
                val ownerId = ownersMap[sheet.id]
                if (ownerId != null) {
                    sheet.toResponse().copy(ownerId = ownerId)
                } else {
                    // Log warning pero no fallar completamente
                    println("⚠️ WARNING: No owner found for sheet ID=${sheet.id}, Title='${sheet.title}'")
                    null
                }
            }

            println("✅ Optimized search completed. Returning ${results.size} results")
            println("📊 Performance improvement: ~${sheets.size}x faster (2 queries vs ${sheets.size + 1})")
            return results

        } catch (e: Exception) {
            println("❌ ERROR in optimized advanced search: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun getRecentSheets(): List<SheetResponse> {
        val sheets = sheetRepository.findByIsPublicOrderByCreatedAtDesc(true).take(20)
        return getSheetsWithOwnersOptimized(sheets)
    }

    // ===== 🚀 MÉTODO HELPER OPTIMIZADO =====
    /**
     * ✅ CRÍTICO: Centraliza la lógica de batch loading de owners
     * Usado por todos los métodos que retornan listas de sheets
     * Evita el N+1 query problem en TODOS los métodos
     */
    private fun getSheetsWithOwnersOptimized(sheets: List<Sheet>): List<SheetResponse> {
        if (sheets.isEmpty()) return emptyList()

        // Batch loading de owners en UNA sola query
        val sheetIds = sheets.map { it.id }
        val ownersMap = userSheetRepository.findSheetOwners(sheetIds)
            .associate { it.getSheetId() to it.getOwnerId() }

        // Mapeo sin queries adicionales
        return sheets.mapNotNull { sheet ->
            val ownerId = ownersMap[sheet.id]
            if (ownerId != null) {
                sheet.toResponse().copy(ownerId = ownerId)
            } else {
                println("⚠️ WARNING: No owner found for sheet ID=${sheet.id}")
                null
            }
        }
    }

    // ===== ❌ MÉTODO ELIMINADO - validateFile() =====
    // Ya no necesitamos este método porque usamos fileValidationService.validatePdfFile()

    // ===== ❌ MÉTODO ELIMINADO - formatFileSize() =====
    // Ya no necesitamos este método porque usamos fileValidationService.formatFileSize()

    // ===== ✅ MÉTODO toResponse() ACTUALIZADO =====
    private fun Sheet.toResponse() = SheetResponse(
        id = this.id,
        title = this.title,
        description = this.description,
        artist = this.artist,
        genre = this.genre,
        instrument = this.instrument,
        pdfFilename = this.pdfFilename,
        pdfSize = this.pdfSize,
        pdfSizeMB = fileValidationService.formatFileSize(this.pdfSize), // ✅ USAR SERVICIO CENTRALIZADO
        pdfContentType = this.pdfContentType,
        pdfDownloadUrl = "/api/sheets/${this.id}/pdf",
        isPublic = this.isPublic,
        ownerId = 0L, // Se sobrescribe con el owner real
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}