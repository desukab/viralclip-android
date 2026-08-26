package com.viralclip.app.core.ai

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ViralityScorerTest {

    private lateinit var scorer: ViralityScorer

    @Before
    fun setup() {
        val context = mockk<Context>(relaxed = true)
        scorer = ViralityScorer(context)
    }

    @Test
    fun `progress starts at zero`() {
        assertEquals(0f, scorer.progress.value, 0.01f)
    }
}
