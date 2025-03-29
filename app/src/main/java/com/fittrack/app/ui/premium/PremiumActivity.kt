package com.fittrack.app.ui.premium

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.fittrack.app.databinding.ActivityPremiumBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PremiumActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var binding: ActivityPremiumBinding
    private val viewModel: PremiumViewModel by viewModels()
    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBillingClient()
        setupSubscriptionSelection()
        setupSubscribeButton()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                } else {
                    showError("Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                showError("Billing service disconnected")
            }
        })
    }

    private fun setupSubscriptionSelection() {
        binding.subscriptionRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.monthlySubscription.id -> {
                    viewModel.selectPlan(SubscriptionPlan.MONTHLY)
                    updateSubscribeButtonText("Subscribe Monthly - $4.99")
                }
                binding.yearlySubscription.id -> {
                    viewModel.selectPlan(SubscriptionPlan.YEARLY)
                    updateSubscribeButtonText("Subscribe Yearly - $49.99")
                }
            }
        }
        
        // Set default selection
        binding.monthlySubscription.isChecked = true
        viewModel.selectPlan(SubscriptionPlan.MONTHLY)
        updateSubscribeButtonText("Subscribe Monthly - $4.99")
    }

    private fun setupSubscribeButton() {
        binding.subscribeButton.setOnClickListener {
            viewModel.purchaseSubscription()
        }
    }

    private fun updateSubscribeButtonText(text: String) {
        binding.subscribeButton.text = text
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.subscribeButton.isEnabled = !isLoading
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.isPurchaseSuccessful.observe(this) { isSuccessful ->
            if (isSuccessful) {
                showSuccess("Successfully subscribed to premium!")
                // Give some time for the user to see the success message
                binding.root.postDelayed({
                    finish()
                }, 1500)
            }
        }

        viewModel.subscriptionPlans.observe(this) { plans ->
            // Update UI with available plans if needed
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        viewModel.handlePurchaseResult(billingResult, purchases)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(android.R.color.holo_green_dark))
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingClient.endConnection()
    }
}