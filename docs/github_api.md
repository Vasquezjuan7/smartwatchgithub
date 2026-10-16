# GitHub REST API Integration

El cliente nativo de Android interactua directamente con la API REST v3 de GitHub:
- \GET /user\: Obtiene el usuario autenticado.
- \GET /user/repos\: Lista repositorios ordenados por actividad reciente (\ffiliation=owner,collaborator,organization_member\).
- \GET /repos/{owner}/{repo}/pulls\: Obtiene los Pull Requests abiertos.
- \PUT /repos/{owner}/{repo}/pulls/{number}/merge\: Fusiona un PR con commit firmado desde el reloj.
- \PATCH /repos/{owner}/{repo}/pulls/{number}\: Cierra un PR rechazado.

### Optimizaciones de Red
- Control de concurrencia mediante \Semaphore(6)\ para no saturar la red ni la tasa de peticiones de GitHub.
- Omision de repositorios sin \open_issues_count\ para reducir peticiones innecesarias.
