package com.partituresforall.partitures.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.partituresforall.partitures.exceptions.exceptions.users.DuplicateEmailException
import com.partituresforall.partitures.exceptions.exceptions.users.InvalidPasswordException
import com.partituresforall.partitures.exceptions.exceptions.users.UserNotFoundException
import com.partituresforall.partitures.models.requests.CreateUserRequest
import com.partituresforall.partitures.models.requests.UpdateUserRequest
import com.partituresforall.partitures.models.responses.UserResponse
import com.partituresforall.partitures.services.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
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
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)

    }

    private val baseUrl = "/api/users"

    @Test
    fun should_return_user_when_get_by_id() {
        val user = UserResponse(
            id = 1L,
            name = "Test User",
            email = "test@example.com",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        `when`(userService.getUserById(1L)).thenReturn(user)

        val result = mockMvc.get("$baseUrl/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Test User") }
                jsonPath("$.email") { value("test@example.com") }
                jsonPath("$.id") { value(1) }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun should_return_404_when_get_a_non_existent_user() {
        val userId = 999L

        `when`(userService.getUserById(userId)).thenThrow(UserNotFoundException(userId))

        val result = mockMvc.get("$baseUrl/999")
            .andExpect {
                status { isNotFound() }
            }.andReturn()

        assertEquals(404, result.response.status)
    }

    @Test
    fun should_create_user_when_post() {
        val request = CreateUserRequest(
            name = "New User",
            email = "newuser@example.com",
            password = "password123"
        )

        val response = UserResponse(
            id = 2L,
            name = "New User",
            email = "newuser@example.com",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        `when`(userService.createUser(request)).thenReturn(response)

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post(baseUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(2) }
            jsonPath("$.name") { value("New User") }
            jsonPath("$.email") { value("newuser@example.com") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun should_return_400_when_create_with_duplicate_email() {
        val request = CreateUserRequest(
            name = "Duplicate User",
            email = "duplicate@example.com",
            password = "password123"
        )

        `when`(userService.createUser(request)).thenThrow(DuplicateEmailException(request.email))

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post(baseUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isBadRequest() }
        }.andReturn()

        assertEquals(400, result.response.status)
    }

    @Test
    fun should_return_400_when_create_with_invalid_password() {
        val request = CreateUserRequest(
            name = "Invalid User",
            email = "invalid@example.com",
            password = "123"
        )

        `when`(userService.createUser(request)).thenThrow(InvalidPasswordException())

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.post(baseUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isBadRequest() }
        }.andReturn()

        assertEquals(400, result.response.status)
    }

    @Test
    fun should_update_user_when_put() {
        val request = UpdateUserRequest(
            name = "Updated User",
            email = "updated@example.com",
            password = "newpassword123"
        )

        val response = UserResponse(
            id = 1L,
            name = "Updated User",
            email = "updated@example.com",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        `when`(userService.updateUser(1L, request)).thenReturn(response)

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.put("$baseUrl/1") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Updated User") }
            jsonPath("$.email") { value("updated@example.com") }
        }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun should_return_404_when_update_non_existent_user() {
        val request = UpdateUserRequest(name = "Updated", email = "example@gmail.com", password = "1234567897")

        `when`(userService.updateUser(999L, request)).thenThrow(UserNotFoundException(999L))

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.put("$baseUrl/999") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isNotFound() }
        }.andReturn()

        assertEquals(404, result.response.status)
    }

    @Test
    fun should_return_400_when_update_with_duplicate_email() {
        val request = UpdateUserRequest(
            name = "ejemplo",
            email = "duplicate@example.com",
            password = "jasdjaifhihfihwehdoew"
        )

        `when`(userService.updateUser(1L, request)).thenThrow(DuplicateEmailException(request.email!!))

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.put("$baseUrl/1") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isBadRequest() }
        }.andReturn()

        assertEquals(400, result.response.status)
    }

    @Test
    fun should_return_400_when_update_with_invalid_password() {
        val request = UpdateUserRequest(
            name = "ejemplo",
            email = "ejemplo2@gmail.com",
            password = "123"
        )

        `when`(userService.updateUser(1L, request)).thenThrow(InvalidPasswordException())

        val json = objectMapper.writeValueAsString(request)

        val result = mockMvc.put("$baseUrl/1") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isBadRequest() }
        }.andReturn()

        assertEquals(400, result.response.status)
    }

    @Test
    fun should_delete_user_when_delete() {
        doNothing().`when`(userService).deleteUser(1L)

        val result = mockMvc.delete("$baseUrl/1")
            .andExpect {
                status { isOk() }
            }.andReturn()

        assertEquals(200, result.response.status)
    }

    @Test
    fun should_return_404_when_delete_non_existent_user() {
        doThrow(UserNotFoundException(999L)).`when`(userService).deleteUser(999L)

        val result = mockMvc.delete("$baseUrl/999")
            .andExpect {
                status { isNotFound() }
            }.andReturn()

        assertEquals(404, result.response.status)

    }

}


@TestConfiguration
class UserMockConfig {
    @Bean
    fun userService(): UserService = mock(UserService::class.java)
}
