# GitHub OAuth 2.0 Device Authorization Grant (RFC 8628)

Este modulo permite iniciar sesion en smartwatches sin necesidad de teclado:
1. El reloj solicita un \device_code\ y \user_code\ a GitHub.
2. El usuario entra a \github.com/login/device\ en su movil o PC y digita el codigo.
3. El reloj consulta periodicamente \/login/oauth/access_token\ hasta recibir el token.
4. El token se almacena localmente de forma segura en \TokenStore\.
