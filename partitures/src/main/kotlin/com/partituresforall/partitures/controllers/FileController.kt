package com.partituresforall.partitures.controllers

import com.partituresforall.partitures.services.FileService
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/files")
class FileController(
    private val fileService: FileService
) {

    @GetMapping("/{fileName:.+}")
    fun downloadFile(@PathVariable fileName: String): ResponseEntity<Resource> {
        val resource = fileService.loadFileAsResource(fileName)
        val contentType = determineContentType(fileName)

        return ResponseEntity.ok()
            .contentType(contentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${resource.filename}\"")
            .body(resource)
    }

    @GetMapping("/{fileName:.+}/download")
    fun downloadFileAsAttachment(@PathVariable fileName: String): ResponseEntity<Resource> {
        val resource = fileService.loadFileAsResource(fileName)
        val contentType = determineContentType(fileName)

        return ResponseEntity.ok()
            .contentType(contentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${resource.filename}\"")
            .body(resource)
    }

    // ===== ENDPOINT ESPECÍFICO PARA IMÁGENES =====
    @GetMapping("/images/{fileName:.+}")
    fun getImage(@PathVariable fileName: String): ResponseEntity<Resource> {
        val resource = fileService.loadFileAsResource("images/$fileName")
        val contentType = determineImageContentType(fileName)

        return ResponseEntity.ok()
            .contentType(contentType)
            .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache por 1 hora
            .body(resource)
    }

    // ===== ENDPOINT ESPECÍFICO PARA PDFs =====
    @GetMapping("/pdfs/{fileName:.+}")
    fun getPdf(@PathVariable fileName: String): ResponseEntity<Resource> {
        val resource = fileService.loadFileAsResource("pdfs/$fileName")

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${resource.filename}\"")
            .body(resource)
    }

    @GetMapping("/pdfs/{fileName:.+}/download")
    fun downloadPdf(@PathVariable fileName: String): ResponseEntity<Resource> {
        val resource = fileService.loadFileAsResource("pdfs/$fileName")

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${resource.filename}\"")
            .body(resource)
    }

    // ===== MÉTODOS HELPER PARA CONTENT-TYPE =====

    private fun determineContentType(fileName: String): MediaType {
        val extension = fileName.substringAfterLast(".", "").lowercase()

        return when (extension) {
            "pdf" -> MediaType.APPLICATION_PDF
            "jpg", "jpeg" -> MediaType.IMAGE_JPEG
            "png" -> MediaType.IMAGE_PNG
            "webp" -> MediaType.parseMediaType("image/webp")
            else -> {
                // Si no se puede determinar por extensión, verificar por ruta
                when {
                    fileName.contains("images/") -> MediaType.IMAGE_JPEG // Default para imágenes
                    fileName.contains("pdfs/") -> MediaType.APPLICATION_PDF
                    else -> MediaType.APPLICATION_OCTET_STREAM
                }
            }
        }
    }

    private fun determineImageContentType(fileName: String): MediaType {
        val extension = fileName.substringAfterLast(".", "").lowercase()

        return when (extension) {
            "jpg", "jpeg" -> MediaType.IMAGE_JPEG
            "png" -> MediaType.IMAGE_PNG
            "webp" -> MediaType.parseMediaType("image/webp")
            else -> MediaType.IMAGE_JPEG // Default
        }
    }
}