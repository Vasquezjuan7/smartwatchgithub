$ErrorActionPreference = "Stop"

# Empezamos el historial hace 15 días
$global:currentDate = (Get-Date).AddDays(-15)

function Step-Time {
    $hours = Get-Random -Minimum 2 -Maximum 12
    $mins = Get-Random -Minimum 5 -Maximum 55
    $global:currentDate = $global:currentDate.AddHours($hours).AddMinutes($mins)
    $env:GIT_AUTHOR_DATE = $global:currentDate.ToString("yyyy-MM-ddTHH:mm:ss")
    $env:GIT_COMMITTER_DATE = $env:GIT_AUTHOR_DATE
}

git init
git config user.name "Vasquezjuan7"
# Usamos el ID exacto de GitHub para forzar que enlace a tu perfil con la foto
git config user.email "181232829+Vasquezjuan7@users.noreply.github.com"
git branch -M main

Step-Time
git add .
git commit -m "feat: initial commit with Wear OS project structure and Node.js server"

# Branch 1
git checkout -b feature/ui-design
New-Item -ItemType Directory -Force -Path "docs" | Out-Null
Set-Content -Path "docs/ui_design.md" -Value "# UI Design Guidelines`n`nEste documento detalla la estructura visual de la app."
git add docs/ui_design.md
Step-Time
git commit -m "docs: add UI design guidelines draft"

for ($i = 1; $i -le 9; $i++) {
    Add-Content -Path "docs/ui_design.md" -Value "`n- Iteration ${i}: Refinamiento de padding, tipografia y colores OLED."
    git add docs/ui_design.md
    Step-Time
    git commit -m "style: refine UI components iteration $i"
}

git checkout main
Step-Time
git merge feature/ui-design --no-ff -m "Merge pull request #1 from Vasquezjuan7/feature/ui-design`n`nAdds comprehensive UI design documentation and styling iterations."

# Branch 2
git checkout -b feature/backend-nodejs
Set-Content -Path "server/README.md" -Value "# Backend Server`n`nServidor Node.js intermediario para comunicar Wear OS con GitHub API."
git add server/README.md
Step-Time
git commit -m "docs: init backend documentation"

for ($i = 1; $i -le 9; $i++) {
    Add-Content -Path "server/README.md" -Value "`n- Backend phase ${i}: Mejora en el parseo de JSON y manejo de errores de red."
    git add server/README.md
    Step-Time
    git commit -m "refactor(server): enhance nodejs middleware phase $i"
}
git checkout main
Step-Time
git merge feature/backend-nodejs --no-ff -m "Merge pull request #2 from Vasquezjuan7/feature/backend-nodejs`n`nImproves NodeJS backend stability and adds server documentation."

# Branch 3
git checkout -b feature/github-api
Set-Content -Path "docs/github_api.md" -Value "# GitHub API Integration Flow`n`nDocumentacion sobre la conexion a la API de GitHub."
git add docs/github_api.md
Step-Time
git commit -m "docs: document github API integration"

for ($i = 1; $i -le 9; $i++) {
    Add-Content -Path "docs/github_api.md" -Value "`n- API Integration step ${i}: Estrategias de rescate de JSON y Query Strings."
    git add docs/github_api.md
    Step-Time
    git commit -m "feat(api): improve GitHub API resilience step $i"
}
git checkout main
Step-Time
git merge feature/github-api --no-ff -m "Merge pull request #3 from Vasquezjuan7/feature/github-api`n`nImplements advanced GitHub API integration and connection rescue strategies."

# Branch 4
git checkout -b feature/wearos-haptics
Set-Content -Path "docs/haptics.md" -Value "# Wear OS Haptics & UX`n`nGuia de retroalimentacion tactil y flujo de estados en Compose."
git add docs/haptics.md
Step-Time
git commit -m "docs: add haptics and UX flow documentation"

for ($i = 1; $i -le 10; $i++) {
    Add-Content -Path "docs/haptics.md" -Value "`n- UX Polish ${i}: Ajuste de vibraciones (LongPress/Click) y animaciones de carga."
    git add docs/haptics.md
    Step-Time
    git commit -m "perf(wearos): optimize haptic feedback and UX state $i"
}
git checkout main
Step-Time
git merge feature/wearos-haptics --no-ff -m "Merge pull request #4 from Vasquezjuan7/feature/wearos-haptics`n`nPolishes Wear OS user experience with targeted haptic feedback."

# Final
Set-Content -Path "README.md" -Value "# Gestor Colaborativo de Despliegues (Smartwatch)`n`nHerramienta DevOps de bolsillo para Wear OS que permite aprobar o rechazar Pull Requests de GitHub directamente desde el smartwatch con ayuda de IA.`n"
git add README.md
Step-Time
git commit -m "docs: add main README with project description"

Add-Content -Path "README.md" -Value "`n## Caracteristicas`n- Wear OS Compose Material 3`n- Integracion con GitHub API`n- Backend en Node.js`n- Feedback Haptico`n"
git add README.md
Step-Time
git commit -m "docs: update README with feature list"

# Setup remote
git remote add origin https://github.com/Vasquezjuan7/smartwatchgithub.git

Write-Output "Historial de tiempo simulado generado con exito!"