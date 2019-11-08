package com.epam.drill.plugin.api.processing

import com.epam.drill.session.*

object DrillContext : IDrillContex {
    override operator fun invoke(): String? = DrillRequest.threadStorage.get()?.drillSessionId
    override operator fun get(key: String): String? = DrillRequest.threadStorage.get()?.get(key.toLowerCase())
}

interface IDrillContex {
    operator fun invoke(): String?
    operator fun get(key: String): String?
}