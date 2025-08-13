package com.partituresforall.partitures.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.partituresforall.partitures.config.PasswordEncoder
import com.partituresforall.partitures.exceptions.exceptions.users.DuplicateEmailException
import com.partituresforall.partitures.exceptions.exceptions.users.InvalidPasswordException
import com.partituresforall.partitures.exceptions.exceptions.users.UserNotFoundException
import com.partituresforall.partitures.models.requests.CreateUserRequest
import com.partituresforall.partitures.models.requests.UpdateUserRequest
import com.partituresforall.partitures.models.requests.UpdateProfileRequest
import com.partituresforall.partitures.models.responses.UserResponse
import com.partituresforall.partitures.models.responses.UserProfileResponse
import com.partituresforall.partitures.repositories.UserRepository
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDateTime
import kotlin.test.assertEquals

@WebMvcTest(UserController::class)
@Import(UserMockConfig::class)
class UserControllerTest {

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

    private val sampleProfileResponse = UserProfileResponse(
        id = 1L,
        name = "Test User",
        bio = "Test bio",
        createdAt = LocalDateTime.now()
    )

    // ===== CREATE USER TESTS =====

    @Test
    fun `should create user successfully`() {
        val request = CreateUserRequest(
            name = "Test User",
            email = "test@example.com",
            password = "TestPass123"
        )

        `when`(userService.createUser(request)).thenReturn(sampleUserResponse)

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post("/api/users") {
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
    fun `should return 400 when creating user with invalid password`() {
        val request = CreateUserRequest(
            name = "Test User",
            email = "test@example.com",
            password = "weak"
        )

        `when`(userService.createUser(request))
            .thenThrow(InvalidPasswordException("La contraseña debe tener al menos 8 caracteres"))

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isBadRequest() }
        }.andReturn()

        assertEquals(400, result.response.status)
    }

