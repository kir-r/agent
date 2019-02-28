package com.epam.drill.core.ws

import com.epam.drill.JarVfsFile
import com.epam.drill.common.*
import com.epam.drill.extractPluginFacilitiesTo
import com.epam.drill.core.agentGroupName
import com.epam.drill.core.agentName
import com.epam.drill.core.callbacks.vminit.loadPlugin
import com.epam.drill.core.drillAdminUrl
import com.epam.drill.core.drillInstallationDir
import com.epam.drill.logger.DLogger
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.writeToFile
import com.soywiz.korio.lang.Thread_sleep
import com.soywiz.korio.net.ws.WebSocketClient
import jvmapi.*
import kotlinx.cinterop.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker


val wsLogger
    get() = DLogger("DrillWebsocket")


suspend fun register(wsClient: WebSocketClient) = memScoped {
    val agentInfo = AgentInfo(
        agentName, agentGroupName,
        mutableSetOf(),

        //todo remove this hardcode
        AgentAdditionalInfo(
            listOf(),
            4,
            "x64",
            "windows",
            "10",
            mapOf()
        )
    )

    val message = Json.stringify(
        Message.serializer(),
        Message(MessageType.AGENT_REGISTER, message = Json.stringify(AgentInfo.serializer(), agentInfo))
    )

    wsClient.send(message)

}

fun ws() = runBlocking {
    launch {
        val url = "ws://$drillAdminUrl/agent/attach"
        wsLogger.debug { "try to create ws $url" }
        val wsClient = WebSocketClient(url)
        wsLogger.debug { "WS created" }

        wsClient.onStringMessage.add { rawMessage ->
            val message = Json().parse(Message.serializer(), rawMessage)
            val action = Json().parse(AgentEvent.serializer(), message.message)
            when (action.event) {
                DrillEvent.UNLOAD_PLUGIN -> unload(action.data)
                DrillEvent.AGENT_LOAD_SUCCESSFULLY -> wsLogger.info { "connected with DrillConsole" }
                DrillEvent.LOAD_PLUGIN -> wsLogger.info { "'${DrillEvent.LOAD_PLUGIN}' event waiting for a file" }
            }


        }
        wsClient.onBinaryMessage.add {

            load(it)
        }
        wsClient.onError.add {
            wsLogger.error { "WS error: ${it.message}" }
        }
        wsClient.onClose.add {
            wsLogger.info { "Websocket closed" }
            //todo thing about this throwing...
            throw RuntimeException("close")
        }
        register(wsClient)
        launch {
            while (true) {
                delay(5)
                MessageQueue.retrieveMessage()?.apply {
                    wsClient.send(this)
                }

            }
        }
    }.join()
}


private fun unload(pluginName: String) {
    //disable the plugin
//fixme think about physical deletion
//    wsLogger.info { "$pluginName unloaded" }
//    PluginLoader.loadedPluginsHandler[pluginName]?.tryUnload()

}

fun load(it: ByteArray) = runBlocking {
    try {
        val (pluginName, rawFileData) = parseRawPluginFromAdminStorage(it)
        val pluginsDir = localVfs(drillInstallationDir)["drill-plugins"]
        if (!pluginsDir.exists()) {
            pluginsDir.mkdir()
        }
        val vfsFile = pluginsDir[pluginName]
        if (!vfsFile.exists()) {
            vfsFile.mkdir()
        }
        val targetFile: JarVfsFile = vfsFile["agent-part.jar"]
        rawFileData.writeToFile(targetFile)
        try {
            targetFile.extractPluginFacilitiesTo(localVfs(targetFile.parent.absolutePath)) { vf ->
                !vf.baseName.contains("nativePart") &&
                        !vf.baseName.contains("static")
            }
            //init env...
            currentEnvs()

            AddToSystemClassLoaderSearch(targetFile.absolutePath)


            loadPlugin(targetFile)


            wsLogger.info { "load $pluginName" }
        } catch (ex: Exception) {
            wsLogger.error { "cant load the plugin..." }
            ex.printStackTrace()
        }

    } catch (ex: Throwable) {
        ex.printStackTrace()
    }
}

fun parseRawPluginFromAdminStorage(it: ByteArray): Pair<String, ByteArray> {
    val pluginName = it.sliceArray(0 until 20).stringFromUtf8().replace("!", "")
    val rawFileData = it.sliceArray(20 until it.size)
    return Pair(pluginName, rawFileData)
}

fun startWs(): Future<Unit> {
    val worker = Worker.start(true)
    return worker.execute(TransferMode.UNSAFE, {}) {
        while (true) {
            wsLogger.debug { "create new Connection" }
            Thread_sleep(5000)
            try {
                ws()
            } catch (sx: Throwable) {
                wsLogger.error { sx.message }
            }
        }
    }
}

