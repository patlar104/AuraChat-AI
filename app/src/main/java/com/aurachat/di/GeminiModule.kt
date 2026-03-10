package com.aurachat.di

import com.aurachat.data.remote.GeminiDataSource
import com.aurachat.data.remote.GeminiDataSourceImpl
import com.aurachat.util.Constants
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the Firebase AI [GenerativeModel] used for Gemini streaming.
 *
 * Model configuration values (temperature, topK, etc.) are sourced from
 * [Constants.Gemini] to keep them in one place.
 */
@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    /**
     * Provides the singleton [GenerativeModel] configured for Gemini 2.5 Flash
     * via the Firebase AI (Google AI) backend.
     */
    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel =
        FirebaseAI.getInstance(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = Constants.Gemini.MODEL_NAME,
                generationConfig = generationConfig {
                    temperature = Constants.Gemini.TEMPERATURE
                    topK = Constants.Gemini.TOP_K
                    topP = Constants.Gemini.TOP_P
                    maxOutputTokens = Constants.Gemini.MAX_OUTPUT_TOKENS
                },
            )
}

/**
 * Hilt module that binds [GeminiDataSourceImpl] as the [GeminiDataSource] implementation.
 *
 * [@Binds] requires an abstract function — same pattern as [RepositoryModule].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GeminiBindingModule {

    @Binds
    @Singleton
    abstract fun bindGeminiDataSource(impl: GeminiDataSourceImpl): GeminiDataSource
}
