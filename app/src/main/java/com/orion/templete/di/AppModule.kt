package com.orion.templete.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.orion.templete.data.network.ApiService
import com.orion.templete.data.network.ApiService.Companion.baseurl
import com.orion.templete.data.repository.GetArtworkRepositoryImplementation
import com.orion.templete.data.repository.UserRepositoryImplementation
import com.orion.templete.domain.repository.GetArtworkRepository
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object  AppModule {
    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return Retrofit.Builder().baseUrl(baseurl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
    @Provides
    fun provideArtWorkRepository(apiService: ApiService): GetArtworkRepository {
        return GetArtworkRepositoryImplementation(apiService = apiService)
    }

    @Provides
    fun provideUserRepository(
        apiService: ApiService,
    ): UserRepository {
        return UserRepositoryImplementation(
            apiService = apiService,
        )
    }

    @Provides
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

}