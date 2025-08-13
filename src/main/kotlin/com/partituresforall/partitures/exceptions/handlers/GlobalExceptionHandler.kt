package com.partituresforall.partitures.exceptions.handlers

import com.partituresforall.partitures.exceptions.exceptions.sheets.InvalidPdfReferenceException
import com.partituresforall.partitures.exceptions.exceptions.sheets.SheetNotFoundException
import com.partituresforall.partitures.exceptions.exceptions.sheets.UnauthorizedSheetModificationException
import com.partituresforall.partitures.exceptions.exceptions.users.DuplicateEmailException
import com.partituresforall.partitures.exceptions.exceptions.users.InvalidPasswordException
import com.partituresforall.partitures.exceptions.exceptions.users.UserNotFoundException
import com.partituresforall.partitures.exceptions.exceptions.files.FileNotFoundException
import com.partituresforall.partitures.exceptions.exceptions.files.FileStorageException
import com.partituresforall.partitures.exceptions.exceptions.files.InvalidFileTypeException
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

    @ExceptionHandler(FileStorageException::class)
    fun handleFileStorage(e: FileStorageException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message)
    }

    @ExceptionHandler(FileNotFoundException::class)
    fun handleFileNotFound(e: FileNotFoundException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
    }

    @ExceptionHandler(InvalidFileTypeException::class)
    fun handleInvalidFileType(e: InvalidFileTypeException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
    }
}