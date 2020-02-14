@file:Suppress("unused", "UNUSED_PARAMETER")
package com.epam.drill

import com.epam.drill.plugin.api.processing.*
import kotlinx.cinterop.*
import platform.windows.*


actual fun injectDynamicLibrary(path: String): Any? = memScoped {
    LoadLibrary!!(path.replace("/", "\\").toLPCWSTR(this).pointed.ptr)?.GetProcAddress(hModule, initPlugin)
}

//        val initPlugin =

//        val callbacks: jvmtiEventCallbacks? = gjavaVMGlob?.pointed?.callbackss
//        val reinterpret =
//            initPlugin?.reinterpret<CFunction<(CPointer<ByteVar>, CPointer<com.epam.drill.jvmapi.gen.jvmtiEnvVar>?, CPointer<JavaVMVar>?, CPointer<jvmtiEventCallbacks>?, CPointer<CFunction<(pluginId: CPointer<ByteVar>, message: CPointer<ByteVar>) -> Unit>>) -> COpaquePointer>>()
//        val id = pluginId.cstr.getPointer(this)
//        val jvmti = gdata?.pointed?.jvmti
//        val jvm = gjavaVMGlob?.pointed?.jvm
//        val clb = callbacks?.ptr
//        pluginInstance =
//            reinterpret?.invoke(
//                id,
//                jvmti,
//                jvm,
//                clb,
//                sender
//            )?.asStableRef<NativePart<*>>()?.get()
//    }
//    pluginInstance
//}

private fun String.toLPCWSTR(ms: MemScope): CArrayPointer<UShortVar> {
    val length = this.length
    val allocArray = ms.allocArray<UShortVar>(length.toLong())
    for (i in 0 until length) {
        allocArray[i] = this[i].toShort().toUShort()
    }
    return allocArray
}