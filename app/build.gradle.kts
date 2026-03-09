import java.io.File
import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.hilt)
	alias(libs.plugins.ksp)
	alias(libs.plugins.google.services)
}

// Load local.properties to expose user's API key via BuildConfig (used in Settings screen)
val localProperties = Properties()
val localPropertiesFile: File = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
	localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
	namespace = "com.aurachat"
	compileSdk {
		version = release(36) {
			minorApiLevel = 1
		}
	}

	defaultConfig {
		applicationId = "com.aurachat"
		minSdk = 24
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

		// Exposes the API key to the Settings screen via BuildConfig
		buildConfigField(
			"String",
			"GEMINI_API_KEY",
			"\"${localProperties.getProperty("GEMINI_API_KEY", "")}\""
		)
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}

	buildFeatures {
		compose = true
		buildConfig = true
	}
}

// Project-level Kotlin toolchain — must be at top-level (Project receiver), not inside android {}
kotlin {
	jvmToolchain(11)
}

ksp {
	arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
	// AndroidX Core
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.lifecycle.runtime.compose)
	implementation(libs.androidx.lifecycle.viewmodel.compose)
	implementation(libs.androidx.activity.compose)

	// Compose BOM (manages all Compose versions)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.ui.graphics)
	implementation(libs.androidx.compose.ui.tooling.preview)
	implementation(libs.androidx.compose.material3)
	implementation(libs.androidx.compose.material.icons.core)

	// Navigation
	implementation(libs.androidx.navigation.compose)

	// Hilt (DI) — compiler uses KSP
	implementation(libs.hilt.android)
	ksp(libs.hilt.android.compiler)
	implementation(libs.hilt.navigation.compose)

	// Room (local database) — compiler uses KSP
	implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	ksp(libs.androidx.room.compiler)

	// DataStore (Settings persistence)
	implementation(libs.androidx.datastore.preferences)

	// Coil (image loading for attachments)
	implementation(libs.coil.compose)

	// EncryptedSharedPreferences (API key storage in Settings)
	implementation(libs.androidx.security.crypto)

	// Coroutines
	implementation(libs.kotlinx.coroutines.android)

	// Logging
	implementation(libs.timber)

	// Firebase BOM + Firebase AI Logic (primary Gemini integration)
	implementation(platform(libs.firebase.bom))
	implementation(libs.firebase.ai)

	// Testing
	testImplementation(libs.junit)
	testImplementation(libs.mockk)
	testImplementation(libs.turbine)
	testImplementation(libs.kotlinx.coroutines.test)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.compose.ui.test.junit4)
	debugImplementation(libs.androidx.compose.ui.tooling)
	debugImplementation(libs.androidx.compose.ui.test.manifest)
}
