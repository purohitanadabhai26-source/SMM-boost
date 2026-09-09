package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val utrNumber: String,
    val amount: Double,
    val upiId: String,
    val status: String = "SUCCESS",
    val timestamp: Long = System.currentTimeMillis()
)
