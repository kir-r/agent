package com.epam.drill

import co.touchlab.stately.collections.*
import com.epam.drill.common.*
import com.epam.drill.common.ws.*
import com.epam.drill.plugin.api.processing.*
import kotlin.native.concurrent.*

private val _requestPattern = AtomicReference<String?>(null).freeze()
private val _drillInstallationDir = AtomicReference<String?>(null).freeze()
private val _adminAddress = AtomicReference<URL?>(null).freeze()
private val _secureAdminAddress = AtomicReference<URL?>(null).freeze()
private val _agentConfig = AtomicReference<AgentConfig?>(null).freeze()


var requestPattern: String?
    get() = _requestPattern.value
    set(value) {
        _requestPattern.value = value.freeze()
    }

var drillInstallationDir: String?
    get() = _drillInstallationDir.value
    set(value) {
        _drillInstallationDir.value = value.freeze()
    }

var adminAddress: URL?
    get() = _adminAddress.value
    set(value) {
        _adminAddress.value = value.freeze()
    }

var secureAdminAddress: URL?
    get() = _secureAdminAddress.value
    set(value) {
        _secureAdminAddress.value = value.freeze()
    }

var agentConfig: AgentConfig
    get() = _agentConfig.value!!
    set(value) {
        _agentConfig.value = value.freeze()
    }

@SharedImmutable
val pstorage = IsoMutableMap<String, AgentPart<*, *>>()

@SharedImmutable
val pl = IsoMutableMap<String, PluginMetadata>()
