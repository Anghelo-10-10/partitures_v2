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
            else -> {
                // Si no se puede determinar por extensión, verificar por ruta
                when {
                    fileName.contains("pdfs/") -> MediaType.APPLICATION_PDF
                    else -> MediaType.APPLICATION_OCTET_STREAM
                }
            }
        }
    }
}