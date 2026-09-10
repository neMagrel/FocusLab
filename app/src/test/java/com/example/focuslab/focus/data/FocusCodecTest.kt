package com.example.focuslab.focus.data

import com.example.focuslab.focus.model.ActiveFocusSession
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

    @Test
    fun `active session round trips with absolute timestamps`() {
        val session = ActiveFocusSession(
            id = "session.1",
            categoryId = "code/2",
            durationMinutes = 15,
            startedAtEpochMillis = 1_000L,
            endsAtEpochMillis = 901_000L
        )

        assertEquals(
            session,
            FocusCodec.decodeActiveSession(FocusCodec.encodeActiveSession(session))
        )
    }

    @Test
    fun `malformed active session payload returns null`() {
        assertNull(FocusCodec.decodeActiveSession("not-a-supported-payload"))
        assertNull(FocusCodec.decodeActiveSession("v1.a.b.not-an-int.1.2"))
        assertNull(FocusCodec.decodeActiveSession("v1.%%%.Y29kZQ.5.1.300001"))
    }
}
