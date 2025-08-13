package com.partituresforall.partitures.services

import com.partituresforall.partitures.exceptions.exceptions.files.InvalidFileTypeException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FileValidationServiceTest {

    private lateinit var fileValidationService: FileValidationService

    @BeforeEach
    fun carga() {
        fileValidationService = FileValidationService()
    }

    // ===== HELPER: MultipartFile Mock Implementation =====
    private class TestMultipartFile(
        private val filename: String?,
        private val contentType: String?,
        private val content: ByteArray,
        private val empty: Boolean = false,
        private val size: Long? = null
    ) : MultipartFile {

        override fun getName(): String = "file"
        override fun getOriginalFilename(): String? = filename
        override fun getContentType(): String? = contentType
        override fun isEmpty(): Boolean = empty
        override fun getSize(): Long = size ?: content.size.toLong()
        override fun getBytes(): ByteArray = content
        override fun getInputStream(): InputStream = ByteArrayInputStream(content)

        override fun transferTo(dest: java.io.File) {
            dest.writeBytes(content)
        }

        override fun transferTo(dest: java.nio.file.Path) {
            java.nio.file.Files.write(dest, content)
        }
    }

    // ===== TESTS PARA validatePdfFile() =====

    @Test
    fun should_validate_valid_pdf_file_successfully() {
        val file = TestMultipartFile(
            filename = "test.pdf",
            contentType = "application/pdf",
            content = "valid pdf content".toByteArray()
        )

        // No debe lanzar excepción
        fileValidationService.validatePdfFile(file)
    }

    @Test
    fun should_throw_exception_when_file_is_empty() {
        val file = TestMultipartFile(
            filename = "test.pdf",
            contentType = "application/pdf",
            content = byteArrayOf(),
            empty = true
        )

        val exception = assertFailsWith<InvalidFileTypeException> {
            fileValidationService.validatePdfFile(file)
        }
        assertEquals("El archivo está vacío", exception.message)
    }

    @Test
    fun should_throw_exception_when_file_is_too_large() {
        val file = TestMultipartFile(
            filename = "large.pdf",
            contentType = "application/pdf",
            content = "content".toByteArray(),
            size = 6 * 1024 * 1024L // 6MB > 5MB limit
        )

        val exception = assertFailsWith<InvalidFileTypeException> {
            fileValidationService.validatePdfFile(file)
        }
        assertTrue(exception.message!!.contains("El archivo es demasiado grande"))
        assertTrue(exception.message!!.contains("5MB"))
    }

    @Test
    fun should_throw_exception_when_content_type_is_not_pdf() {
        val file = TestMultipartFile(
            filename = "document.pdf",
            contentType = "text/plain",
            content = "content".toByteArray()
        )

        val exception = assertFailsWith<InvalidFileTypeException> {
            fileValidationService.validatePdfFile(file)
        }
        assertTrue(exception.message!!.contains("Tipo de archivo no permitido"))
        assertTrue(exception.message!!.contains("text/plain"))
    }

    @Test
    fun should_throw_exception_when_content_type_is_null() {
        val file = TestMultipartFile(
            filename = "document.pdf",
            contentType = null,
            content = "content".toByteArray()
        )

        val exception = assertFailsWith<InvalidFileTypeException> {
            fileValidationService.validatePdfFile(file)
        }
        assertTrue(exception.message!!.contains("Tipo de archivo no permitido"))
        assertTrue(exception.message!!.contains("desconocido"))
    }

    @Test
    fun should_throw_exception_when_extension_is_not_pdf() {
        val file = TestMultipartFile(
            filename = "document.txt",
            contentType = "application/pdf",
            content = "content".toByteArray()
        )

        val exception = assertFailsWith<InvalidFileTypeException> {
            fileValidationService.validatePdfFile(file)
        }
        assertTrue(exception.message!!.contains("El archivo debe tener extensión .pdf"))
        assertTrue(exception.message!!.contains("txt"))
    }

    @Test
    fun should_throw_exception_when_filename_is_null() {
        val file = TestMultipartFile(
            filename = null,
            contentType = "application/pdf",
            content = "content".toByteArray()
        )

        val exception = assertFailsWith<InvalidFileTypeException> {
            fileValidationService.validatePdfFile(file)
        }
        assertTrue(exception.message!!.contains("El archivo debe tener extensión .pdf"))
    }

    @Test
    fun should_throw_exception_when_filename_has_no_extension() {
        val file = TestMultipartFile(
            filename = "filename_without_extension",
            contentType = "application/pdf",
            content = "content".toByteArray()
        )

        val exception = assertFailsWith<InvalidFileTypeException> {
            fileValidationService.validatePdfFile(file)
        }
        assertTrue(exception.message!!.contains("El archivo debe tener extensión .pdf"))
    }

    @Test
    fun should_accept_uppercase_pdf_extension() {
        val file = TestMultipartFile(
            filename = "document.PDF",
            contentType = "application/pdf",
            content = "content".toByteArray()
        )

        // No debe lanzar excepción
        fileValidationService.validatePdfFile(file)
    }

    @Test
    fun should_accept_mixed_case_pdf_extension() {
        val file = TestMultipartFile(
            filename = "document.Pdf",
            contentType = "application/pdf",
            content = "content".toByteArray()
        )

        // No debe lanzar excepción
        fileValidationService.validatePdfFile(file)
    }

    @Test
    fun should_accept_file_at_maximum_size_limit() {
        val file = TestMultipartFile(
            filename = "max_size.pdf",
            contentType = "application/pdf",
            content = "content".toByteArray(),
            size = 5 * 1024 * 1024L // Exactly 5MB
        )

        // No debe lanzar excepción
        fileValidationService.validatePdfFile(file)
    }

    @Test
    fun should_reject_file_just_over_size_limit() {
        val file = TestMultipartFile(
            filename = "oversized.pdf",
            contentType = "application/pdf",
            content = "content".toByteArray(),
            size = 5 * 1024 * 1024L + 1 // 5MB + 1 byte
        )

        assertFailsWith<InvalidFileTypeException> {
            fileValidationService.validatePdfFile(file)
        }
    }

    // ===== TESTS PARA validatePdfContent() =====

    @Test
    fun should_validate_pdf_content_with_valid_magic_bytes() {
        // PDF magic bytes: %PDF
        val pdfContent = byteArrayOf(0x25, 0x50, 0x44, 0x46) + "rest of pdf content".toByteArray()
        val file = TestMultipartFile(
            filename = "test.pdf",
            contentType = "application/pdf",
            content = pdfContent
        )

        // No debe lanzar excepción
        fileValidationService.validatePdfContent(file)
    }

    @Test
    fun should_throw_exception_when_pdf_magic_bytes_are_invalid() {
        val invalidContent = "Not a PDF file".toByteArray()
        val file = TestMultipartFile(
            filename = "fake.pdf",
            contentType = "application/pdf",
            content = invalidContent
        )

        val exception = assertFailsWith<InvalidFileTypeException> {
            fileValidationService.validatePdfContent(file)
        }
        assertEquals("El archivo no es un PDF válido", exception.message)
    }

    @Test
    fun should_throw_exception_when_file_is_too_small_for_magic_bytes() {
        val tinyContent = byteArrayOf(0x25, 0x50, 0x44) // Only 3 bytes
        val file = TestMultipartFile(
            filename = "tiny.pdf",
            contentType = "application/pdf",
            content = tinyContent
        )

        val exception = assertFailsWith<InvalidFileTypeException> {
            fileValidationService.validatePdfContent(file)
        }
        assertEquals("El archivo está corrupto o es demasiado pequeño", exception.message)
    }

    @Test
    fun should_validate_basic_checks_before_magic_bytes() {
        // File que falla validación básica (archivo vacío)
        val file = TestMultipartFile(
            filename = "test.pdf",
            contentType = "application/pdf",
            content = byteArrayOf(),
            empty = true
        )

        val exception = assertFailsWith<InvalidFileTypeException> {
            fileValidationService.validatePdfContent(file)
        }
        assertEquals("El archivo está vacío", exception.message)
    }

    // ===== TESTS PARA formatFileSize() =====

    @Test
    fun should_format_bytes_correctly() {
        assertEquals("512.0 KB", fileValidationService.formatFileSize(512 * 1024))
    }

    @Test
    fun should_format_kilobytes_correctly() {
        assertEquals("1.5 KB", fileValidationService.formatFileSize(1536)) // 1.5 KB
    }

    @Test
    fun should_format_megabytes_correctly() {
        assertEquals("2.5 MB", fileValidationService.formatFileSize(2621440)) // 2.5 MB
    }

    @Test
    fun should_format_exact_megabyte() {
        assertEquals("1.0 MB", fileValidationService.formatFileSize(1024 * 1024))
    }

    @Test
    fun should_format_small_files_in_kb() {
        assertEquals("0.5 KB", fileValidationService.formatFileSize(512)) // ✅ 512 bytes = 0.5 KB
    }

    @Test
    fun should_format_very_small_files() {
        assertEquals("0.01 KB", fileValidationService.formatFileSize(10)) // ✅ CORREGIDO: 10 bytes = "0.01 KB"
    }

    @Test
    fun should_format_zero_bytes() {
        assertEquals("0.0 KB", fileValidationService.formatFileSize(0)) // ✅ 0 bytes sí debería ser "0.0 KB"
    }

    @Test
    fun should_format_large_files() {
        assertEquals("10.0 MB", fileValidationService.formatFileSize(10 * 1024 * 1024))
    }

    // ===== TESTS PARA isPdfFile() =====

    @Test
    fun should_return_true_for_pdf_content_type() {
        assertTrue(fileValidationService.isPdfFile("application/pdf"))
    }

    @Test
    fun should_return_false_for_non_pdf_content_type() {
        assertFalse(fileValidationService.isPdfFile("text/plain"))
        assertFalse(fileValidationService.isPdfFile("image/jpeg"))
        assertFalse(fileValidationService.isPdfFile("application/json"))
    }

    @Test
    fun should_return_false_for_null_content_type() {
        assertFalse(fileValidationService.isPdfFile(null))
    }

    @Test
    fun should_return_false_for_empty_content_type() {
        assertFalse(fileValidationService.isPdfFile(""))
    }

    @Test
    fun should_be_case_sensitive_for_content_type() {
        assertFalse(fileValidationService.isPdfFile("APPLICATION/PDF"))
        assertFalse(fileValidationService.isPdfFile("Application/PDF"))
        assertFalse(fileValidationService.isPdfFile("application/PDF"))
    }

    // ===== TESTS PARA getFileInfo() =====

    @Test
    fun should_get_complete_file_info() {
        val file = TestMultipartFile(
            filename = "test_document.pdf",
            contentType = "application/pdf",
            content = "test content for file info".toByteArray()
        )

        val fileInfo = fileValidationService.getFileInfo(file)

        assertEquals("test_document.pdf", fileInfo.originalName)
        assertEquals(26L, fileInfo.size) // ✅ CORREGIDO: "test content for file info" tiene 26 caracteres
        assertEquals("application/pdf", fileInfo.contentType)
        assertEquals("pdf", fileInfo.extension)
        assertEquals(false, fileInfo.isEmpty)
        assertTrue(fileInfo.sizeFormatted.isNotEmpty())
    }

    @Test
    fun should_get_file_info_with_null_filename() {
        val file = TestMultipartFile(
            filename = null,
            contentType = "application/pdf",
            content = "content".toByteArray()
        )

        val fileInfo = fileValidationService.getFileInfo(file)

        assertEquals("archivo_sin_nombre", fileInfo.originalName)
        assertEquals("", fileInfo.extension)
    }

    @Test
    fun should_get_file_info_with_null_content_type() {
        val file = TestMultipartFile(
            filename = "test.pdf",
            contentType = null,
            content = "content".toByteArray()
        )

        val fileInfo = fileValidationService.getFileInfo(file)

        assertEquals("desconocido", fileInfo.contentType)
    }

    @Test
    fun should_get_file_info_with_empty_file() {
        val file = TestMultipartFile(
            filename = "empty.pdf",
            contentType = "application/pdf",
            content = byteArrayOf(),
            empty = true
        )

        val fileInfo = fileValidationService.getFileInfo(file)

        assertEquals("empty.pdf", fileInfo.originalName)
        assertEquals(0L, fileInfo.size)
        assertEquals(true, fileInfo.isEmpty)
    }

    @Test
    fun should_extract_extension_correctly() {
        val testCases = mapOf(
            "document.pdf" to "pdf",
            "file.txt" to "txt",
            "archive.tar.gz" to "gz",
            "no_extension" to "",
            "hidden.file.pdf" to "pdf",
            ".hidden" to "hidden", // ✅ CORREGIDO: ".hidden" tiene extensión "hidden"
            "file." to ""
        )

        testCases.forEach { (filename, expectedExtension) ->
            val file = TestMultipartFile(
                filename = filename,
                contentType = "application/pdf",
                content = "content".toByteArray()
            )

            val fileInfo = fileValidationService.getFileInfo(file)
            assertEquals(expectedExtension, fileInfo.extension, "Failed for filename: $filename")
        }
    }

    // ===== EDGE CASES Y ERROR HANDLING =====

    @Test
    fun should_handle_file_with_very_long_filename() {
        val longFilename = "a".repeat(1000) + ".pdf"
        val file = TestMultipartFile(
            filename = longFilename,
            contentType = "application/pdf",
            content = "content".toByteArray()
        )

        // No debe lanzar excepción
        fileValidationService.validatePdfFile(file)

        val fileInfo = fileValidationService.getFileInfo(file)
        assertEquals(longFilename, fileInfo.originalName)
    }

    @Test
    fun should_handle_file_with_special_characters_in_filename() {
        val specialFilename = "test file (1) - copy [2023].pdf"
        val file = TestMultipartFile(
            filename = specialFilename,
            contentType = "application/pdf",
            content = "content".toByteArray()
        )

        // No debe lanzar excepción
        fileValidationService.validatePdfFile(file)

        val fileInfo = fileValidationService.getFileInfo(file)
        assertEquals(specialFilename, fileInfo.originalName)
        assertEquals("pdf", fileInfo.extension)
    }

    @Test
    fun should_handle_file_with_unicode_characters() {
        val unicodeFilename = "documento_español_测试.pdf"
        val file = TestMultipartFile(
            filename = unicodeFilename,
            contentType = "application/pdf",
            content = "content".toByteArray()
        )

        // No debe lanzar excepción
        fileValidationService.validatePdfFile(file)

        val fileInfo = fileValidationService.getFileInfo(file)
        assertEquals(unicodeFilename, fileInfo.originalName)
    }

    @Test
    fun should_validate_file_at_exact_boundaries() {
        // Test exactly at the size limit (5MB)
        val exactSizeContent = ByteArray(5 * 1024 * 1024) { 'x'.code.toByte() } // ✅ CORREGIDO: Crear ByteArray real de 5MB
        val exactSizeFile = TestMultipartFile(
            filename = "exact.pdf",
            contentType = "application/pdf",
            content = exactSizeContent
        )

        // Should not throw exception
        fileValidationService.validatePdfFile(exactSizeFile)
    }

    // ===== INTEGRATION TESTS =====

    @Test
    fun should_validate_complete_workflow() {
        val file = TestMultipartFile(
            filename = "workflow_test.pdf",
            contentType = "application/pdf",
            content = "workflow test content".toByteArray()
        )

        // All methods should work together without conflicts
        assertTrue(fileValidationService.isPdfFile(file.contentType))
        fileValidationService.validatePdfFile(file)

        val fileInfo = fileValidationService.getFileInfo(file)
        assertTrue(fileInfo.sizeFormatted.isNotEmpty())
        assertFalse(fileInfo.isEmpty)
    }

    @Test
    fun should_provide_detailed_error_messages() {
        val oversizedFile = TestMultipartFile(
            filename = "big.pdf",
            contentType = "application/pdf",
            content = "content".toByteArray(),
            size = 10 * 1024 * 1024L // 10MB
        )

        val exception = assertFailsWith<InvalidFileTypeException> {
            fileValidationService.validatePdfFile(oversizedFile)
        }

        // Verify error message contains useful info
        assertTrue(exception.message!!.contains("5MB"))
        assertTrue(exception.message!!.contains("10.0 MB")) // ✅ VERIFICADO: Current file size correcto
    }
}