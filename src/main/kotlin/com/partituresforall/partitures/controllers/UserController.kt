package com.partituresforall.partitures.controllers

import com.partituresforall.partitures.models.requests.CreateUserRequest
import com.partituresforall.partitures.models.requests.UpdateProfileRequest
import com.partituresforall.partitures.models.requests.UpdateUserRequest
import com.partituresforall.partitures.models.responses.UserProfileResponse
import com.partituresforall.partitures.models.responses.UserResponse
import com.partituresforall.partitures.models.responses.SheetResponse
import com.partituresforall.partitures.services.UserService
import com.partituresforall.partitures.services.SheetService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val sheetService: SheetService  // ✅ NUEVA DEPENDENCIA
) {
    @PostMapping
    fun createUser(@RequestBody request: CreateUserRequest): UserResponse {
        return userService.createUser(request)
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long): UserResponse {
        return userService.getUserById(id)
    }

    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @RequestBody request: UpdateUserRequest
    ): UserResponse {
        return userService.updateUser(id, request)
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long) {
        userService.deleteUser(id)
    }

    @GetMapping("/{id}/profile")
    fun getUserProfile(@PathVariable id: Long): UserProfileResponse {
        return userService.getUserProfile(id)
    }

    @PutMapping("/profile")
    fun updateMyProfile(
        @RequestParam userId: Long,
        @RequestBody request: UpdateProfileRequest
    ): UserResponse {
        return userService.updateProfile(userId, request)
    }

    // ✅ NUEVO ENDPOINT FALTANTE - Este era el problema
    @GetMapping("/{id}/sheets/public")
    fun getUserPublicSheets(@PathVariable id: Long): List<SheetResponse> {
        return sheetService.getUserPublicSheets(id)
    }
}