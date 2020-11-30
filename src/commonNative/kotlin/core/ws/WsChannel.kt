package com.epam.drill.core.ws

import kotlinx.coroutines.channels.*
import kotlin.native.concurrent.*

private val messageBus = AtomicReference(Channel<ByteArray>(Channel.UNLIMITED).freeze())

fun sendMessage(message: ByteArray) {
    messageBus.value.offer(message.freeze())
}

fun readMessage(): ByteArray? {
   return messageBus.value.poll()
}

