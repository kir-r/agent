package com.epam.drill.core.ws

import com.epam.drill.*
import com.epam.drill.common.*
import com.epam.drill.core.exceptions.*
import com.epam.drill.logger.*
import com.epam.drill.transport.exception.*
import com.epam.drill.transport.ws.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*
import kotlin.time.*

const val DELAY = 1000L

@SharedImmutable
private val wsLogger = Logging.logger("DrillWebsocket")

@SharedImmutable
private val dispatcher = newSingleThreadContext("sender coroutine")

private val attemptCounter = AtomicInt(0.freeze()).freeze()

class WsSocket(
    val onBinaryMessage: (suspend (ByteArray) -> Unit)? = null,
    val onStringMessage: (suspend (String) -> Unit)? = null,
    val onAnyMessage: (suspend (Any) -> Unit)? = null
) : CoroutineScope {

    val fallback: CoroutineContext = CoroutineExceptionHandler { _, ex ->
        when (ex) {
            is WsException -> wsLogger.error { "WS error: ${ex.message}" }
            is WsClosedException -> wsLogger.info { "WS closed" }
            else -> wsLogger.error(ex) { "WS error: ${ex.message}"; };
        }
        wsLogger.debug { "try reconnect" }
        attemptCounter.increment()
        val adminUrl = adminAddress.toString()
        connect(adminUrl)
    } + SupervisorJob()

    override val coroutineContext: CoroutineContext = dispatcher + fallback

    init {
        launch { topicRegister() }
    }

    fun connect(adminUrl: String) = launch(fallback) {
        supervisorScope {
            delay(((DELAY + attemptCounter.value * 1000)))
            val url = "$adminUrl/agent/attach"
            wsLogger.debug { "try to create websocket $url" }
            process(url)
        }
    }

    private suspend fun process(url: String) {
        val wsClient = RWebsocketClient(
            url = url,
            protocols = emptyList(),
            origin = "",
            wskey = "",
            params = mutableMapOf(
                AgentConfigParam to ProtoBuf.dumps(AgentConfig.serializer(), agentConfig),
                NeedSyncParam to agentConfig.needSync.toString(),
                HttpHeaders.ContentEncoding to "deflate"
            )
        )
        wsClient.onOpen += {
            wsLogger.info { "Agent connected" }
        }

        onBinaryMessage?.let { wsClient.onBinaryMessage.add(it) }
        onStringMessage?.let { wsClient.onStringMessage.add(it) }
        onAnyMessage?.let { wsClient.onAnyMessage.add(it) }
        wsClient.onAnyMessage.add {
            Sender.send(Message(MessageType.DEBUG, ""))
        }

        wsClient.onBinaryMessage.add { rawMessage ->
            val message = rawMessage.toWsMessage()
            val destination = message.destination
            val topic = WsRouter[destination]
            if (topic != null) {
                when (topic) {
                    is PluginTopic -> {
                        val pluginMetadata = ProtoBuf.load(PluginBinary.serializer(), message.data)
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
            } else {
                wsLogger.warn { "topic with name '$destination' didn't register" }
            }

        }

        wsClient.onError.add {
            when (it) {
                is WsException -> wsLogger.error { "WS error: ${it.message}" }
                else -> wsLogger.error(it) { "WS error: ${it.message}" }
            }

        }
        wsClient.onClose.add {
            wsLogger.info { "Websocket closed" }
            wsClient.close()
            throw WsClosedException("")
        }
        attemptCounter.value = 0.freeze()
        try {
            while (true) {
                readMessage()?.let { wsClient.blockingSend(it) }
                delay(10)
            }
        } catch (ex: Throwable) {
            wsClient.onError.forEach { it(ex) }
        }
    }

    fun close() {
        try {
            coroutineContext.cancelChildren()
        } catch (_: Exception) {
        }

    }
}

private fun ByteArray.toWsMessage() = ProtoBuf.load(Message.serializer(), this)
