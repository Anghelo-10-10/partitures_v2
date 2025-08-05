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

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${resource.filename}\"")
            .body(resource)
    }

    @GetMapping("/{fileName:.+}/download")
    fun downloadFileAsAttachment(@PathVariable fileName: String): ResponseEntity<Resource> {
        val resource = fileService.loadFileAsResource(fileName)

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${resource.filename}\"")
            .body(resource)
    }
}