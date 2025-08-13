package com.partituresforall.partitures.controllers


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.partituresforall.partitures.models.requests.AdvanceSearchRequest
import com.partituresforall.partitures.models.requests.CreateSheetWithFileRequest
import com.partituresforall.partitures.models.requests.UpdateSheetRequest
import com.partituresforall.partitures.models.responses.SheetResponse
import com.partituresforall.partitures.services.SheetService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDateTime
import kotlin.test.assertEquals
import org.mockito.Mockito.`when`

@WebMvcTest(SheetController::class)
@Import(SheetMockConfig::class)
class SheetControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var sheetService: SheetService

    private lateinit var objectMapper: ObjectMapper

    private val sampleSheetResponse = SheetResponse(
        id = 1L,
        title = "Test Sheet",
        description = "Test Description",
        artist = "Test Artist",
        genre = "Rock",
        instrument = "Guitar",
        isPublic = true,
        ownerId = 1L,
        pdfFilename = "test.pdf",
        pdfSize = 1024L,
        pdfContentType = "application/pdf",
        pdfDownloadUrl = "http://example.com/test.pdf",
        pdfSizeMB = "1.0",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setup() {
        objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    // ===== CREATE SHEET TESTS =====
    @Test
    fun `should create sheet with file successfully`() {
        val file = MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".toByteArray())

        `when`(
            sheetService.createSheetWithFile(any(CreateSheetWithFileRequest::class.java), any(MockMultipartFile::class.java))
        ).thenReturn(sampleSheetResponse)

        val result = mockMvc.multipart("/api/sheets") {
            file(file)
            param("title", "Test Sheet")
            param("description", "Test Description")
            param("artist", "Test Artist")
            param("genre", "Rock")
            param("instrument", "Guitar")
            param("isPublic", "true")
            param("ownerId", "1")
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("Test Sheet") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    // ===== GET SHEET TESTS =====
    @Test
    fun `should get sheet by id successfully`() {
        `when`(sheetService.getSheetById(1L)).thenReturn(sampleSheetResponse)

        val result = mockMvc.get("/api/sheets/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(1) }
                jsonPath("$.title") { value("Test Sheet") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    // ===== UPDATE SHEET TESTS =====
    @Test
    fun `should update sheet successfully`() {
        val updateRequest = UpdateSheetRequest(
            title = "Updated Sheet",
            description = "Updated Desc",
            artist = "New Artist",
            genre = "Jazz",
            instrument = "Piano",
            isPublic = false
        )

        val updatedResponse = sampleSheetResponse.copy(title = "Updated Sheet")
        `when`(sheetService.updateSheet(1L, updateRequest)).thenReturn(updatedResponse)

        val json = objectMapper.writeValueAsString(updateRequest)

        val result = mockMvc.put("/api/sheets/1") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("Updated Sheet") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should update sheet file successfully`() {
        val file = MockMultipartFile("file", "updated.pdf", MediaType.APPLICATION_PDF_VALUE, "new content".toByteArray())
        val updatedResponse = sampleSheetResponse.copy(pdfFilename = "updated.pdf")

        `when`(sheetService.updateSheetFile(1L, file)).thenReturn(updatedResponse)

        val result = mockMvc.multipart("/api/sheets/1/file") {
            file(file)
            with { it.method = "PUT"; it }
        }.andExpect {
            status { isOk() }
            jsonPath("$.pdfFilename") { value("updated.pdf") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    // ===== DELETE SHEET TESTS =====
    @Test
    fun `should delete sheet successfully`() {
        val result = mockMvc.delete("/api/sheets/1")
            .andExpect {
                status { isOk() }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    // ===== PDF VIEW & DOWNLOAD TESTS =====
    @Test
    fun `should view sheet pdf successfully`() {
        `when`(sheetService.getSheetById(1L)).thenReturn(sampleSheetResponse)
        `when`(sheetService.getSheetPdfContent(1L)).thenReturn("PDF bytes".toByteArray())

        val result = mockMvc.get("/api/sheets/1/pdf")
            .andExpect {
                status { isOk() }
                header { string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"test.pdf\"") }
                content { contentType(MediaType.APPLICATION_PDF) }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should download sheet pdf successfully`() {
        `when`(sheetService.getSheetById(1L)).thenReturn(sampleSheetResponse)
        `when`(sheetService.getSheetPdfContent(1L)).thenReturn("PDF bytes".toByteArray())

        val result = mockMvc.get("/api/sheets/1/pdf/download")
            .andExpect {
                status { isOk() }
                header { string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test.pdf\"") }
                content { contentType(MediaType.APPLICATION_PDF) }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    // ===== PUBLIC & SEARCH TESTS =====
    @Test
    fun `should return public sheets`() {
        `when`(sheetService.getPublicSheets()).thenReturn(listOf(sampleSheetResponse))

        val result = mockMvc.get("/api/sheets/public")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].title") { value("Test Sheet") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should search sheets successfully`() {
        `when`(sheetService.searchSheets("rock")).thenReturn(listOf(sampleSheetResponse))

        val result = mockMvc.get("/api/sheets/search?q=rock")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].genre") { value("Rock") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    // ===== FILTERS TESTS =====
    @Test
    fun `should get sheets by genre`() {
        `when`(sheetService.getSheetsByGenre("Rock")).thenReturn(listOf(sampleSheetResponse))
        val result = mockMvc.get("/api/sheets/genre/Rock").andExpect { status { isOk() } }.andReturn()
        assertEquals(200, result.response.status)
    }

    @Test
    fun `should get sheets by instrument`() {
        `when`(sheetService.getSheetsByInstrument("Guitar")).thenReturn(listOf(sampleSheetResponse))
        val result = mockMvc.get("/api/sheets/instrument/Guitar").andExpect { status { isOk() } }.andReturn()
        assertEquals(200, result.response.status)
    }

    @Test
    fun `should get sheets by artist`() {
        `when`(sheetService.getSheetsByArtist("Test Artist")).thenReturn(listOf(sampleSheetResponse))
        val result = mockMvc.get("/api/sheets/artist/Test%20Artist").andExpect { status { isOk() } }.andReturn()
        assertEquals(200, result.response.status)
    }

    @Test
    fun `should get available genres`() {
        `when`(sheetService.getAvailableGenres()).thenReturn(listOf("Rock", "Jazz"))
        val result = mockMvc.get("/api/sheets/filters/genres").andExpect { status { isOk() } }.andReturn()
        assertEquals(200, result.response.status)
    }

    @Test
    fun `should get available instruments`() {
        `when`(sheetService.getAvailableInstruments()).thenReturn(listOf("Guitar", "Piano"))
        val result = mockMvc.get("/api/sheets/filters/instruments").andExpect { status { isOk() } }.andReturn()
        assertEquals(200, result.response.status)
    }

    @Test
    fun `should get available artists`() {
        `when`(sheetService.getAvailableArtists()).thenReturn(listOf("Test Artist"))
        val result = mockMvc.get("/api/sheets/filters/artists").andExpect { status { isOk() } }.andReturn()
        assertEquals(200, result.response.status)
    }

    // ===== USER SHEETS TESTS =====
    @Test
    fun `should get user owned sheets`() {
        `when`(sheetService.getUserOwnedSheets(1L)).thenReturn(listOf(sampleSheetResponse))
        val result = mockMvc.get("/api/sheets/users/1/owned").andExpect { status { isOk() } }.andReturn()
        assertEquals(200, result.response.status)
    }

    // ===== FAVORITES TESTS =====
    @Test
    fun `should add to favorites`() {
        val result = mockMvc.post("/api/sheets/1/favorites?userId=1").andExpect { status { isOk() } }.andReturn()
        assertEquals(200, result.response.status)
    }

    @Test
    fun `should remove from favorites`() {
        val result = mockMvc.delete("/api/sheets/1/favorites?userId=1").andExpect { status { isOk() } }.andReturn()
        assertEquals(200, result.response.status)
    }

    @Test
    fun `should get favorites`() {
        `when`(sheetService.getFavoriteSheets(1L)).thenReturn(listOf(sampleSheetResponse))
        val result = mockMvc.get("/api/sheets/users/1/favorites").andExpect { status { isOk() } }.andReturn()
        assertEquals(200, result.response.status)
    }

    @Test
    fun `should check if sheet is favorite`() {
        `when`(sheetService.isSheetFavorite(1L, 1L)).thenReturn(true)
        val result = mockMvc.get("/api/sheets/1/is-favorite?userId=1")
            .andExpect { status { isOk() } }
            .andReturn()
        assertEquals(200, result.response.status)
    }

    // ===== ADVANCED SEARCH TESTS =====
    @Test
    fun `should perform advanced search`() {
        `when`(sheetService.advancedSearch(AdvanceSearchRequest("rock", "Test Artist", "Rock", "Guitar", "recent")))
            .thenReturn(listOf(sampleSheetResponse))

        val result = mockMvc.get("/api/sheets/search/advanced?searchTerm=rock&artist=Test%20Artist&genre=Rock&instrument=Guitar&sortBy=recent")
            .andExpect { status { isOk() } }
            .andReturn()

        assertEquals(200, result.response.status)
    }

    // ===== RECENT & TRENDING TESTS =====
    @Test
    fun `should get recent sheets`() {
        `when`(sheetService.getRecentSheets()).thenReturn(listOf(sampleSheetResponse))
        val result = mockMvc.get("/api/sheets/recent").andExpect { status { isOk() } }.andReturn()
        assertEquals(200, result.response.status)
    }

    @Test
    fun `should get trending sheets`() {
        `when`(sheetService.getRecentSheets()).thenReturn(listOf(sampleSheetResponse))
        val result = mockMvc.get("/api/sheets/trending").andExpect { status { isOk() } }.andReturn()
        assertEquals(200, result.response.status)
    }
}

@TestConfiguration
class SheetMockConfig {
    @Bean
    fun sheetService(): SheetService = mock(SheetService::class.java)
}