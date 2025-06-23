package com.partituresforall.partitures.controllers
//hola mundo
import com.partituresforall.partitures.models.requests.CreateSheetRequest
import com.partituresforall.partitures.models.requests.UpdateSheetRequest
import com.partituresforall.partitures.models.responses.SheetResponse
import com.partituresforall.partitures.services.SheetService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sheets")
class SheetController(
    private val sheetService: SheetService
) {
    @PostMapping
    fun createSheet(@RequestBody request: CreateSheetRequest): SheetResponse {
        return sheetService.createSheet(request)
    }

    @GetMapping("/{id}")
    fun getSheet(@PathVariable id: Long): SheetResponse {
        return sheetService.getSheetById(id)
    }

    @PutMapping("/{id}")
    fun updateSheet(
        @PathVariable id: Long,
        @RequestBody request: UpdateSheetRequest
    ): SheetResponse {
        return sheetService.updateSheet(id, request)
    }

    @DeleteMapping("/{id}")
    fun deleteSheet(@PathVariable id: Long) {
        sheetService.deleteSheet(id)
    }

    @PostMapping("/{sheetId}/favorites")
    fun addToFavorites(
        @PathVariable sheetId: Long,
        @RequestParam userId: Long
    ) {
        sheetService.addToFavorites(userId, sheetId)
    }

    @DeleteMapping("/{sheetId}/favorites")
    fun removeFromFavorites(
        @PathVariable sheetId: Long,
        @RequestParam userId: Long
    ) {
        sheetService.removeFromFavorites(userId, sheetId)
    }

    @GetMapping("/users/{userId}/favorites")
    fun getFavorites(@PathVariable userId: Long): List<SheetResponse> {
        return sheetService.getFavoriteSheets(userId)
    }

    @GetMapping("/{sheetId}/is-favorite")
    fun isSheetFavorite(
        @PathVariable sheetId: Long,
        @RequestParam userId: Long
    ): Boolean {
        return sheetService.isSheetFavorite(userId, sheetId)
    }
}