package com.bitcoinportfolio.domain.model

sealed class DataResult<out T> {
    data class Success<T>(val data: T, val isStale: Boolean = false, val staleAsOf: Long? = null) : DataResult<T>()
    data class Error(val message: String) : DataResult<Nothing>()
    object Loading : DataResult<Nothing>()
}
