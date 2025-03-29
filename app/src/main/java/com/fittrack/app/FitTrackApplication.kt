package com.fittrack.app

import android.app.Application
import com.android.billingclient.api.BillingClient
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FitTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Any application-wide initialization can go here
    }
}