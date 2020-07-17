package com.epam.drill.zlib

import com.epam.drill.*
import com.epam.drill.core.transport.*
import com.epam.drill.hook.io.tcp.*
import kotlin.test.*


private const val H1 = "header1"
private const val H2 = "header2"

private const val V1 = "value1"
private const val V2 = "value2"

internal class SocketDispatcherTest {

    private val headers = mapOf(
        H1.encodeToByteArray() to V1.encodeToByteArray(),
        H2.encodeToByteArray() to V2.encodeToByteArray()
    )

    @BeforeTest
    fun beforeEach() {
        configureHttp()
    }

    @Test
    fun shouldNotCreateDrillRequest() {
        readHeaders.value(headers)
        assertNull(drillRequest)
    }

    @Test
    fun shouldDetectSessionId() {
        val value = "sessionId"
        readHeaders.value(headers + mapOf("drill-session-id".encodeToByteArray() to value.encodeToByteArray()))
        assertEquals(value, drillRequest?.drillSessionId)
    }

    @Test
    fun shouldApplyHeaderMapping() {
        requestPattern = H1
        readHeaders.value(headers)
        assertEquals(V1, drillRequest?.drillSessionId)
    }

    @Test
    fun shouldOverlapSessionIdByHeaderMapping() {
        val value = "sessionId"
        requestPattern = H1
        readHeaders.value(headers + mapOf("drill-session-id".encodeToByteArray() to value.encodeToByteArray()))
        assertEquals(V1, drillRequest?.drillSessionId)
    }
}