package com.epam.drill.core.messanger

import com.epam.drill.common.*
import com.epam.drill.core.ws.*
import com.epam.drill.plugin.api.message.*
import kotlinx.cinterop.*

fun sendNativeMessage(pluginId: CPointer<ByteVar>, content: CPointer<ByteVar>) {
    sendMessage(pluginId.toKString(), content.toKString())
}

fun sendMessage(pluginId: String, content: String) {
    Sender.send(
        Message(
            type = MessageType.PLUGIN_DATA,
            destination = "",
            data = (MessageWrapper.serializer() stringify MessageWrapper(
                pluginId,
                DrillMessage(content = content)
            )).encodeToByteArray()
        )
    )
}
