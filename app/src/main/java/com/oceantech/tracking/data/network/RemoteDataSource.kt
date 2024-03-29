package com.oceantech.tracking.data.network

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.oceantech.tracking.BuildConfig
import com.oceantech.tracking.utils.format
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.oceantech.tracking.data.model.Constants.Companion.TAG
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Singleton
class RemoteDataSource {

    companion object {
        const val BASE_URL = "http://timesheet-dev.oceantech.com.vn/timesheet/"
        const val CHECK_TOKEN_AUTH = "Basic Y29yZV9jbGllbnQ6c2VjcmV0"
        const val DEFAULT_CONTENT_TYPE = "application/json"
        const val AUTH_CONTENT_TYPE = "application/x-www-form-urlencoded"
        private const val DEFAULT_USER_AGENT = "Nimpe-Android"
    }

    fun <Api> buildAuthApi(
        api: Class<Api>,
        context: Context
    ): Api {
        val gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, UnitEpochDateTypeAdapter())
            .setLenient()
            .create()

        val sessionManager = SessionManager(context.applicationContext)
        var authenticator: TokenAuthenticator? = null

        sessionManager.fetchAuthToken()?.let {
            authenticator = TokenAuthenticator(it, context.applicationContext)
        }

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getAuthRetrofitClient(authenticator))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(api)
    }

    fun <Api> buildUserApi(api: Class<Api>, context: Context): Api {
        val gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, UnitEpochDateTypeAdapter())
            .setLenient()
            .create()

        val sessionManager = SessionManager(context.applicationContext)
        var authenticator: TokenAuthenticator? = null

        sessionManager.fetchAuthToken()?.let {
            authenticator = TokenAuthenticator(it, context.applicationContext)
        }

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getUserRetrofitClient(authenticator))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(api)
    }

    private fun getAuthRetrofitClient(
        authenticator: Authenticator? = null
    ): OkHttpClient {

        return getUnsafeOkHttpClient()
            .writeTimeout(31, TimeUnit.SECONDS)
            .readTimeout(31, TimeUnit.SECONDS)
            .connectTimeout(31, TimeUnit.SECONDS)
            .cookieJar(cookieJar())
            .addNetworkInterceptor(customAuthInterceptor())
            .also { client ->
                authenticator?.let { client.authenticator(it) }
                if (BuildConfig.DEBUG) {
                    client.addInterceptor(loggingInterceptor())
                }
            }
            .build()
    }

    private fun getUserRetrofitClient(authenticator: TokenAuthenticator? = null): OkHttpClient {
        Log.i(TAG, "Custom retrofit client: ${authenticator?.accessToken?.length}")
        return getUnsafeOkHttpClient()
            .writeTimeout(31, TimeUnit.SECONDS)
            .readTimeout(31, TimeUnit.SECONDS)
            .connectTimeout(31, TimeUnit.SECONDS)
            .cookieJar(cookieJar())

            .addNetworkInterceptor(customUserInterceptor(authenticator?.accessToken))
            .also { client ->
                authenticator?.let {
                    client.authenticator(it)
                }
                if (BuildConfig.DEBUG) {
                    client.addInterceptor(loggingInterceptor())
                }
            }
            .build()
    }

    private fun cookieJar(): CookieJar {
        val cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }

        return JavaNetCookieJar(cookieManager)
    }

    private fun loggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private fun customAuthInterceptor(): Interceptor {
        return Interceptor { chain: Interceptor.Chain ->
            val original = chain.request()

            // rewrite the request
            val request: Request = original.newBuilder()
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", DEFAULT_CONTENT_TYPE)
                .header("Content-Type", AUTH_CONTENT_TYPE)
                .method(original.method, original.body)
                .build()

            chain.proceed(request)
        }
    }

    private fun customUserInterceptor(accessToken: String?): Interceptor {
        return Interceptor { chain: Interceptor.Chain ->
            val original = chain.request()

            val request: Request = original.newBuilder()
                .also { request ->
                    accessToken?.let { request.header("Authorization", "Bearer $it") }
                }
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", DEFAULT_CONTENT_TYPE)
                .header("Content-Type", DEFAULT_CONTENT_TYPE)
                .method(original.method, original.body)
                .build()

            chain.proceed(request)
        }
    }

    inner class UnitEpochDateTypeAdapter : TypeAdapter<Date>() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun write(out: JsonWriter?, value: Date?) {
            if (value == null) {
                out?.nullValue()
            } else {
                out?.value(value.format("yyyy-MM-dd"))
            }
        }

        override fun read(_in: JsonReader?) =
            if (_in != null) {
                if (_in.peek() == JsonToken.NULL) {
                    _in.nextNull()
                    null
                } else {
                    Date(_in.nextLong())
                }
            } else null
    }

    private fun getUnsafeOkHttpClient(): OkHttpClient.Builder =
        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts: Array<TrustManager> = arrayOf(
                object : X509TrustManager {
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate?>?,
                        authType: String?
                    ) = Unit

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(
                        chain: Array<X509Certificate?>?,
                        authType: String?
                    ) = Unit

                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            // Install the all-trusting trust manager
            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(
                sslSocketFactory,
                trustAllCerts[0] as X509TrustManager
            )
            builder.hostnameVerifier { _, _ -> true }
            builder
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
}
