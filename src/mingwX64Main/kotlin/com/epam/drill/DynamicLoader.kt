@file:Suppress("unused", "UNUSED_PARAMETER")

package com.epam.drill

import com.epam.drill.plugin.api.processing.*
import kotlinx.cinterop.*
import platform.windows.*


actual fun injectDynamicLibrary(path: String): Any? = memScoped {
    LoadLibrary!!(path.replace("/", "\\").toLPCWSTR(this).pointed.ptr)
        ?.let { hModule -> GetProcAddress(hModule, initPlugin) }
}

private fun String.toLPCWSTR(ms: MemScope): CArrayPointer<UShortVar> {
    val length = this.length
    val allocArray = ms.allocArray<UShortVar>(length.toLong())
    for (i in 0 until length) {
        allocArray[i] = this[i].toShort().toUShort()
    }
    return allocArray
}