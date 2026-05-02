package com.bitcoinportfolio.data.secure

interface SecureStorage {
    fun isSetupComplete(): Boolean
    fun markSetupComplete()
    fun savePinHash(hash: String, salt: String)
    fun getPinHash(): String?
    fun getPinSalt(): String?
    fun saveBtcAmount(amount: Double)
    fun getBtcAmount(): Double?
    fun saveSpoofAmount(amount: Double)
    fun getSpoofAmount(): Double?
    fun clearAll()
}
