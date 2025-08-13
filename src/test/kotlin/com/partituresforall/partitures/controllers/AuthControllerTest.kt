package com.partituresforall.partitures.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.partituresforall.partitures.models.requests.LoginRequest
import com.partituresforall.partitures.models.responses.UserResponse
import com.partituresforall.partitures.services.UserService
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
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime
import kotlin.test.assertEquals

@WebMvcTest(AuthController::class)
@Import(AuthMockConfig::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userService: UserService

    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private val sampleUserResponse = UserResponse(
        id = 1L,
        name = "Test User",
        email = "test@example.com",
        bio = "Test bio",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    // ===== LOGIN TESTS =====

    @Test
    fun `should login user successfully`() {
        val request = LoginRequest(
            email = "test@example.com",
            password = "TestPass123"
        )

        `when`(userService.loginUser(request)).thenReturn(sampleUserResponse)

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(1) }
            jsonPath("$.name") { value("Test User") }
            jsonPath("$.email") { value("test@example.com") }
            jsonPath("$.bio") { value("Test bio") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should login user with minimal data`() {
        val request = LoginRequest(
            email = "simple@example.com",
            password = "password123"
        )

        val simpleUserResponse = sampleUserResponse.copy(
            email = "simple@example.com",
            bio = null
        )

        `when`(userService.loginUser(request)).thenReturn(simpleUserResponse)

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("simple@example.com") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should login different users successfully`() {
        val users = listOf(
            Triple("user1@example.com", "password1", "User One"),
            Triple("user2@example.com", "password2", "User Two"),
            Triple("user3@example.com", "password3", "User Three")
        )

        users.forEachIndexed { index, (email, password, name) ->
            val request = LoginRequest(email = email, password = password)
            val userResponse = sampleUserResponse.copy(
                id = (index + 1).toLong(),
                email = email,
                name = name
            )

            `when`(userService.loginUser(request)).thenReturn(userResponse)

            val json = objectMapper.writeValueAsString(request)

            val result = mockMvc.post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json
            }.andExpect {
                status { isOk() }
                jsonPath("$.email") { value(email) }
                jsonPath("$.name") { value(name) }
            }.andReturn()

            assertEquals(200, result.response.status)
        }
    }

    @Test
    fun `should handle case insensitive email login`() {
        val request = LoginRequest(
            email = "TEST@EXAMPLE.COM",
            password = "TestPass123"
        )

        val userResponse = sampleUserResponse.copy(
            email = "test@example.com" // Service normalizes to lowercase
        )

        `when`(userService.loginUser(request)).thenReturn(userResponse)

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("test@example.com") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should login user with special characters in email`() {
        val request = LoginRequest(
            email = "user.name+tag@example.com",
            password = "TestPass123"
        )

        val userResponse = sampleUserResponse.copy(
            email = "user.name+tag@example.com"
        )

        `when`(userService.loginUser(request)).thenReturn(userResponse)

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("user.name+tag@example.com") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should handle login with long password`() {
        val request = LoginRequest(
            email = "test@example.com",
            password = "a".repeat(50) // Long password
        )

        `when`(userService.loginUser(request)).thenReturn(sampleUserResponse)

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("test@example.com") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should handle multiple login attempts for same user`() {
        val request = LoginRequest(
            email = "test@example.com",
            password = "TestPass123"
        )

        `when`(userService.loginUser(request)).thenReturn(sampleUserResponse)

        val json = objectMapper.writeValueAsString(request)

        // Simulate multiple login attempts
        repeat(3) {
            val result = mockMvc.post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json
            }.andExpect {
                status { isOk() }
                jsonPath("$.email") { value("test@example.com") }
            }.andReturn()

            assertEquals(200, result.response.status)
        }
    }

    @Test
    fun `should login with different password formats`() {
        val passwords = listOf(
            "simple123",
            "Complex@Pass123",
            "AnotherPassword456",
            "pass_with_underscore"
        )

        passwords.forEachIndexed { index, password ->
            val request = LoginRequest(
                email = "test$index@example.com",
                password = password
            )

            val userResponse = sampleUserResponse.copy(
                id = (index + 1).toLong(),
                email = "test$index@example.com"
            )

            `when`(userService.loginUser(request)).thenReturn(userResponse)

            val json = objectMapper.writeValueAsString(request)

            val result = mockMvc.post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json
            }.andExpect {
                status { isOk() }
                jsonPath("$.email") { value("test$index@example.com") }
            }.andReturn()

            assertEquals(200, result.response.status)
        }
    }

    @Test
    fun `should login with various email formats`() {
        val emails = listOf(
            "simple@example.com",
            "user.name@domain.co.uk",
            "test+label@gmail.com",
            "admin@subdomain.example.org"
        )

        emails.forEachIndexed { index, email ->
            val request = LoginRequest(
                email = email,
                password = "password123"
            )

            val userResponse = sampleUserResponse.copy(
                id = (index + 1).toLong(),
                email = email
            )

            `when`(userService.loginUser(request)).thenReturn(userResponse)

            val json = objectMapper.writeValueAsString(request)

            val result = mockMvc.post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = json
            }.andExpect {
                status { isOk() }
                jsonPath("$.email") { value(email) }
            }.andReturn()

            assertEquals(200, result.response.status)
        }
    }

    @Test
    fun `should return user with all fields on successful login`() {
        val request = LoginRequest(
            email = "complete@example.com",
            password = "CompletePass123"
        )

        val completeUserResponse = UserResponse(
            id = 42L,
            name = "Complete User",
            email = "complete@example.com",
            bio = "This is a complete user bio",
            createdAt = LocalDateTime.now().minusDays(30),
            updatedAt = LocalDateTime.now().minusHours(1)
        )

        `when`(userService.loginUser(request)).thenReturn(completeUserResponse)

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(42) }
            jsonPath("$.name") { value("Complete User") }
            jsonPath("$.email") { value("complete@example.com") }
            jsonPath("$.bio") { value("This is a complete user bio") }
            jsonPath("$.createdAt") { exists() }
            jsonPath("$.updatedAt") { exists() }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should handle login with standard password requirements`() {
        val request = LoginRequest(
            email = "secure@example.com",
            password = "SecurePassword123!"
        )

        val userResponse = sampleUserResponse.copy(
            email = "secure@example.com",
            name = "Secure User"
        )

        `when`(userService.loginUser(request)).thenReturn(userResponse)

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("secure@example.com") }
            jsonPath("$.name") { value("Secure User") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }
}

@TestConfiguration
class AuthMockConfig {
    @Bean
    fun userService(): UserService = mock(UserService::class.java)
}