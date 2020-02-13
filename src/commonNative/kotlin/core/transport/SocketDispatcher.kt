package com.epam.drill.core.transport

import com.epam.drill.*
import com.epam.drill.core.*
import com.epam.drill.interceptor.configureHttpInterceptor
import com.epam.drill.interceptor.headersForInject
import mu.*
import com.epam.drill.interceptor.readHttpCallback
import com.epam.drill.interceptor.writeHttpCallback
import kotlin.native.SharedImmutable
import kotlin.native.concurrent.*

@SharedImmutable
val httpRequestLogger = KotlinLogging.logger("http requestLogger")


fun configureHttp() {
    configureHttpInterceptor()
    headersForInject.value = {
        val idHeaderPair = idHeaderPairFromConfig()
        val adminUrl = retrieveAdminUrl()
        val sessionId = drillSessionId()
        mapOf(
            idHeaderPair,
            "drill-admin-url" to adminUrl
        ) + if (sessionId != null)
            mapOf("drill-session-id" to sessionId)
        else emptyMap()
    }.freeze()
    readHttpCallback.value = { bytes: ByteArray ->
        sessionStorage(bytes.decodeToString())
        httpRequestLogger.debug { "READ" }
    }.freeze()
    writeHttpCallback.value = { _: ByteArray -> httpRequestLogger.debug { "WRITE" } }.freeze()

}

private fun idHeaderPairFromConfig(): Pair<String, String> = exec {
    when (val groupId = agentConfig.serviceGroupId) {
        "" -> "drill-agent-id" to agentConfig.id
        else -> "drill-group-id" to groupId
    }
}

private fun retrieveAdminUrl(): String {
    return exec {
        if (::secureAdminAddress.isInitialized) {
            secureAdminAddress.toUrlString(false)
        } else adminAddress.toUrlString(false)
    }.toString()
}

private fun generateId() =
    exec { if (agentConfig.serviceGroupId.isEmpty()) agentConfig.id else agentConfig.serviceGroupId }
