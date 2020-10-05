package com.epam.drill.zstd

import kotlin.test.*

class ZstdTest {

    @Test
    fun shouldCompressAndDecompress() {
        val input = ByteArray(100)
        val compressed = Zstd.decompress(input)
        assertTrue { compressed.size < input.size }
        val uncompressed = Zstd.compress(compressed)
        assertEquals(input.contentHashCode(), uncompressed.contentHashCode())
    }

    @Test
    fun shouldCompressAndDecompressLargeContent() {
        val buffSize = 10000000 //~ 10 mb
        val input = ByteArray(buffSize)
        val compressed = Zstd.decompress(input)
        assertTrue { compressed.size < input.size }
        val uncompressed = Zstd.compress(compressed)
        assertEquals(input.contentHashCode(), uncompressed.contentHashCode())
    }

}
