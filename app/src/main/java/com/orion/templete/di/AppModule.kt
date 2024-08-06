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
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object  AppModule {
    @Provides
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
    @Provides
    @Singleton
    fun provideOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = SecureStorage(context).getToken()
                val originalRequest = chain.request()
                val newRequestBuilder = originalRequest.newBuilder()
                // Add the Authorization header only if the token is not null
                token?.let {
                    newRequestBuilder.header("Authorization", "Bearer $it")
                }
                val newRequest = newRequestBuilder.build()
                chain.proceed(newRequest)
            }
            .build()
    }
    @Provides
    @Singleton
    fun provideApiService(okHttpClient: OkHttpClient): ApiService {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(baseurl)
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

}