package com.veyline.app.data.network.adapter

import com.squareup.moshi.Moshi
import com.veyline.app.data.network.model.NoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NoDataJsonAdapterTest {

    private val moshi = Moshi.Builder()
        .add(NoDataJsonAdapter())
        .build()

    private val adapter = moshi.adapter(NoData::class.java)

    @Test
    fun `deserialize null returns NoData`() {
        val result = adapter.fromJson("null")

        assertSame(NoData, result)
    }

    @Test
    fun `deserialize empty string returns NoData`() {
        val result = adapter.fromJson("\"\"")

        assertSame(NoData, result)
    }

    @Test
    fun `deserialize empty object returns NoData`() {
        val result = adapter.fromJson("{}")

        assertSame(NoData, result)
    }

    @Test
    fun `deserialize nested array returns NoData`() {
        val result = adapter.fromJson(
            """[null, "", 1, true, {"key": "value"}]"""
        )

        assertSame(NoData, result)
    }

    @Test
    fun `serialize NoData returns null`() {
        val result = adapter.toJson(NoData)

        assertEquals("null", result)
    }
}
