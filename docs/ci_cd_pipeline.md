# CI/CD Pipeline Architecture

Estrategia de integracion y despliegue continuo con GitHub Actions:
1. **Compilacion y Validacion de Android**: Compila el APK de Wear OS en runners Ubuntu con JDK 17.
2. **Ejecucion de Pruebas Unitarias**: Corre los tests de JVM antes de permitir el merge.
3. **Analisis Estatico de Deno / Supabase**: Valida que la funcion TypeScript este libre de errores de sintaxis y tipos.
