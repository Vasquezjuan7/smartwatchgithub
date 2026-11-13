# Testing Strategy and Guide

Guia de ejecucion de pruebas unitarias y de integracion:
- **Unit Tests**: Ejecutables directamente en la JVM con \./gradlew test\.
- **Model Tests**: Verifican consistencia en \PullRequest\, \DeviceCode\ y serializacion JSON de \AiSummary\.
- **Demo Data Integrity**: Aseguran que el dataset mock offline no contenga punteros nulos ni campos vacios.
