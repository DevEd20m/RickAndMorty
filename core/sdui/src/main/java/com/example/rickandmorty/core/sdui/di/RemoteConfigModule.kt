package com.example.rickandmorty.core.sdui.di

import com.example.rickandmorty.core.sdui.BuildConfig
import com.example.rickandmorty.core.sdui.R
import com.example.rickandmorty.core.sdui.repository.ScreenConfigRepository
import com.example.rickandmorty.core.sdui.repository.ScreenConfigRepositoryImpl
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigModule {

    private const val FETCH_INTERVAL_PRODUCTION = 3600L
    private const val FETCH_INTERVAL_DEBUG = 0L

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig {
        val remoteConfig = Firebase.remoteConfig

        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) {
                FETCH_INTERVAL_DEBUG
            } else {
                FETCH_INTERVAL_PRODUCTION
            }
        }

        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        return remoteConfig
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenConfigRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScreenConfigRepository(
        impl: ScreenConfigRepositoryImpl
    ): ScreenConfigRepository
}