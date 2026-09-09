package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AppSettingsDao
import com.example.data.dao.OrderDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.WalletDao
import com.example.data.entities.AppSettingsEntity
import com.example.data.entities.OrderEntity
import com.example.data.entities.TransactionEntity
import com.example.data.entities.WalletEntity

@Database(
    entities = [
        OrderEntity::class,
        TransactionEntity::class,
        WalletEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smmxpert_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
