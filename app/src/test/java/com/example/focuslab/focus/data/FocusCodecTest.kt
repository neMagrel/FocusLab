package com.example.focuslab.focus.data

import com.example.focuslab.focus.model.FocusCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusCodecTest {
    @Test
    fun `empty category list round trips`() {
        assertEquals(emptyList<FocusCategory>(), FocusCodec.decode(FocusCodec.encode(emptyList())))
    }

    @Test
    fun `category values and order round trip without delimiter collisions`() {
        val categories = listOf(
            FocusCategory(id = "study.1", emoji = "📚", title = "Учёба\nи практика"),
            FocusCategory(id = "code/2", emoji = "👩🏽‍💻", title = "Kotlin | Android = fun")
        )

        assertEquals(categories, FocusCodec.decode(FocusCodec.encode(categories)))
    }

    @Test
    fun `malformed payload returns null`() {
        assertNull(FocusCodec.decode("not-a-supported-payload"))
        assertNull(FocusCodec.decode("v1\ninvalid.category"))
        assertNull(FocusCodec.decode("v1\n%%%..."))
    }
}
