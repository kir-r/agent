package com.epam.drill.core.ws

import kotlinx.coroutines.channels.*
import kotlin.native.concurrent.*

private val messageChannel = AtomicReference(Channel<String>().freeze())

val msChannel: Channel<String>
    get() = messageChannel.value
