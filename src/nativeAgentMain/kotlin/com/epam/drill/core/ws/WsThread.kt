package com.epam.drill.core.ws

import com.epam.drill.common.*
import com.epam.drill.core.*
import com.epam.drill.core.concurrency.*
import com.epam.drill.core.exceptions.*
import com.epam.drill.core.messanger.*
import com.epam.drill.core.plugin.loader.*
import com.epam.drill.logger.*
import com.soywiz.korio.net.ws.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*
import kotlin.collections.isNotEmpty
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.toByteArray
import kotlin.native.concurrent.*


@SharedImmutable
val wsLogger = DLogger("DrillWebsocket")

@SharedImmutable
val wsThread = Worker.start(true)

@SharedImmutable
val sendWorker = Worker.start(true)

@SharedImmutable
val loader = Worker.start(true)

@ThreadLocal
private val guaranteeQueue = LockFreeMPSCQueue<String>()

fun sendMessage(message: String) {
    sendWorker.execute(TransferMode.UNSAFE, { message }) {
        guaranteeQueue.addLast(it)
    }
}


fun startWs() =
    wsThread.executeCoroutines {
        launch { topicRegister() }
        while (true) {
            delay(3000)
            try {
                runBlocking {
                    websocket(exec { agentConfig.adminUrl })
                }
            } catch (ex: Exception) {
//                when (ex) {
//                    is WsClosedException -> {
//                    }
                println(ex.message + "\ntry reconnect\n")
//                }
            }
        }
    }


suspend fun websocket(adminUrl: String) {
    val url = "ws://$adminUrl/agent/attach"
    wsLogger.debug { "try to create websocket $url" }
    val wsClient = WebSocketClient(
        url, params = mutableMapOf(
            AgentConfigParam to Cbor.dumps(AgentConfig.serializer(), exec { agentConfig }),
            NeedSyncParam to exec { agentConfig.needSync }.toString()
        )
    )
    wsClient.onOpen {
        wsLogger.debug { "Agent connected" }
    }

    wsClient.onAnyMessage.add {
        sendMessage(Message.serializer() stringify Message(MessageType.DEBUG, "", ""))
    }

    wsClient.onStringMessage.add { rawMessage ->
        val message = rawMessage.toWsMessage()
        if (message.type == MessageType.INFO) {
            when (message.message) {
                DrillEvent.SYNC_FINISHED.name -> {
                    exec { agentConfig.needSync = false }
                    wsLogger.info { "Agent synchronization is finished" }
                }
                DrillEvent.SYNC_STARTED.name -> {
                    wsLogger.info { "Agent synchronization is started" }
                }
            }
        } else {
            val destination = message.destination
            val topic = WsRouter[destination]
            if (topic != null) {
                when (topic) {
                    is FileTopic -> throw RuntimeException("We can't use File topic in not binary retriever")
                    is InfoTopic -> topic.block(message.message)
                    is GenericTopic<*> -> topic.deserializeAndRun(message.message)
                }
            } else {
                wsLogger.warn { "topic with name '$destination' didn't register" }
            }
        }

    }
    wsClient.onBinaryMessage.add {
        val load = Cbor.load(PluginMessage.serializer(), it)
        if (exec { pstorage[load.pl.id] } != null) return@add
        when {
            load.event == DrillEvent.LOAD_PLUGIN -> {
                val pluginId = load.pl.id
                exec { pl[pluginId] = load.pl }
                loader.execute(TransferMode.UNSAFE, { load }) { plugMessage ->
                    println("try to load ${plugMessage.pl.id} plugin")
                    runBlocking {
                        exec { agentConfig.needSync = false }
                        val id = plugMessage.pl.id
                        val ajar = "agent-part.jar"

                        val src = plugMessage.pluginFile.toByteArray()
                        val pluginsDir = "$drillInstallationDir/drill-plugins"
                        com.epam.drill.doMkdir(pluginsDir)
                        val pluginDir = "$pluginsDir/$id"
                        com.epam.drill.doMkdir(pluginDir)
                        val path = "$pluginDir/$ajar"

                        writeFileAsync(path, src)
                        loadPlugin(path, plugMessage.pl)

                        if (plugMessage.nativePart != null) {
                            val natPlugin = when {
                                plugMessage.nativePart!!.windowsPlugin.isNotEmpty() -> {
                                    val nativePath = "$pluginDir/native_plugin.dll"
                                    writeFileAsync(nativePath, plugMessage.nativePart!!.windowsPlugin.toByteArray())
                                    nativePath
                                }
                                plugMessage.nativePart!!.linuxPluginFileBytes.isNotEmpty() -> {
                                    val nativePath = "$pluginDir/native_plugin.so"
                                    writeFileAsync(
                                        nativePath,
                                        plugMessage.nativePart!!.linuxPluginFileBytes.toByteArray()
                                    )
                                    nativePath
                                }
                                else -> {
                                    throw RuntimeException()
                                }
                            }
                            val loadNativePlugin = com.epam.drill.loadNativePlugin(
                                id,
                                natPlugin,
                                staticCFunction(::sendNativeMessage)
                            )
                            loadNativePlugin?.initPlugin()
                            loadNativePlugin?.on()
                        }

                    }
                    println("${plugMessage.pl.id} plugin loaded")
                    //TODO spinner hack
                    sendMessage(Message.serializer() stringify Message(MessageType.MESSAGE, "", "OK"))
                }
            }
        }


    }
    wsClient.onError.add {
        wsLogger.error { "WS error: ${it.message}" }
    }
    wsClient.onClose.add {
        wsLogger.info { "Websocket closed" }
        wsClient.close()
        throw WsClosedException("")
    }

    coroutineScope {
        launch {
            while (true) {
                delay(50)
                val execute = sendWorker.execute(TransferMode.UNSAFE, {}) {
                    val first = guaranteeQueue.removeFirstOrNull()
                    first
                }.result
                if (execute != null) {
                    wsClient.send(execute)
//                    sendWorker.execute(TransferMode.UNSAFE, {}) {
//                        guaranteeQueue.removeFirstOrNull()
//                    }.result
                }
            }

        }
    }
}


private fun String.toWsMessage() = Message.serializer().parse(this)


fun Worker.executeCoroutines(block: suspend CoroutineScope.() -> Unit): Future<Unit> {
    return this.execute(TransferMode.UNSAFE, { block }) {
        try {
            runBlocking {
                it(this)
            }
        } catch (ex: Throwable) {
            println("ss")
        }
    }
}
