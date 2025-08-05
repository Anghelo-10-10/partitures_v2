package com.partituresforall.partitures.controllers

import com.partituresforall.partitures.models.requests.CreateSheetRequest
import com.partituresforall.partitures.models.requests.CreateSheetWithFileRequest
import com.partituresforall.partitures.models.requests.UpdateSheetRequest
import com.partituresforall.partitures.models.responses.SheetResponse
import com.partituresforall.partitures.services.SheetService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/sheets")
class SheetController(
    private val sheetService: SheetService
) {

    // ===== CRUD BÁSICO =====

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createSheetWithFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("title") title: String,
        @RequestParam("description", required = false) description: String?,
        @RequestParam("artist") artist: String,
        @RequestParam("genre") genre: String,
        @RequestParam("instrument") instrument: String,
        @RequestParam("isPublic", defaultValue = "false") isPublic: Boolean,
        @RequestParam("ownerId") ownerId: Long
    ): SheetResponse {
        val request = CreateSheetWithFileRequest(
            title = title,
            description = description,
            artist = artist,
            genre = genre,
            instrument = instrument,
            isPublic = isPublic,
            ownerId = ownerId
        )
        return sheetService.createSheetWithFile(request, file)
    }

    @GetMapping("/{id}")
    fun getSheet(@PathVariable id: Long): SheetResponse {
        return sheetService.getSheetById(id)
    }

    @PutMapping("/{id}")
    fun updateSheet(
        @PathVariable id: Long,
        @RequestBody request: UpdateSheetRequest
    ): SheetResponse {
        return sheetService.updateSheet(id, request)
    }

    @PutMapping("/{id}/file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateSheetFile(
        @PathVariable id: Long,
        @RequestParam("file") file: MultipartFile
    ): SheetResponse {
        return sheetService.updateSheetFile(id, file)
    }

    @DeleteMapping("/{id}")
    fun deleteSheet(@PathVariable id: Long) {
        sheetService.deleteSheet(id)
    }

    // ===== ARCHIVOS PDF =====

    @GetMapping("/{id}/pdf")
    fun viewSheetPdf(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val sheet = sheetService.getSheetById(id)
        val pdfContent = sheetService.getSheetPdfContent(id)

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${sheet.pdfFilename}\"")
            .contentLength(sheet.pdfSize)
            .body(pdfContent)
    }

    @GetMapping("/{id}/pdf/download")
    fun downloadSheetPdf(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val sheet = sheetService.getSheetById(id)
        val pdfContent = sheetService.getSheetPdfContent(id)

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${sheet.pdfFilename}\"")
            .contentLength(sheet.pdfSize)
            .body(pdfContent)
    }

    // ===== BÚSQUEDAS Y FILTROS =====

    @GetMapping("/public")
    fun getPublicSheets(): List<SheetResponse> {
        return sheetService.getPublicSheets()
    }

    @GetMapping("/search")
    fun searchSheets(@RequestParam("q") searchTerm: String): List<SheetResponse> {
        return sheetService.searchSheets(searchTerm)
    }

    @GetMapping("/genre/{genre}")
    fun getSheetsByGenre(@PathVariable genre: String): List<SheetResponse> {
        return sheetService.getSheetsByGenre(genre)
    }

    @GetMapping("/instrument/{instrument}")
    fun getSheetsByInstrument(@PathVariable instrument: String): List<SheetResponse> {
        return sheetService.getSheetsByInstrument(instrument)
    }

    @GetMapping("/artist/{artist}")
    fun getSheetsByArtist(@PathVariable artist: String): List<SheetResponse> {
        return sheetService.getSheetsByArtist(artist)
    }

    // ===== FILTROS DISPONIBLES =====

    @GetMapping("/filters/genres")
    fun getAvailableGenres(): List<String> {
        return sheetService.getAvailableGenres()
    }

    @GetMapping("/filters/instruments")
    fun getAvailableInstruments(): List<String> {
        return sheetService.getAvailableInstruments()
    }

    @GetMapping("/filters/artists")
    fun getAvailableArtists(): List<String> {
        return sheetService.getAvailableArtists()
    }

    // ===== USUARIO =====

    @GetMapping("/users/{userId}/owned")
    fun getUserOwnedSheets(@PathVariable userId: Long): List<SheetResponse> {
        return sheetService.getUserOwnedSheets(userId)
    }

    // ===== FAVORITOS =====

    @PostMapping("/{sheetId}/favorites")
    fun addToFavorites(
        @PathVariable sheetId: Long,
        @RequestParam userId: Long
    ) {
        sheetService.addToFavorites(userId, sheetId)
    }

    @DeleteMapping("/{sheetId}/favorites")
    fun removeFromFavorites(
        @PathVariable sheetId: Long,
        @RequestParam userId: Long
    ) {
        sheetService.removeFromFavorites(userId, sheetId)
    }

    @GetMapping("/users/{userId}/favorites")
    fun getFavorites(@PathVariable userId: Long): List<SheetResponse> {
        return sheetService.getFavoriteSheets(userId)
    }

    @GetMapping("/{sheetId}/is-favorite")
    fun isSheetFavorite(
        @PathVariable sheetId: Long,
        @RequestParam userId: Long
    ): Boolean {
        return sheetService.isSheetFavorite(userId, sheetId)
    }
}