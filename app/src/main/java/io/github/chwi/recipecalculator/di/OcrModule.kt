package io.github.chwi.recipecalculator.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.chwi.recipecalculator.data.ocr.MlKitOcrService
import io.github.chwi.recipecalculator.data.ocr.OcrService

/** Binds [OcrService] to the ML Kit-backed implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class OcrModule {

    @Binds
    abstract fun bindOcrService(impl: MlKitOcrService): OcrService
}