    @Test
    fun `should return 409 when creating user with duplicate email`() {
        val request = CreateUserRequest(
            name = "Test User",
            email = "existing@example.com",
            password = "TestPass123"
        )

        `when`(userService.createUser(request))
            .thenThrow(DuplicateEmailException("existing@example.com"))

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isConflict() }
        }.andReturn()

        assertEquals(409, result.response.status)
    }

    // ===== GET USER TESTS =====

    @Test
    fun `should get user by id successfully`() {
        `when`(userService.getUserById(1L)).thenReturn(sampleUserResponse)

        val result = mockMvc.get("/api/users/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(1) }
                jsonPath("$.name") { value("Test User") }
                jsonPath("$.email") { value("test@example.com") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should return 404 when user not found`() {
        `when`(userService.getUserById(999L))
            .thenThrow(UserNotFoundException(999L))

        val result = mockMvc.get("/api/users/999")
            .andExpect {
                status { isNotFound() }
            }.andReturn()

        assertEquals(404, result.response.status)
    }

    // ===== UPDATE USER TESTS =====

    @Test
    fun `should update user successfully`() {
        val updateRequest = UpdateUserRequest(
            name = "Updated Name",
            email = "updated@example.com",
            password = "NewPass123"
        )

        val updatedUserResponse = sampleUserResponse.copy(
            name = "Updated Name",
            email = "updated@example.com"
        )

        `when`(userService.updateUser(1L, updateRequest)).thenReturn(updatedUserResponse)

        val json = objectMapper.writeValueAsString(updateRequest)

        val result = mockMvc.put("/api/users/1") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Updated Name") }
            jsonPath("$.email") { value("updated@example.com") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should update user with partial data`() {
        val updateRequest = UpdateUserRequest(
            name = "Only Name Updated",
            email = null,
            password = null
        )

        val updatedUserResponse = sampleUserResponse.copy(name = "Only Name Updated")
        `when`(userService.updateUser(1L, updateRequest)).thenReturn(updatedUserResponse)

        val json = objectMapper.writeValueAsString(updateRequest)

        val result = mockMvc.put("/api/users/1") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Only Name Updated") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should return 404 when updating non-existing user`() {
        val updateRequest = UpdateUserRequest(
            name = "Test",
            email = null,
            password = null
        )

        `when`(userService.updateUser(999L, updateRequest))
            .thenThrow(UserNotFoundException(999L))

        val json = objectMapper.writeValueAsString(updateRequest)

        val result = mockMvc.put("/api/users/999") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isNotFound() }
        }.andReturn()

        assertEquals(404, result.response.status)
    }

    @Test
    fun `should return 400 when update user with invalid password`() {
        val updateRequest = UpdateUserRequest(
            name = "Test",
            email = null,
            password = "weak"
        )

        `when`(userService.updateUser(1L, updateRequest))
            .thenThrow(InvalidPasswordException("La contraseña debe contener al menos un número"))

        val json = objectMapper.writeValueAsString(updateRequest)

        val result = mockMvc.put("/api/users/1") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isBadRequest() }
        }.andReturn()

        assertEquals(400, result.response.status)
    }

    // ===== DELETE USER TESTS =====

    @Test
    fun `should delete user successfully`() {
        val result = mockMvc.delete("/api/users/1")
            .andExpect {
                status { isOk() }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should return 404 when deleting non-existing user`() {
        `when`(userService.deleteUser(999L))
            .thenThrow(UserNotFoundException(999L))

        val result = mockMvc.delete("/api/users/999")
            .andExpect {
                status { isNotFound() }
            }.andReturn()

        assertEquals(404, result.response.status)
    }

    // ===== GET USER PROFILE TESTS =====

    @Test
    fun `should get user profile successfully`() {
        `when`(userService.getUserProfile(1L)).thenReturn(sampleProfileResponse)

        val result = mockMvc.get("/api/users/1/profile")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(1) }
                jsonPath("$.name") { value("Test User") }
                jsonPath("$.bio") { value("Test bio") }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should return 404 when profile user not found`() {
        `when`(userService.getUserProfile(999L))
            .thenThrow(UserNotFoundException(999L))

        val result = mockMvc.get("/api/users/999/profile")
            .andExpect {
                status { isNotFound() }
            }.andReturn()

        assertEquals(404, result.response.status)
    }

    // ===== UPDATE PROFILE TESTS =====

    @Test
    fun `should update profile successfully`() {
        val updateRequest = UpdateProfileRequest(
            name = "Updated Profile Name",
            bio = "Updated bio"
        )

        val updatedUserResponse = sampleUserResponse.copy(
            name = "Updated Profile Name",
            bio = "Updated bio"
        )

        `when`(userService.updateProfile(1L, updateRequest)).thenReturn(updatedUserResponse)

        val json = objectMapper.writeValueAsString(updateRequest)

        val result = mockMvc.put("/api/users/profile") {
            contentType = MediaType.APPLICATION_JSON
            content = json
            param("userId", "1")
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Updated Profile Name") }
            jsonPath("$.bio") { value("Updated bio") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun `should return 404 when updating profile of non-existing user`() {
        val updateRequest = UpdateProfileRequest(
            name = "Test",
            bio = "Test bio"
        )

        `when`(userService.updateProfile(999L, updateRequest))
            .thenThrow(UserNotFoundException(999L))

        val json = objectMapper.writeValueAsString(updateRequest)

        val result = mockMvc.put("/api/users/profile") {
            contentType = MediaType.APPLICATION_JSON
            content = json
            param("userId", "999")
        }.andExpect {
            status { isNotFound() }
        }.andReturn()

        assertEquals(404, result.response.status)
    }
}

@TestConfiguration
class UserMockConfig {
    @Bean
    fun userService(): UserService = mock(UserService::class.java)

    @Bean
    fun userRepository(): UserRepository = mock(UserRepository::class.java)

    @Bean
    fun passwordEncoder(): PasswordEncoder = mock(PasswordEncoder::class.java)
}