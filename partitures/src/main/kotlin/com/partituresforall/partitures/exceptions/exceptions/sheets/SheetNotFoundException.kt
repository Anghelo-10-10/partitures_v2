package com.partituresforall.partitures.exceptions.exceptions.sheets

class SheetNotFoundException(val sheetId: Long) :
    RuntimeException("Partitura con ID $sheetId no encontrada")