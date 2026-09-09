package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SmmRepository
import com.example.data.entities.AppSettingsEntity
import com.example.data.entities.OrderEntity
import com.example.data.entities.TransactionEntity
import com.example.data.entities.WalletEntity
import com.example.model.ServiceCategory
import com.example.model.SmmCatalog
import com.example.model.SmmService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrderFormState(
    val selectedCategory: ServiceCategory = ServiceCategory.INSTAGRAM_VIEWS,
    val selectedService: SmmService = SmmCatalog.services.first(),
    val targetLink: String = "",
    val quantity: Int = 500,
    val isSubmitting: Boolean = false,
    val orderSuccess: OrderEntity? = null,
    val error: String? = null
)

class SmmViewModel(
    application: Application,
    private val repository: SmmRepository
) : AndroidViewModel(application) {

    val orders: StateFlow<List<OrderEntity>> = repository.orders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTransactions: StateFlow<List<TransactionEntity>> = repository.pendingTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallet: StateFlow<WalletEntity?> = repository.wallet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<AppSettingsEntity?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _orderForm = MutableStateFlow(OrderFormState())
    val orderForm: StateFlow<OrderFormState> = _orderForm.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    fun onCategorySelected(category: ServiceCategory) {
        val firstServiceInCategory = SmmCatalog.services.firstOrNull { it.category == category }
            ?: SmmCatalog.services.first()
        _orderForm.value = _orderForm.value.copy(
            selectedCategory = category,
            selectedService = firstServiceInCategory
        )
    }

    fun onServiceSelected(service: SmmService) {
        _orderForm.value = _orderForm.value.copy(
            selectedCategory = service.category,
            selectedService = service
        )
    }

    fun onLinkChanged(link: String) {
        _orderForm.value = _orderForm.value.copy(
            targetLink = link,
            error = null
        )
    }

    fun onQuantityChanged(quantity: Int) {
        val safeQty = quantity.coerceAtLeast(1)
        _orderForm.value = _orderForm.value.copy(
            quantity = safeQty,
            error = null
        )
    }

    fun placeOrder(context: Context, onSuccess: (OrderEntity) -> Unit = {}) {
        val state = _orderForm.value
        val link = state.targetLink.trim()
        val service = state.selectedService
        val qty = state.quantity

        if (link.isBlank()) {
            _orderForm.value = state.copy(error = "Please enter target profile/post link or username")
            return
        }

        if (qty < service.minQuantity) {
            _orderForm.value = state.copy(error = "Minimum quantity for this service is ${service.minQuantity}")
            return
        }

        if (qty > service.maxQuantity) {
            _orderForm.value = state.copy(error = "Maximum quantity for this service is ${service.maxQuantity}")
            return
        }

        viewModelScope.launch {
            _orderForm.value = _orderForm.value.copy(isSubmitting = true, error = null)
            val result = repository.placeOrder(service, link, qty, context)
            result.onSuccess { order ->
                _orderForm.value = _orderForm.value.copy(
                    isSubmitting = false,
                    targetLink = "",
                    orderSuccess = order,
                    error = null
                )
                _snackbarMessage.value = "Order #ORD-${order.orderNumber} placed successfully!"
                onSuccess(order)
            }.onFailure { ex ->
                _orderForm.value = _orderForm.value.copy(
                    isSubmitting = false,
                    error = ex.message ?: "Failed to place order"
                )
            }
        }
    }

    fun submitDepositRequest(
        amount: Double,
        utrNumber: String,
        upiId: String,
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.submitDepositRequest(amount, utrNumber, upiId, context)
            result.onSuccess {
                _snackbarMessage.value = "Deposit request submitted! Funds will be credited after verification."
                onComplete(true)
            }.onFailure { ex ->
                _snackbarMessage.value = ex.message ?: "Failed to submit deposit request"
                onComplete(false)
            }
        }
    }

    fun approveDeposit(transactionId: Long, context: Context) {
        viewModelScope.launch {
            val result = repository.approveDeposit(transactionId, context)
            result.onSuccess { tx ->
                _snackbarMessage.value = "Approved! ₹${"%.2f".format(tx.amount)} credited to wallet."
            }.onFailure { ex ->
                _snackbarMessage.value = ex.message ?: "Approval failed"
            }
        }
    }

    fun rejectDeposit(transactionId: Long) {
        viewModelScope.launch {
            val result = repository.rejectDeposit(transactionId)
            result.onSuccess {
                _snackbarMessage.value = "Deposit request rejected."
            }.onFailure { ex ->
                _snackbarMessage.value = ex.message ?: "Action failed"
            }
        }
    }

    fun directCreditWallet(amount: Double, note: String) {
        viewModelScope.launch {
            val result = repository.directCreditWallet(amount, note)
            result.onSuccess {
                _snackbarMessage.value = "₹${"%.2f".format(amount)} successfully credited to wallet!"
            }.onFailure { ex ->
                _snackbarMessage.value = ex.message ?: "Direct credit failed"
            }
        }
    }

    fun addFunds(amount: Double, utrNumber: String, upiId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.addFunds(amount, utrNumber, upiId)
            result.onSuccess {
                _snackbarMessage.value = "₹${"%.2f".format(amount)} added to wallet successfully!"
                onComplete(true)
            }.onFailure { ex ->
                _snackbarMessage.value = ex.message ?: "Failed to add funds"
                onComplete(false)
            }
        }
    }

    fun updateOrderStatus(orderId: Long, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            _snackbarMessage.value = "Order status updated to $newStatus"
        }
    }

    fun updateUpiSettings(upiId: String, payeeName: String, notificationsEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateAppSettings(upiId, payeeName, notificationsEnabled)
            _snackbarMessage.value = "UPI & Admin settings updated successfully"
        }
    }

    fun clearOrderSuccess() {
        _orderForm.value = _orderForm.value.copy(orderSuccess = null)
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val repository = SmmRepository(
                        orderDao = db.orderDao(),
                        transactionDao = db.transactionDao(),
                        walletDao = db.walletDao(),
                        appSettingsDao = db.appSettingsDao()
                    )
                    return SmmViewModel(application, repository) as T
                }
            }
    }
}
