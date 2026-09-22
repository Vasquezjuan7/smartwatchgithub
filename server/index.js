const express = require('express');
const axios = require('axios');

const app = express();
// Solución Definitiva: Forzamos a Express a parsear CUALQUIER petición como JSON,
// Solución para que Node.js entienda TODOS los formatos posibles (JSON, texto plano y bytes)
app.use(express.json());
app.use(express.text());
app.use(express.urlencoded({ extended: true }));

const PORT = 3000;

// Tu Token de GitHub (NUNCA SUBAS EL TOKEN REAL AL REPOSITORIO DE GITHUB)
// Para que esto funcione, crea un archivo .env o simplemente pégalo aquí temporalmente.
const GITHUB_TOKEN = 'ghp_PegaAquiTuTokenParaProbar_PeroNoLoSubas';
const REPO_OWNER = 'Vasquezjuan7';
const REPO_NAME = 'smartwatchgithub';

app.post('/api/decision', async (req, res) => {
    console.log(`\n-----------------------------------------`);
    console.log(`[Cerebro] Petición recibida desde el reloj.`);
    console.log(`Cuerpo original:`, req.body);

    let pr_number;
    let action = 'merge';

    // Si viene información válida, la usamos
    if (req.body && req.body.pr_number) pr_number = req.body.pr_number;
    if (req.body && req.body.action) action = req.body.action;

    // Si viene texto extraño y vacío, pedimos que se arregle desde Android o intentamos adivinar
    if (!pr_number) {
         console.log("⚠️ AVISO: El emulador no envió el PR number. Intentando leer desde el Query String o simulando 1...");
         if (req.query && req.query.pr_number) {
             pr_number = req.query.pr_number;
             console.log(`[Rescate] Encontrado PR #${pr_number} en la URL!`);
         } else {
             pr_number = 1;
         }
    }

    console.log(`[Reloj -> Servidor] Decisión: ${action.toUpperCase()} para el PR #${pr_number}`);

    try {
        if (action === 'merge') {
            console.log('🔄 Contactando a GitHub para hacer MERGE...');
            const githubUrl = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/pulls/${pr_number}/merge`;

            const response = await axios.put(githubUrl,
                { commit_title: "Aprobado desde el Smartwatch" },
                {
                    headers: {
                        'Authorization': `token ${GITHUB_TOKEN}`,
                        'Accept': 'application/vnd.github.v3+json'
                    }
                }
            );

            console.log('✅ MERGE Exitoso en GitHub!');
            return res.status(200).json({ status: "success" });

        } else if (action === 'close') {
            console.log('🚫 Contactando a GitHub para CERRAR el PR...');
            const githubUrl = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/pulls/${pr_number}`;

            const response = await axios.patch(githubUrl,
                { state: "closed" },
                {
                    headers: {
                        'Authorization': `token ${GITHUB_TOKEN}`,
                        'Accept': 'application/vnd.github.v3+json'
                    }
                }
            );

            console.log('✅ PR Cerrado Exitosamente en GitHub!');
            return res.status(200).json({ status: "success" });
        }
    } catch (error) {
        console.error('❌ Error de GitHub:', error.response ? error.response.data : error.message);
        return res.status(500).json({ error: "Fallo al ejecutar la acción en GitHub" });
    }
});

app.listen(PORT, () => {
    console.log(`🚀 Cerebro (Servidor Node.js) encendido y escuchando en el puerto ${PORT}`);
    console.log(`👉 Esperando conexión desde la app del Reloj...`);
});