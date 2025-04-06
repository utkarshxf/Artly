package com.orion.templete.di

import android.content.Context
import androidx.room.Room
import com.orion.templete.data.local.AppDatabase
import com.flashcall.me.data.local.dao.UserDao
import com.google.firebase.auth.FirebaseAuth
import com.orion.templete.data.network.ApiService
import com.orion.templete.data.network.ApiService.Companion.baseurl
import com.orion.templete.data.repository.AIRepositoryImplementation
import com.orion.templete.data.repository.ArtistRepositoryImplementation
import com.orion.templete.data.repository.ArtworkRepositoryImplementation
import com.orion.templete.data.repository.LoginRepositoryImplementation
import com.orion.templete.data.repository.UserRepositoryImplementation
import com.orion.templete.domain.repository.AIRepository
import com.orion.templete.domain.repository.ArtistRepository
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.domain.repository.LoginRepository
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.util.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
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
    fun providesFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
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
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // Remove this in production
            .build()
    }
    @Provides
    fun provideArtWorkRepository(apiService: ApiService): ArtworkRepository {
        return ArtworkRepositoryImplementation(apiService = apiService)
    }
    @Provides
    fun provideAIRepository(apiService: ApiService): AIRepository {
        return AIRepositoryImplementation(apiService = apiService)
    }
    @Provides
    fun provideUserRepository(
        apiService: ApiService,
        firebaseAuth: FirebaseAuth,
        @ApplicationContext context: Context
    ): LoginRepository {
        return LoginRepositoryImplementation(
            apiService = apiService,
            db = firebaseAuth,
            context = context
        )
    }
    @Provides
    fun artistRepository(
        apiService: ApiService,
    ): ArtistRepository {
        return ArtistRepositoryImplementation(
            apiService = apiService,
        )
    }
    @Provides
    fun userRepository(
        apiService: ApiService,
        userDao: UserDao,
        context: Context
    ): UserRepository {
        return UserRepositoryImplementation(apiService ,userDao ,context)
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
}