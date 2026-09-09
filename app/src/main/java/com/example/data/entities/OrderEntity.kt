package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: Long, // e.g. 81629, 81630
    val serviceId: String,
    val serviceName: String,
    val category: String,
    val targetLink: String,
    val quantity: Int,
    val charge: Double,
    val status: String = "PENDING", // PENDING, PROCESSING, COMPLETED, CANCELLED
    val timestamp: Long = System.currentTimeMillis()
)
