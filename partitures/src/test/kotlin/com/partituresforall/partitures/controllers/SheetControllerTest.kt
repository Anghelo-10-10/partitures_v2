package com.partituresforall.partitures.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.partituresforall.partitures.models.requests.CreateSheetRequest
import com.partituresforall.partitures.models.requests.UpdateSheetRequest
import com.partituresforall.partitures.models.responses.SheetResponse
import com.partituresforall.partitures.services.SheetService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.*
import java.time.LocalDateTime
import kotlin.test.assertEquals



@WebMvcTest(SheetController::class)
@Import(SheetMockConfig::class)
class SheetControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var sheetService: SheetService

    private lateinit var objectMapper: ObjectMapper
    private val baseUrl = "/api/sheets"

    private val now = LocalDateTime.now()

    @BeforeEach
    fun setup() {
        objectMapper = ObjectMapper()
    }

    @Test
    fun `should create sheet`() {
        val request = CreateSheetRequest(
            title = "Sonata",
            description = "Ludwig van Beethoven",
            pdfReference = "sheet1.pdf",
            isPublic = true,
            ownerId = 1L
        )
        val response = SheetResponse(
            id = 1L,
            title = request.title,
            description = request.description,
            pdfReference = request.pdfReference,
            isPublic = request.isPublic,
            ownerId = request.ownerId,
            createdAt = now,
            updatedAt = now
        )

        `when`(sheetService.createSheet(any())).thenReturn(response)

        val result = mockMvc.post(baseUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("Sonata") }
            jsonPath("$.pdfReference") { value("sheet1.pdf") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should get sheet by id`() {
        val response = SheetResponse(1L, "Title", "Desc", "ref.pdf", true, 1L, now, now)
        `when`(sheetService.getSheetById(1L)).thenReturn(response)

        val result = mockMvc.get("$baseUrl/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(1) }
                jsonPath("$.title") { value("Title") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should update sheet`() {
        val request = UpdateSheetRequest("New Title", "Updated Desc", false)
        val response = SheetResponse(1L, "New Title", "Updated Desc", "ref.pdf", false, 1L, now, now)

        `when`(sheetService.updateSheet(eq(1L), any())).thenReturn(response)

        val result = mockMvc.put("$baseUrl/1") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("New Title") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should delete sheet`() {
        val result = mockMvc.delete("$baseUrl/1")
            .andExpect {
                status { isOk() }
            }.andReturn()

        verify(sheetService).deleteSheet(1L)
    }

    @Test
    fun `should add to favorites`() {
        mockMvc.post("$baseUrl/10/favorites?userId=99")
            .andExpect {
                status { isOk() }
            }

        verify(sheetService).addToFavorites(99L, 10L)
    }

    @Test
    fun `should remove from favorites`() {
        mockMvc.delete("$baseUrl/10/favorites?userId=99")
            .andExpect {
                status { isOk() }
            }

        verify(sheetService).removeFromFavorites(99L, 10L)
    }

    @Test
    fun `should get user favorites`() {
        val response = listOf(
            SheetResponse(1L, "One", "D1", "ref1.pdf", true, 99L, now, now),
            SheetResponse(2L, "Two", "D2", "ref2.pdf", false, 99L, now, now)
        )

        `when`(sheetService.getFavoriteSheets(99L)).thenReturn(response)

        val result = mockMvc.get("$baseUrl/users/99/favorites")
            .andExpect {
                status { isOk() }
                jsonPath("$.size()") { value(2) }
                jsonPath("$[0].title") { value("One") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should check if is favorite`() {
        `when`(sheetService.isSheetFavorite(99L, 3L)).thenReturn(true)

        val result = mockMvc.get("$baseUrl/3/is-favorite?userId=99")
            .andExpect {
                status { isOk() }
                content { string("true") }
            }.andReturn()

        assertEquals("true", result.response.contentAsString)
    }

}

@TestConfiguration
class SheetMockConfig {
    @Bean
    fun sheetService(): SheetService = mock(SheetService::class.java)
}
