package com.fittrack.app.ui.premium

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.*
import com.fittrack.app.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingClient: BillingClient
) : BaseViewModel() {

    private val _subscriptionPlans = MutableLiveData<List<ProductDetails>>()
    val subscriptionPlans: LiveData<List<ProductDetails>> = _subscriptionPlans

    private val _isPurchaseSuccessful = MutableLiveData<Boolean>()
    val isPurchaseSuccessful: LiveData<Boolean> = _isPurchaseSuccessful

    private val _selectedPlan = MutableLiveData<SubscriptionPlan>()
    val selectedPlan: LiveData<SubscriptionPlan> = _selectedPlan

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySubscriptionPlans()
                } else {
                    _error.value = "Failed to setup billing: ${billingResult.debugMessage}"
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                _error.value = "Billing service disconnected"
            }
        })
    }

    private fun querySubscriptionPlans() {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("premium_monthly")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("premium_yearly")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _subscriptionPlans.value = productDetailsList
            } else {
                _error.value = "Failed to query subscription plans: ${billingResult.debugMessage}"
            }
        }
    }

    fun selectPlan(plan: SubscriptionPlan) {
        _selectedPlan.value = plan
    }

    fun purchaseSubscription() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val selectedPlan = _selectedPlan.value ?: throw IllegalStateException("No plan selected")
                val productId = when (selectedPlan) {
                    SubscriptionPlan.MONTHLY -> "premium_monthly"
                    SubscriptionPlan.YEARLY -> "premium_yearly"
                }

                val productDetails = _subscriptionPlans.value?.find { it.productId == productId }
                    ?: throw IllegalStateException("Product details not found")

                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                    ?: throw IllegalStateException("Offer token not found")

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    )
                    .build()

                // Launch the billing flow
                val billingResult = billingClient.launchBillingFlow(getActivity(), billingFlowParams)
                
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    throw Exception("Failed to launch billing flow: ${billingResult.debugMessage}")
                }

            } catch (e: Exception) {
                _error.value = "Failed to process purchase: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun handlePurchaseResult(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            viewModelScope.launch {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        // Acknowledge the purchase if it hasn't been acknowledged yet
                        if (!purchase.isAcknowledged) {
                            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                            
                            val ackResult = billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                            
                            if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                _isPurchaseSuccessful.value = true
                            } else {
                                _error.value = "Failed to acknowledge purchase: ${ackResult.debugMessage}"
                            }
                        } else {
                            _isPurchaseSuccessful.value = true
                        }
                    }
                }
            }
        } else {
            _error.value = "Purchase failed: ${billingResult.debugMessage}"
        }
    }

    override fun onCleared() {
        super.onCleared()
        billingClient.endConnection()
    }
}

enum class SubscriptionPlan {
    MONTHLY,
    YEARLY
}