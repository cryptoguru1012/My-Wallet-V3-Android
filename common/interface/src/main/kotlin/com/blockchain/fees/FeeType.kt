package com.blockchain.fees

sealed class FeeType {
    object Regular : FeeType()
    object Priority : FeeType()
}
