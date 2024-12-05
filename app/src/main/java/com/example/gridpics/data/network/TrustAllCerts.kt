package com.example.gridpics.data.network

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@SuppressLint("CustomX509TrustManager")
class TrustAllCerts: X509TrustManager
{
	override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
	override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
	override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}

fun getUnsafeOkHttpClient(): OkHttpClient
{
	val trustAllCerts = arrayOf<TrustManager>(TrustAllCerts())
	val sslContext = SSLContext.getInstance("SSL")
	sslContext.init(null, trustAllCerts, java.security.SecureRandom())
	val sslSocketFactory = sslContext.socketFactory
	return OkHttpClient.Builder()
		.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
		.hostnameVerifier { _, _ -> true }
		.build()
}