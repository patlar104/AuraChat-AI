package com.aurachat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for Hilt dependency injection.
 * @HiltAndroidApp triggers Hilt's code generation and sets up the component hierarchy.
 * Referenced in AndroidManifest.xml via android:name=".AuraChatApplication"
 */
@HiltAndroidApp
class AuraChatApplication : Application()
