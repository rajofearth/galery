package rs.rajofearth.galery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaDragSelectionTest {
    private val keys = listOf("a", "b", "c", "d", "e")

    @Test
    fun `dragging forward selects the inclusive range and shortening removes added items`() {
        val drag = checkNotNull(MediaDragSelection.start(keys, "b", setOf("d", "outside")))

        assertEquals(setOf("b", "d", "outside"), drag.moveTo("b"))
        assertEquals(setOf("b", "c", "d", "e", "outside"), drag.moveTo("e"))
        assertEquals(setOf("b", "c", "d", "outside"), drag.moveTo("c"))
        assertEquals(setOf("b", "d", "outside"), drag.moveTo("b"))
    }

    @Test
    fun `dragging backward then across the anchor replaces the range`() {
        val drag = checkNotNull(MediaDragSelection.start(keys, "c", setOf("b")))

        assertEquals(setOf("a", "b", "c"), drag.moveTo("a"))
        assertEquals(setOf("b", "c", "d", "e"), drag.moveTo("e"))
    }

    @Test
    fun `an already selected anchor still produces the first selection`() {
        val drag = checkNotNull(MediaDragSelection.start(keys, "b", setOf("b", "c")))

        assertNull(drag.moveTo("missing"))
        assertEquals(setOf("b", "c"), drag.moveTo("b"))
        assertNull(drag.moveTo("b"))
        assertNull(drag.moveTo("c"))
        assertEquals(setOf("b", "c", "d"), drag.moveTo("d"))
    }

    @Test
    fun `an unknown anchor cannot start a drag`() {
        assertNull(MediaDragSelection.start(keys, "missing", emptySet()))
    }

    @Test
    fun `repeated keys keep their first position in the range order`() {
        val drag = checkNotNull(
            MediaDragSelection.start(listOf("a", "b", "c", "a"), "b", emptySet()),
        )

        assertEquals(setOf("a", "b"), drag.moveTo("a"))
    }

    @Test
    fun `the drag keeps its starting order and selection`() {
        val orderedKeys = keys.toMutableList()
        val selection = mutableSetOf("outside")
        val drag = checkNotNull(MediaDragSelection.start(orderedKeys, "b", selection))
        orderedKeys.reverse()
        selection.clear()

        assertEquals(setOf("b", "c", "d", "outside"), drag.moveTo("d"))
    }
}
