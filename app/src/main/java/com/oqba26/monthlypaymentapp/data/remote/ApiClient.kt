package com.oqba26.monthlypaymentapp.data.remote

import com.oqba26.monthlypaymentapp.BuildConfig

object ApiClient {

    val pocketBase: PocketBaseClient by lazy {
        PocketBaseClient(baseUrl = BuildConfig.POCKETBASE_URL)
    }
}
