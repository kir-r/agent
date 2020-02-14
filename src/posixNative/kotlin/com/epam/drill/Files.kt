package com.epam.drill

import platform.posix.*
import kotlinx.cinterop.*

actual val tempPath = "/tmp"

val processInfoCmd = "ps -p ${getpid()} -o args"

fun AutofreeScope.openPipe(): CPointer<FILE>? = popen(processInfoCmd, "r")

fun CPointer<FILE>?.close() = pclose(this)

actual fun doMkdir(path: String) {
    mkdir(path, "0777".toInt(8).convert())
}
