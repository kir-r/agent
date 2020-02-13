package com.epam.drill.core

import com.epam.drill.common.*
import kotlin.native.concurrent.*

private val drillSessionIdCallback_ = AtomicReference<() -> String?>({ null }.freeze()).freeze()
private val sessionStorageCallback = AtomicReference({ _: String -> Unit }.freeze()).freeze()
private val loadPluginCallback = AtomicReference({ _: String, _: PluginMetadata -> Unit }.freeze()).freeze()
private val getClassesByConfigCallback = AtomicReference({ listOf<String>() }.freeze()).freeze()
private val setPackagesPrefixesCallback = AtomicReference({ _: String -> Unit }.freeze()).freeze()

var drillSessionId: () -> String?
    get() = drillSessionIdCallback_.value
    set(value) {
        drillSessionIdCallback_.value = value.freeze()
    }

var sessionStorage: (String) -> Unit
    get() = sessionStorageCallback.value
    set(value) {
        sessionStorageCallback.value = value.freeze()
    }

var loadPlugin: (String, PluginMetadata) -> Unit
    get() = loadPluginCallback.value
    set(value) {
        loadPluginCallback.value = value.freeze()
    }


var getClassesByConfig: () -> List<String>
    get() = getClassesByConfigCallback.value
    set(value) {
        getClassesByConfigCallback.value = value.freeze()
    }
var setPackagesPrefixes: (String) -> Unit
    get() = setPackagesPrefixesCallback.value
    set(value) {
        setPackagesPrefixesCallback.value = value.freeze()
    }