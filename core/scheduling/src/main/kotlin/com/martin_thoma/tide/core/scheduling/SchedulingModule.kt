package com.martin_thoma.tide.core.scheduling

import com.martin_thoma.tide.core.domain.SystemTimeSource
import com.martin_thoma.tide.core.domain.TimeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** A scope that lives as long as the process, for work that must not stop with a screen. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object SchedulingModule {
    @Provides
    fun timeSource(): TimeSource = SystemTimeSource

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
