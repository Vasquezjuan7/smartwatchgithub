# Background Polling with WorkManager

Estrategia de revision en segundo plano para smartwatches Wear OS:
- **PeriodicWorkRequest**: Programado cada 15 minutos (el intervalo optimo para conservar la bateria del reloj).
- **Network Constraints**: Solo se ejecuta si el reloj cuenta con conexion activa (Wi-Fi, Bluetooth o LTE).
- **Deteccion de Novedades**: Compara contra \seenPrs\ almacenado en \TokenStore\.
- **Notificacion Haptica**: Patron de vibracion distintivo \[0, 120, 80, 120]\ que alerta al desarrollador en su muñeca.
- **Deep Link Directo**: Al tocar la notificacion, se abre la app navegando de inmediato a ese Pull Request especifico.
