# Arquitectura del Sistema: PR Watch (Wear OS)

Diagrama de flujo e interaccion de componentes:

\\\mermaid
flowchart TD
    User["Usuario en Smartwatch"] -->|Interactua| UI["Jetpack Compose Wear OS (Aurora Theme)"]
    UI --> Controller["AppController (State Machine)"]
    Controller -->|Token y Cache| Store["TokenStore & SummaryCache"]
    Controller -->|Peticiones Directas| GHClient["GitHubClient (OkHttp + Coroutines)"]
    Controller -->|Analisis de Diff| AIClient["AiClient (OkHttp)"]
    
    GHClient -->|OAuth Device Flow & REST API| GitHub["GitHub REST API (v3)"]
    AIClient -->|POST /pr-summary| Supabase["Supabase Edge Functions (Deno)"]
    Supabase -->|Diff Analysis & JSON Schema| Gemini["Google Gemini (Flash-Lite)"]
    
    WorkManager["Android WorkManager (15 min)"] -->|Periodic Check| GHClient
    WorkManager -->|Notifica Nuevos PRs| Notifier["PrNotifier (Vibracion Haptica)"]
    Notifier --> User
\\\

## Decisiones Clave de Ingenieria
1. **Eliminacion del Servidor Intermedio Node.js**: Ahora el smartwatch es un cliente completamente autonomo.
2. **Serverless AI Gateway**: La clave de Google Gemini nunca viaja al reloj; reside como secreto en Supabase.
3. **Optimizacion de Bateria**: WorkManager con politicas de red estrictas y frecuencia adaptativa.
