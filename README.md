# Gestor Colaborativo de Despliegues (Smartwatch)

Herramienta DevOps de bolsillo para Wear OS que permite aprobar (merge) o rechazar (close) Pull Requests de GitHub directamente desde el smartwatch.

## Descargar

[![Descargar APK](https://img.shields.io/github/v/release/Vasquezjuan7/smartwatchgithub?label=Descargar%20APK&logo=wearos&color=4285F4)](https://github.com/Vasquezjuan7/smartwatchgithub/releases/latest)

Descarga `PRWatch-vX.Y.Z.apk` desde **[Releases → ultima version](https://github.com/Vasquezjuan7/smartwatchgithub/releases/latest)**.
Es una app **Wear OS** (Wear OS 3 / API 30 o superior, p. ej. Galaxy Watch4 en adelante): se instala en el reloj, no en el celular.

**Instalar desde el PC con ADB por Wi-Fi**
1. En el reloj: *Ajustes → Informacion del reloj → Software* y toca 7 veces **Version de software** para activar las opciones de desarrollador.
2. En *Opciones de desarrollador* activa **Depuracion ADB** y **Depurar por Wi-Fi**; anota la IP y el puerto que muestra.
3. En el PC (con [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools)):
   ```bash
   adb pair IP:PUERTO_DE_EMPAREJAMIENTO   # solo la primera vez, con el codigo que muestra el reloj
   adb connect IP:PUERTO
   adb install PRWatch-v1.0.0.apk
   ```

> El APK esta firmado con una clave de depuracion. Si tienes otra compilacion instalada, desinstalala antes.

## Caracteristicas
- Wear OS Compose Material 3
- Inicio de sesion con GitHub (OAuth Device Flow), sin tokens en el codigo
- Lista los PRs abiertos de **todos** tus repos con permiso de escritura (propios, colaborador y organizaciones)
- Merge / Close directo contra la API de GitHub: **no necesita servidor**
- Feedback haptico
- **Avisos en segundo plano**: cada 15 min el reloj revisa GitHub (aunque la app este cerrada) y vibra con una notificacion por cada PR nuevo; al tocarla se abre ese PR directamente
- Logo propio (icono adaptativo vectorial): grafo git dentro de un anillo tipo bisel de reloj
- **Modo demo** (boton "Ver demo"): datos de ejemplo y merge simulado, ideal para presentar sin internet

## Diseno de interfaz
Estilo "Aurora": fondo OLED negro con auroras de color animadas y tarjetas de cristal (glassmorphism).

| Elemento | Animacion |
|---|---|
| Fondo | Tres auroras orbitan lentamente; el color cambia segun la pantalla (verde al fusionar, rojo al cerrar) |
| Navegacion | Transiciones con deslizamiento + escala + fundido, con direccion segun avanzas o retrocedes |
| Login | Logo propio (grafo git) flotando con ondas tipo radar; boton con degradado y destello |
| Codigo de acceso | Cada caracter aparece con "pop", borde degradado giratorio y cuenta atras en el bisel del reloj |
| Lista de PRs | Contador animado, tarjetas que entran escalonadas, color unico por repositorio, rebote al pulsar |
| Detalle | Puntos de luz que fluyen de la rama origen a la destino |
| Confirmar | **Manten pulsado**: un anillo se llena para evitar merges accidentales (con vibracion) |
| Resultado | Check que se dibuja solo con explosion de particulas; error con sacudida |

Codigo de la UI: `presentation/components/` (efectos y controles reutilizables) y `presentation/screens/`.

## Configuracion (una sola vez)

1. Crea una OAuth App en GitHub: <https://github.com/settings/applications/new>
   - *Application name*: `Gestor PRs Smartwatch`
   - *Homepage URL* y *Authorization callback URL*: cualquiera, p. ej. `https://github.com`
2. En la pagina de la app marca **Enable Device Flow** y guarda.
3. Copia el **Client ID** (no hace falta el Client Secret) y anadelo a `local.properties`:
   ```properties
   github.clientId=Ov23liXXXXXXXXXXXXXX
   ```
4. Compila e instala la app en el reloj.

## Uso
1. Abre la app y pulsa **Conectar GitHub**.
2. En el movil o PC entra a `github.com/login/device` y escribe el codigo que muestra el reloj.
3. Listo: el reloj guarda la sesion y lista tus PRs abiertos. Toca uno, revisa y pulsa ✓ (merge) o ✗ (close).

> Si un repo de una organizacion no aparece, puede que la org restrinja apps OAuth:
> en la pantalla de autorizacion de GitHub pulsa **Grant/Request** junto a esa organizacion.

La carpeta `server/` (antiguo "cerebro" en Node.js) ya no es necesaria.
