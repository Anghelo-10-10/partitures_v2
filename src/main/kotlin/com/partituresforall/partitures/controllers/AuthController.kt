package com.partituresforall.partitures.controllers

import com.partituresforall.partitures.models.requests.LoginRequest
import com.partituresforall.partitures.models.responses.UserResponse
import com.partituresforall.partitures.services.UserService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService
) {

    @PostMapping("/login")
    fun loginUser(@RequestBody request: LoginRequest): UserResponse {
        return userService.loginUser(request)
    }
}