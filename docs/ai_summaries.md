# Supabase & Google Gemini AI Summaries

El reloj utiliza una Edge Function en Supabase como proxy seguro para consultar a Google Gemini:

## Flujo de Seguridad
1. El reloj envia \{ repo: "owner/repo", number: 123 }\ junto con su token de GitHub en la cabecera \x-github-token\.
2. Supabase verifica que el usuario autenticado este en la lista blanca \ALLOWED_GITHUB_USERS\ (protegiendo tu cuota de Gemini).
3. Supabase descarga el diff del PR con dicho token (compatible con repositorios privados).
4. Se envia a Gemini con el system prompt de revisor senior exigiendo salida JSON estructurada:
   - \esumen\: 1 o 2 frases concisas (max 220 caracteres).
   - \cambios\: 2 a 4 puntos clave (max 70 caracteres cada uno).
   - \iesgo\: "bajo", "medio" o "alto" segun el impacto en produccion.
   - \motivo_riesgo\: justificacion del nivel de riesgo.
5. El reloj almacena en cache local el resultado con \SummaryCache\ para no reconsultar si el PR no ha cambiado.
