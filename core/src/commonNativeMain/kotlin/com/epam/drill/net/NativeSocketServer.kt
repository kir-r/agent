package com.epam.drill.net

import kotlinx.io.internal.utils.*

class NativeSocketServer(val sockfds: KX_SOCKET) : NativeSocket(sockfds) {

    private var isRunning = true
    override fun isAlive() = isRunning
    override fun setIsAlive(isAlive: Boolean) {
        isRunning = isAlive
    }
}