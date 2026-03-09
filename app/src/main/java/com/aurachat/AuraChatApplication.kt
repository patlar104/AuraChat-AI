package com.aurachat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point for Hilt dependency injection.
 * @HiltAndroidApp triggers Hilt's code generation and sets up the component hierarchy.
 * Referenced in AndroidManifest.xml via android:name=".AuraChatApplication"
 */
@HiltAndroidApp
class AuraChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for structured logging (debug builds only)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
