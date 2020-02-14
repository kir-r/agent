@file:Suppress("unused", "UNUSED_PARAMETER")

package com.epam.drill

import com.epam.drill.plugin.api.processing.*
import platform.posix.*

actual fun injectDynamicLibrary(path: String): Any? = dlopen(path, RTLD_LAZY)?.let { handle -> dlsym(handle, initPlugin) }
