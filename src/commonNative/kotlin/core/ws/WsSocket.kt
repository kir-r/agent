/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.core.ws

import com.benasher44.uuid.*
import com.epam.drill.*
import com.epam.drill.common.*
import com.epam.drill.logger.*
import com.epam.drill.transport.*
import com.epam.drill.transport.common.ws.*
import com.epam.drill.ws.*
import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlin.coroutines.*
import kotlin.time.*

@SharedImmutable
private val wsLogger = Logging.logger("DrillWebsocket")

private val dispatcher = newSingleThreadContext("sender coroutine")


class WsSocket : CoroutineScope {

    override val coroutineContext: CoroutineContext = dispatcher

    private val errorMessage = atomic("")
    private val isInstanceIdGenerated = atomic(false)

    init {
        topicRegister()
    }

    fun connect(adminUrl: String) {
        val url = URL("$adminUrl/agent/attach")
        if (agentConfig.instanceId.isEmpty()) {
            isInstanceIdGenerated.update { true }
            wsLogger.debug { "InstanceId will be generated on each WS connection" }
        }
        checkAndGenerateInstanceId()
        headers = {
            mutableMapOf(
                AgentConfigParam to ProtoBuf.encodeToHexString(AgentConfig.serializer(), agentConfig),
                NeedSyncParam to pstorage.none().toString(),
                HttpHeaders.ContentEncoding to "deflate"
            )
        }
        wsLogger.info { "connecting with instanceId '${agentConfig.instanceId}'..." }
        val wsClient = WSClientFactory.createClient(url)
        ws.value = wsClient
        wsClient.onOpen {
            wsLogger.info { "Agent connected with instanceId '${agentConfig.instanceId}'" }
            errorMessage.update { "" }
        }

        wsClient.onBinaryMessage { rawMessage ->
            val message = rawMessage.toWsMessage()
            val destination = message.destination
            val topic = WsRouter[destination]
            if (topic != null) {
                launch {
                    when (topic) {
                        is PluginTopic -> {
                            val pluginMetadata = ProtoBuf.decodeFromByteArray(PluginBinary.serializer(), message.data)
                            val duration = measureTime { topic.block(pluginMetadata.meta, pluginMetadata.data) }
                            wsLogger.debug { "'$destination' took $duration" }
                            Sender.send(Message(MessageType.MESSAGE_DELIVERED, "/agent/load"))
                        }
                        is InfoTopic -> {
                            val duration = measureTime { topic.run(message.data) }
                            wsLogger.debug { "'$destination' took $duration" }
                            Sender.send(
                                Message(
                                    MessageType.MESSAGE_DELIVERED,
                                    destination
                                )
                            )
                        }
                        is GenericTopic<*> -> {
                            val duration = measureTime { topic.deserializeAndRun(message.data) }
                            wsLogger.debug { "'$destination' took $duration" }
                            Sender.send(
                                Message(
                                    MessageType.MESSAGE_DELIVERED,
                                    destination
                                )
                            )
                        }
                    }
                }
            } else {
                wsLogger.warn { "topic with name '$destination' didn't register" }
            }
        }

        wsClient.onError { message ->
            if (errorMessage.value != message) {
                wsLogger.error { "[Duplicates in debug] WS error: $message" }
                errorMessage.update { message }
            } else {
                wsLogger.debug { "WS error: $message" }
            }
        }
        wsClient.onClose {
            checkAndGenerateInstanceId()
            wsLogger.info { "Websocket closed. On next connection instanceId will be '${agentConfig.instanceId}'" }
            errorMessage.update { "" }
        }

    }

    private fun checkAndGenerateInstanceId() {
        if (isInstanceIdGenerated.value) {
            agentConfig = agentConfig.copy(instanceId = uuid4().toString())
        }
    }

}

private fun ByteArray.toWsMessage() = ProtoBuf.decodeFromByteArray(Message.serializer(), this)
