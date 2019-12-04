package com.epam.drill

import kotlinx.cinterop.*
import platform.posix.*

val tempPath = ""

val processInfoCmd = "wmic process where \"processid='${getpid()}'\" get commandline"

fun AutofreeScope.openPipe(): CPointer<FILE>? {
    val cmdByteVar = processInfoCmd.cstr.getPointer(this)
    val mode = "r".cstr.getPointer(this)
    return popen?.invoke(cmdByteVar, mode)
}

fun CPointer<FILE>?.close() = pclose?.invoke(this)

fun doMkdir(path: String) {
    mkdir(path)
}