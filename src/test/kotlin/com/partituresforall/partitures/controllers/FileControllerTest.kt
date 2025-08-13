package com.partituresforall.partitures.controllers

import com.partituresforall.partitures.services.FileService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.assertEquals

@WebMvcTest(FileController::class)
@Import(FileMockConfig::class)
class FileControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var fileService: FileService

    @BeforeEach
    fun setup() {
        // Setup if needed
    }

    private fun createResourceMock(
        filename: String = "test.pdf",
        content: String = "fake pdf content",
        exists: Boolean = true
    ): Resource {
        return object : Resource {
            override fun exists(): Boolean = exists
            override fun getFilename(): String = filename
            override fun getFile(): File = File(filename)
            override fun getInputStream() = ByteArrayInputStream(content.toByteArray())
            override fun contentLength(): Long = content.length.toLong()
            override fun lastModified(): Long = System.currentTimeMillis()
            override fun createRelative(relativePath: String): Resource = this
            override fun getDescription(): String = "Mock resource: $filename"
            override fun getURI() = throw UnsupportedOperationException()
            override fun getURL() = throw UnsupportedOperationException()
            override fun isReadable(): Boolean = true
            override fun isOpen(): Boolean = false
        }
    }

    // ===== DOWNLOAD FILE TESTS =====

    @Test
    fun `should download file successfully with inline disposition`() {
        val resource = createResourceMock("document.pdf")
        `when`(fileService.loadFileAsResource("document.pdf")).thenReturn(resource)

        val result = mockMvc.get("/api/files/document.pdf")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/pdf") }
                header { string("Content-Disposition", "inline; filename=\"document.pdf\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should download file with unknown extension as octet stream`() {
        val resource = createResourceMock("unknown.xyz")
        `when`(fileService.loadFileAsResource("unknown.xyz")).thenReturn(resource)

        val result = mockMvc.get("/api/files/unknown.xyz")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/octet-stream") }
                header { string("Content-Disposition", "inline; filename=\"unknown.xyz\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    // ===== DOWNLOAD FILE AS ATTACHMENT TESTS =====

    @Test
    fun `should download file as attachment successfully`() {
        val resource = createResourceMock("document.pdf")
        `when`(fileService.loadFileAsResource("document.pdf")).thenReturn(resource)

        val result = mockMvc.get("/api/files/document.pdf/download")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/pdf") }
                header { string("Content-Disposition", "attachment; filename=\"document.pdf\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should download attachment with unknown extension as octet stream`() {
        val resource = createResourceMock("data.bin")
        `when`(fileService.loadFileAsResource("data.bin")).thenReturn(resource)

        val result = mockMvc.get("/api/files/data.bin/download")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/octet-stream") }
                header { string("Content-Disposition", "attachment; filename=\"data.bin\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    // ===== PDF SPECIFIC TESTS =====

    @Test
    fun `should get PDF file successfully with inline disposition`() {
        val resource = createResourceMock("sheet-music.pdf")
        `when`(fileService.loadFileAsResource("pdfs/sheet-music.pdf")).thenReturn(resource)

        val result = mockMvc.get("/api/files/pdfs/sheet-music.pdf")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/pdf") }
                header { string("Content-Disposition", "inline; filename=\"sheet-music.pdf\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should download PDF file as attachment successfully`() {
        val resource = createResourceMock("beethoven-sonata.pdf")
        `when`(fileService.loadFileAsResource("pdfs/beethoven-sonata.pdf")).thenReturn(resource)

        val result = mockMvc.get("/api/files/pdfs/beethoven-sonata.pdf/download")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/pdf") }
                header { string("Content-Disposition", "attachment; filename=\"beethoven-sonata.pdf\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    // ===== CONTENT TYPE DETERMINATION TESTS =====

    @Test
    fun `should handle files with dots in name correctly`() {
        val resource = createResourceMock("my.complex.file.name.pdf")
        `when`(fileService.loadFileAsResource("my.complex.file.name.pdf")).thenReturn(resource)

        val result = mockMvc.get("/api/files/my.complex.file.name.pdf")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/pdf") }
                header { string("Content-Disposition", "inline; filename=\"my.complex.file.name.pdf\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should handle files without extension`() {
        val resource = createResourceMock("README")
        `when`(fileService.loadFileAsResource("README")).thenReturn(resource)

        val result = mockMvc.get("/api/files/README")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/octet-stream") }
                header { string("Content-Disposition", "inline; filename=\"README\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should handle PDF files with simple names through PDF endpoint`() {
        val resource = createResourceMock("sonata.pdf")
        `when`(fileService.loadFileAsResource("pdfs/sonata.pdf")).thenReturn(resource)

        val result = mockMvc.get("/api/files/pdfs/sonata.pdf")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/pdf") }
                header { string("Content-Disposition", "inline; filename=\"sonata.pdf\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    // ===== EDGE CASE TESTS =====

    @Test
    fun `should handle files with special characters in name`() {
        val specialFileName = "test-file.pdf"
        val resource = createResourceMock(specialFileName)
        `when`(fileService.loadFileAsResource(specialFileName)).thenReturn(resource)

        val result = mockMvc.get("/api/files/$specialFileName")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/pdf") }
                header { string("Content-Disposition", "inline; filename=\"$specialFileName\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should handle normal file names correctly`() {
        val fileName = "document.pdf"
        val resource = createResourceMock(fileName)
        `when`(fileService.loadFileAsResource(fileName)).thenReturn(resource)

        val result = mockMvc.get("/api/files/$fileName")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/pdf") }
                header { string("Content-Disposition", "inline; filename=\"$fileName\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should handle uppercase PDF extension`() {
        val fileName = "document.PDF"
        val resource = createResourceMock(fileName)
        `when`(fileService.loadFileAsResource(fileName)).thenReturn(resource)

        val result = mockMvc.get("/api/files/$fileName")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/pdf") }
                header { string("Content-Disposition", "inline; filename=\"$fileName\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should handle mixed case file names`() {
        val fileName = "Document.Pdf"
        val resource = createResourceMock(fileName)
        `when`(fileService.loadFileAsResource(fileName)).thenReturn(resource)

        val result = mockMvc.get("/api/files/$fileName")
            .andExpect {
                status { isOk() }
                header { string("Content-Type", "application/pdf") }
                header { string("Content-Disposition", "inline; filename=\"$fileName\"") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should handle multiple PDF files successfully`() {
        val files = listOf("file1.pdf", "file2.pdf", "file3.pdf")

        files.forEach { fileName ->
            val resource = createResourceMock(fileName)
            `when`(fileService.loadFileAsResource(fileName)).thenReturn(resource)

            val result = mockMvc.get("/api/files/$fileName")
                .andExpect {
                    status { isOk() }
                    header { string("Content-Type", "application/pdf") }
                    header { string("Content-Disposition", "inline; filename=\"$fileName\"") }
                }.andReturn()

            assertEquals(200, result.response.status)
        }
    }

    @Test
    fun `should handle different file extensions correctly`() {
        val testFiles = mapOf(
            "document.pdf" to "application/pdf",
            "image.jpg" to "application/octet-stream",
            "data.txt" to "application/octet-stream",
            "archive.zip" to "application/octet-stream"
        )

        testFiles.forEach { (fileName, expectedContentType) ->
            val resource = createResourceMock(fileName)
            `when`(fileService.loadFileAsResource(fileName)).thenReturn(resource)

            val result = mockMvc.get("/api/files/$fileName")
                .andExpect {
                    status { isOk() }
                    header { string("Content-Type", expectedContentType) }
                    header { string("Content-Disposition", "inline; filename=\"$fileName\"") }
                }.andReturn()

            assertEquals(200, result.response.status)
        }
    }
}

@TestConfiguration
class FileMockConfig {
    @Bean
    fun fileService(): FileService = mock(FileService::class.java)
}