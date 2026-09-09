package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet")
data class WalletEntity(
    @PrimaryKey
    val id: Int = 1,
    val balance: Double = 50.0 // Default initial credit for instant test convenience
)
