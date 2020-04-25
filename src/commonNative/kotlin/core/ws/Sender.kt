package com.epam.drill.core.ws

import com.epam.drill.common.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*
import kotlin.native.internal.*

@SharedImmutable
private val dispatcher = newSingleThreadContext("sender coroutine")

object Sender : CoroutineScope {

    operator fun invoke(block: suspend () -> Unit) = launch {
        block()
        GC.collect()
    }

    inline fun <reified T : Any> send(message: T) = launch {
        msChannel.send(T::class.serializer().stringify(message))
        GC.collect()
    }

    override val coroutineContext: CoroutineContext = dispatcher

}