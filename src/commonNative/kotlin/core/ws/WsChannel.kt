package com.epam.drill.core.ws

import com.epam.drill.core.concurrency.LockFreeMPSCQueue
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.native.concurrent.*

private val eventBus = AtomicReference(Channel<Unit>().freeze())
private val messageQueue = AtomicReference(
    LockFreeMPSCQueue<ByteArray>().freeze())

fun CoroutineScope.addMessageToQueue(message: ByteArray) {
    messageQueue.value.addLast(message.freeze())
    launch {
        eventBus.value.send(Unit)
    }
}

suspend fun readMessage(): ByteArray? {
    eventBus.value.receive() // wait for message
    return messageQueue.value.removeFirstOrNull()
}

