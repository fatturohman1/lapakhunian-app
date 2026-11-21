package com.lapakhunian.app.core.analytics

import com.lapakhunian.app.core.location.DeviceLocator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VisitorModule {
    @Binds
    @Singleton
    abstract fun bindVisitorSession(impl: VisitorPrefs): VisitorSession

    @Binds
    @Singleton
    abstract fun bindVisitorCoords(impl: DeviceLocator): VisitorCoords
}
