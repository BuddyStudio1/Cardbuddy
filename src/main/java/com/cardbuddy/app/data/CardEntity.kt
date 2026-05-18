package com.cardbuddy.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storeName: String,
    val barcodeNumber: String,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val logoUrl: String? = null,
    val barcodeFormat: Int = -1, // From ML Kit Barcode.BarcodeFormat
    val notes: String = "",
    val hexColor: String? = null,
    val category: String = "Other"
)