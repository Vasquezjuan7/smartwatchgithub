package com.example.githubmovil

import com.example.githubmovil.data.AiSummary
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class AiSummaryTest {
    @Test
    fun serializationRoundTripMatches() {
        val original = AiSummary(
            resumen = "Actualiza dependencias criticas",
            cambios = listOf("Sube AGP a 8.5", "Actualiza Compose"),
            riesgo = "bajo",
            motivoRiesgo = "Cambios puramente de build sin impacto funcional"
        )
        val json = original.toJson()
        val parsed = AiSummary.fromJson(json)

        assertEquals(original.resumen, parsed.resumen)
        assertEquals(original.cambios.size, parsed.cambios.size)
        assertEquals(original.riesgo, parsed.riesgo)
        assertEquals(original.motivoRiesgo, parsed.motivoRiesgo)
    }

    @Test
    fun fallbackRiskIsMedium() {
        val json = JSONObject("""{"resumen":"Test","cambios":[]}""")
        val parsed = AiSummary.fromJson(json)
        assertEquals("medio", parsed.riesgo)
    }
}
