package com.blockchain.sunriver
import io.reactivex.Single

interface XlmHorizonUrlFetcher {
    fun xlmHorizonUrl(def: String): Single<String>
}