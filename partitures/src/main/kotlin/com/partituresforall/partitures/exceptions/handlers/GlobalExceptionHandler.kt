package com.partituresforall.partitures.exceptions.handlers

import com.partituresforall.partitures.exceptions.exceptions.sheets.InvalidPdfReferenceException
import com.partituresforall.partitures.exceptions.exceptions.sheets.SheetNotFoundException
import com.partituresforall.partitures.exceptions.exceptions.sheets.UnauthorizedSheetModificationException
import com.partituresforall.partitures.exceptions.exceptions.users.DuplicateEmailException
import com.partituresforall.partitures.exceptions.exceptions.users.InvalidPasswordException
import com.partituresforall.partitures.exceptions.exceptions.users.UserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    // ===== Users =====
    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(e: UserNotFoundException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
    }

    @ExceptionHandler(DuplicateEmailException::class)
    fun handleDuplicateEmail(e: DuplicateEmailException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.message)
    }

    @ExceptionHandler(InvalidPasswordException::class)
    fun handleInvalidPassword(e: InvalidPasswordException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
    }

    // ===== Sheets =====
    @ExceptionHandler(SheetNotFoundException::class)
    fun handleSheetNotFound(e: SheetNotFoundException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
    }

    @ExceptionHandler(UnauthorizedSheetModificationException::class)
    fun handleUnauthorizedSheetModification(e: UnauthorizedSheetModificationException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
    }

    @ExceptionHandler(InvalidPdfReferenceException::class)
    fun handleInvalidPdfReference(e: InvalidPdfReferenceException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
    }
}