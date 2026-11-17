# Modelo de Seguridad y Privacidad

- **OAuth Device Flow (RFC 8628)**: Ningun usuario escribe credenciales en texto plano en la app.
- **Client ID Publico**: El Client ID de GitHub OAuth no requiere almacenar Client Secrets en el APK del reloj.
- **Proteccion de Cuota Gemini**: La Edge Function valida contra \ALLOWED_GITHUB_USERS\ para prevenir abusos.
- **Aislamiento Local**: Tokens guardados en \SharedPreferences\ en modo \MODE_PRIVATE\ inaccesibles por otras apps.
