import java.io.File
import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.android.compose.screenshot)
	alias(libs.plugins.android.junit5)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)
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
			isMinifyEnabled = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	buildFeatures {
		compose = true
		buildConfig = true
	}

	testOptions {
		unitTests.all {
			it.useJUnitPlatform()
		}
	}

	experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

// Project-level Kotlin toolchain — must be at top-level (Project receiver), not inside android {}
kotlin {
	jvmToolchain(17)
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
	implementation(libs.androidx.compose.material.icons.extended)

	// Navigation
	implementation(libs.androidx.navigation.compose)
	implementation(libs.kotlinx.serialization.json)

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
	testImplementation(platform(libs.junit.bom))
	testImplementation(libs.junit)
	testImplementation(libs.junit.jupiter)
	testImplementation(libs.junit.jupiter.params)
	testRuntimeOnly(libs.junit.platform.launcher)
	testRuntimeOnly(libs.junit.vintage.engine)
	testImplementation(libs.mockk)
	testImplementation(libs.turbine)
	testImplementation(libs.kotlinx.coroutines.test)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(libs.androidx.room.testing)
	androidTestImplementation(libs.turbine)
	androidTestImplementation(libs.kotlinx.coroutines.test)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.compose.ui.test.junit4)
	screenshotTestImplementation(platform(libs.androidx.compose.bom))
	screenshotTestImplementation(libs.androidx.compose.ui.tooling)
	screenshotTestImplementation(libs.androidx.compose.ui.test)
	screenshotTestImplementation(libs.screenshot.validation.api)
	debugImplementation(libs.androidx.compose.ui.tooling)
	debugImplementation(libs.androidx.compose.ui.test.manifest)
}
