package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val upiId: String = "anadabhai@phonepe",
    val payeeName: String = "SMM Boost",
    val ownerEmail: String = "purohitanadabhai26@gmail.com",
    val notificationsEnabled: Boolean = true
)
