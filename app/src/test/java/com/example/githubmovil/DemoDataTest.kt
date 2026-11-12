package com.example.githubmovil

import com.example.githubmovil.data.DemoData
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataTest {
    @Test
    fun demoPrsAreNotEmptyAndHaveValidSummaries() {
        val prs = DemoData.pullRequests()
        assertTrue("Debe contener al menos 2 PRs de demo", prs.size >= 2)
        for (pr in prs) {
            val summary = DemoData.summaryFor(pr)
            assertNotNull("Cada PR de demo debe tener un resumen precomputado", summary)
            assertTrue("El resumen no debe estar vacio", summary.resumen.isNotBlank())
        }
    }
}
