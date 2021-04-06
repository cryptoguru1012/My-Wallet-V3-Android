package com.blockchain.sunriver

import io.reactivex.Single

interface XlmTransactionTimeoutFetcher {
    fun transactionTimeout(): Single<Long>
}
