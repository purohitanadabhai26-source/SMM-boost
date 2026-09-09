package com.example.data

import android.content.Context
import com.example.data.dao.AppSettingsDao
import com.example.data.dao.OrderDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.WalletDao
import com.example.data.entities.AppSettingsEntity
import com.example.data.entities.OrderEntity
import com.example.data.entities.TransactionEntity
import com.example.data.entities.WalletEntity
import com.example.model.SmmService
import com.example.notification.OrderNotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class SmmRepository(
    private val orderDao: OrderDao,
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao,
    private val appSettingsDao: AppSettingsDao
) {
    val orders: Flow<List<OrderEntity>> = orderDao.getAllOrders()
    val transactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val pendingTransactions: Flow<List<TransactionEntity>> = transactionDao.getPendingTransactions()
    val wallet: Flow<WalletEntity?> = walletDao.getWallet()
    val settings: Flow<AppSettingsEntity?> = appSettingsDao.getSettings()

    suspend fun ensureInitialized() {
        val currentWallet = walletDao.getWalletSync()
        if (currentWallet == null) {
            walletDao.insertOrUpdateWallet(WalletEntity(id = 1, balance = 50.0))
        }

        val currentSettings = appSettingsDao.getSettingsSync()
        if (currentSettings == null) {
            appSettingsDao.insertOrUpdateSettings(
                AppSettingsEntity(
                    id = 1,
                    upiId = "anadabhai@phonepe",
                    payeeName = "SMM Boost",
                    ownerEmail = "purohitanadabhai26@gmail.com",
                    notificationsEnabled = true
                )
            )
        } else if (currentSettings.payeeName.contains("PUROHIT", ignoreCase = true)) {
            appSettingsDao.insertOrUpdateSettings(
                currentSettings.copy(payeeName = "SMM Boost")
            )
        }
    }

    suspend fun placeOrder(
        service: SmmService,
        targetLink: String,
        quantity: Int,
        context: Context
    ): Result<OrderEntity> {
        val charge = service.calculatePrice(quantity)
        val currentWallet = walletDao.getWalletSync() ?: WalletEntity(id = 1, balance = 0.0)

        if (currentWallet.balance < charge) {
            return Result.failure(
                IllegalStateException("Insufficient funds! Required: ₹${"%.2f".format(charge)}, Current Balance: ₹${"%.2f".format(currentWallet.balance)}. Please add money via UPI.")
            )
        }

        // Deduct from wallet
        val newBalance = currentWallet.balance - charge
        walletDao.insertOrUpdateWallet(currentWallet.copy(balance = newBalance))

        // Compute sequential order number starting around 81629
        val maxOrderNumber = orderDao.getMaxOrderNumber() ?: 81628L
        val nextOrderNumber = maxOrderNumber + 1

        val order = OrderEntity(
            orderNumber = nextOrderNumber,
            serviceId = service.id,
            serviceName = service.title,
            category = service.category.displayName,
            targetLink = targetLink,
            quantity = quantity,
            charge = charge,
            status = "PENDING",
            timestamp = System.currentTimeMillis()
        )

        val generatedId = orderDao.insertOrder(order)
        val savedOrder = order.copy(id = generatedId)

        // Send instant Android push notification to phone
        val currentSettings = appSettingsDao.getSettingsSync()
        if (currentSettings == null || currentSettings.notificationsEnabled) {
            OrderNotificationHelper.triggerOrderNotification(context, savedOrder)
        }

        return Result.success(savedOrder)
    }

    suspend fun submitDepositRequest(
        amount: Double,
        utrNumber: String,
        upiId: String,
        context: Context
    ): Result<TransactionEntity> {
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        }
        if (utrNumber.trim().length < 6) {
            return Result.failure(IllegalArgumentException("Please enter a valid 12-digit UTR/Transaction ID"))
        }

        val tx = TransactionEntity(
            utrNumber = utrNumber.trim(),
            amount = amount,
            upiId = upiId,
            status = "PENDING",
            timestamp = System.currentTimeMillis()
        )
        val txId = transactionDao.insertTransaction(tx)
        val savedTx = tx.copy(id = txId)

        // Notify owner immediately on phone with sound and vibration
        val currentSettings = appSettingsDao.getSettingsSync()
        if (currentSettings == null || currentSettings.notificationsEnabled) {
            OrderNotificationHelper.triggerDepositRequestNotification(context, savedTx)
        }

        return Result.success(savedTx)
    }

    suspend fun approveDeposit(transactionId: Long, context: Context): Result<TransactionEntity> {
        val tx = transactionDao.getTransactionById(transactionId)
            ?: return Result.failure(IllegalStateException("Transaction not found"))

        if (tx.status == "APPROVED" || tx.status == "SUCCESS") {
            return Result.failure(IllegalStateException("Transaction is already approved"))
        }

        val currentWallet = walletDao.getWalletSync() ?: WalletEntity(id = 1, balance = 0.0)
        val newBalance = currentWallet.balance + tx.amount
        walletDao.insertOrUpdateWallet(currentWallet.copy(balance = newBalance))

        transactionDao.updateStatus(transactionId, "APPROVED")
        val updatedTx = tx.copy(status = "APPROVED")

        // Notify that wallet was credited
        OrderNotificationHelper.triggerDepositApprovedNotification(context, updatedTx)

        return Result.success(updatedTx)
    }

    suspend fun rejectDeposit(transactionId: Long): Result<Unit> {
        val tx = transactionDao.getTransactionById(transactionId)
            ?: return Result.failure(IllegalStateException("Transaction not found"))
        transactionDao.updateStatus(transactionId, "REJECTED")
        return Result.success(Unit)
    }

    suspend fun directCreditWallet(amount: Double, note: String = "Admin Manual Credit"): Result<WalletEntity> {
        if (amount == 0.0) {
            return Result.failure(IllegalArgumentException("Amount cannot be zero"))
        }
        val currentWallet = walletDao.getWalletSync() ?: WalletEntity(id = 1, balance = 0.0)
        val newBalance = (currentWallet.balance + amount).coerceAtLeast(0.0)
        val updatedWallet = currentWallet.copy(balance = newBalance)
        walletDao.insertOrUpdateWallet(updatedWallet)

        val tx = TransactionEntity(
            utrNumber = "ADMIN-${System.currentTimeMillis() % 1000000}",
            amount = amount,
            upiId = note,
            status = "DIRECT_CREDIT",
            timestamp = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(tx)
        return Result.success(updatedWallet)
    }

    suspend fun addFunds(
        amount: Double,
        utrNumber: String,
        upiId: String
    ): Result<TransactionEntity> {
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        }
        if (utrNumber.trim().length < 6) {
            return Result.failure(IllegalArgumentException("Please enter a valid 12-digit UTR/Transaction ID"))
        }

        val currentWallet = walletDao.getWalletSync() ?: WalletEntity(id = 1, balance = 0.0)
        val newBalance = currentWallet.balance + amount
        walletDao.insertOrUpdateWallet(currentWallet.copy(balance = newBalance))

        val tx = TransactionEntity(
            utrNumber = utrNumber.trim(),
            amount = amount,
            upiId = upiId,
            status = "SUCCESS",
            timestamp = System.currentTimeMillis()
        )
        val txId = transactionDao.insertTransaction(tx)
        return Result.success(tx.copy(id = txId))
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: String) {
        orderDao.updateStatus(orderId, newStatus)
    }

    suspend fun updateAppSettings(upiId: String, payeeName: String, notificationsEnabled: Boolean) {
        val current = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
        appSettingsDao.insertOrUpdateSettings(
            current.copy(
                upiId = upiId.trim(),
                payeeName = payeeName.trim(),
                notificationsEnabled = notificationsEnabled
            )
        )
    }
}
