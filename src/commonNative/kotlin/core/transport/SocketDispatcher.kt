package com.epam.drill.core.transport

import com.epam.drill.*
import com.epam.drill.core.*
import com.epam.drill.hook.io.tcp.*
import com.epam.drill.interceptor.configureHttpInterceptor
import mu.*
import kotlin.native.SharedImmutable
import kotlin.native.concurrent.*

@SharedImmutable
val httpRequestLogger = KotlinLogging.logger("http requestLogger")


fun configureHttp() {
    configureHttpInterceptor()
    injectedHeaders.value = {
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
    readCallback.value = { bytes: ByteArray ->
        sessionStorage(bytes.decodeToString())
        httpRequestLogger.debug { "READ" }
    }.freeze()
    writeCallback.value = { _: ByteArray -> httpRequestLogger.debug { "WRITE" } }.freeze()

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
