package com.gglee.qimendunjia

import com.gglee.qimendunjia.engine.ChartRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Test

/**
 * Regression: in-place ChartRequest.apply() made CastUiState equal after edit,
 * so StateFlow skipped emission and TextFields appeared dead.
 */
class CastViewModelInputTest {

    @Test
    fun chartRequestCopyThenMutateIsDistinct() {
        val original = ChartRequest(question = "")
        val next = original.copy().apply { question = "求财" }
        assertNotSame(original, next)
        assertEquals("", original.question)
        assertEquals("求财", next.question)
        assertFalse(original == next)
    }

    @Test
    fun successiveEditsChangeEquality() {
        var current = ChartRequest(question = "")
        current = current.copy().apply { question = "求" }
        val mid = current
        current = current.copy().apply { question = "求财" }
        assertEquals("求", mid.question)
        assertEquals("求财", current.question)
        assertFalse(mid == current)
    }
}
