package com.codewiz.wordloop.di

import com.codewiz.wordloop.BuildConfig
import com.codewiz.wordloop.data.api.ApiError
import com.codewiz.wordloop.data.api.ApiErrorBody
import com.codewiz.wordloop.data.api.TrackEventApi
import com.codewiz.wordloop.data.api.WordLoopApi
import com.codewiz.wordloop.data.auth.AuthRepository
import com.codewiz.wordloop.data.prefs.UserPrefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun firebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun firebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Singleton
    fun okHttp(prefs: UserPrefs, authRepository: AuthRepository, json: Json): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HostSelectionInterceptor(prefs))
            .addInterceptor(AuthHeaderInterceptor(authRepository, json))
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("${BuildConfig.PRODUCTION_BASE_URL}/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun wordLoopApi(retrofit: Retrofit): WordLoopApi = retrofit.create(WordLoopApi::class.java)

    @Provides
    @Singleton
    fun trackEventApi(json: Json): TrackEventApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl("${BuildConfig.PRODUCTION_BASE_URL}/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TrackEventApi::class.java)
    }
}

class HostSelectionInterceptor(
    private val prefs: UserPrefs,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val base = runBlocking { prefs.baseUrl() }.toHttpUrl()
        val original = chain.request()
        val newUrl = original.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()
        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}

class AuthHeaderInterceptor(
    private val authRepository: AuthRepository,
    private val json: Json,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { runCatching { authRepository.currentIdToken() }.getOrNull() }
        val builder = chain.request().newBuilder()
            .header("Content-Type", "application/json")
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }
        val authorized = builder.build()
        val response = chain.proceed(authorized)
        if (response.code != 401 || token == null) return response
        response.close()
        val fresh = runBlocking { runCatching { authRepository.currentIdToken(forceRefresh = true) }.getOrNull() }
            ?: return chain.proceed(authorized)
        val retried = authorized.newBuilder()
            .header("Authorization", "Bearer $fresh")
            .build()
        return chain.proceed(retried)
    }
}

fun readApiError(json: Json, body: String?, status: Int): ApiError {
    if (body.isNullOrBlank()) return ApiError.UnexpectedStatus(status)
    val parsed = runCatching { json.decodeFromString(ApiErrorBody.serializer(), body) }.getOrNull()
    return ApiError.fromBody(status, parsed)
}
