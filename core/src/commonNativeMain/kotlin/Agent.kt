package com.epam.drill.agent

import com.epam.drill.common.*
import com.epam.drill.common.ws.*
import com.epam.drill.plugin.api.processing.*


data class Agent(
    val adminAddress: URL,
    val secureAdminAddress: URL,
    val agentConfig: AgentConfig,
    val drillInstallationDir: String,
    val enabled: Boolean,
    var requestPattern: String? = null,
    var pstorage: MutableMap<String, AgentPart<*, *>> = mutableMapOf(),
    val pl: MutableMap<String, PluginMetadata> = mutableMapOf()
)