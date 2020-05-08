package com.epam.drill.core.transport

import com.epam.drill.*
import com.epam.drill.core.*
import com.epam.drill.hook.io.tcp.*
import com.epam.drill.interceptor.configureHttpInterceptor
import mu.*
import kotlin.native.SharedImmutable
import kotlin.native.concurrent.*

fun configureHttp() {
    configureHttpInterceptor()
    injectedHeaders.value = {
        val idHeaderPair = idHeaderPairFromConfig()
        val adminUrl = retrieveAdminUrl()
        mapOf(
            idHeaderPair,
            "drill-admin-url" to adminUrl
        ) + (drillRequest()?.headers?.filterKeys { it.startsWith("drill-") } ?: mapOf())

    }.freeze()
    readCallback.value = { bytes: ByteArray ->
        sessionStorage(bytes.decodeToString(), null)
    }.freeze()
    writeCallback.value = { _: ByteArray -> }.freeze()

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