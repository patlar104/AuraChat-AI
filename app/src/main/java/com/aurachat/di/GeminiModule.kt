package com.aurachat.di

import com.aurachat.data.remote.GeminiDataSource
import com.aurachat.data.remote.GeminiDataSourceImpl
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

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel =
        FirebaseAI.getInstance(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-2.0-flash",
                generationConfig = generationConfig {
                    temperature = 0.7f
                    topK = 40
                    topP = 0.95f
                    maxOutputTokens = 8192
                }
            )
}

// @Binds requires abstract fun — separate abstract module (same pattern as RepositoryModule)
@Module
@InstallIn(SingletonComponent::class)
abstract class GeminiBindingModule {

    @Binds
    @Singleton
    abstract fun bindGeminiDataSource(impl: GeminiDataSourceImpl): GeminiDataSource
}
