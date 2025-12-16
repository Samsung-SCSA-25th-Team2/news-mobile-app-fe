package com.example.mynewsmobileappfe.core.network.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TokenRefreshOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TokenRefreshRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TokenRefreshApi
