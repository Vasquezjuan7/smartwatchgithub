package com.example.githubmovil.data

import java.time.Instant
import java.time.temporal.ChronoUnit

/** PRs de ejemplo para el modo demo (presentaciones sin internet ni cuenta de GitHub). */
object DemoData {
    const val USER = "demo"

    fun pullRequests(): List<PullRequest> {
        val now = Instant.now()
        fun ago(minutes: Long) = now.minus(minutes, ChronoUnit.MINUTES).toString()
        return listOf(
            PullRequest(
                "demo/tienda-online", 42, "Nueva pasarela de pago con Stripe", "maria-dev",
                "feature/stripe", "main",
                "Integra Stripe Checkout, añade webhooks de confirmación y tests de integración.",
                ago(7), draft = false
            ),
            PullRequest(
                "demo/app-movil", 18, "Modo oscuro en toda la app", "carlos",
                "feat/dark-mode", "develop",
                "Se añade tema oscuro con soporte para colores dinámicos de Android 12+.",
                ago(95), draft = false
            ),
            PullRequest(
                "acme/api-backend", 7, "Arreglar fuga de memoria en el caché", "lucia",
                "fix/cache-leak", "main",
                "El caché LRU no liberaba entradas expiradas. Se añade limpieza periódica.",
                ago(60 * 5), draft = true
            ),
            PullRequest(
                "acme/landing", 3, "Actualizar dependencias de seguridad", "dependabot",
                "deps/security", "main",
                "Actualiza 4 dependencias con vulnerabilidades conocidas.",
                ago(60 * 26), draft = false
            ),
        )
    }

    /** Resúmenes de IA de ejemplo para el modo demo. */
    fun summaryFor(pr: PullRequest): AiSummary = when (pr.number) {
        42 -> AiSummary(
            "Sustituye el pago manual por Stripe Checkout y confirma los pedidos mediante webhooks.",
            listOf("Nuevo flujo de pago con Stripe", "Webhook que marca pedidos como pagados", "Tests de integración del checkout"),
            "alto",
            "Toca el cobro a clientes y requiere claves de producción"
        )
        18 -> AiSummary(
            "Añade un tema oscuro a toda la app y adapta los colores al fondo de pantalla en Android 12+.",
            listOf("Paleta de colores oscura", "Colores dinámicos de Material You", "Ajustes de contraste en textos"),
            "bajo",
            "Solo cambia estilos visuales, sin lógica"
        )
        7 -> AiSummary(
            "Corrige una fuga de memoria: el caché ya no acumula entradas caducadas y las limpia cada cierto tiempo.",
            listOf("Limpieza periódica del caché LRU", "Límite de tamaño configurable", "Métrica de entradas expulsadas"),
            "medio",
            "Cambia el comportamiento del caché en producción"
        )
        else -> AiSummary(
            "Actualiza dependencias con vulnerabilidades conocidas a sus versiones seguras.",
            listOf("4 librerías actualizadas", "Sin cambios en el código de la app"),
            "bajo",
            "Versiones menores, compatibles hacia atrás"
        )
    }
}
