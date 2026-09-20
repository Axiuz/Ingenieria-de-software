# Capturas de la entrega

Ya está `despliegue-apk-en-emulador.png`: el APK del release `build-4`, instalado en el emulador
y corriendo contra la imagen de GHCR levantada con `docker compose`.

Faltan tres capturas que solo se pueden tomar desde una sesión abierta:

| Archivo | Qué capturar | Dónde |
|---|---|---|
| `pipeline-exitoso.png` | La ejecución del workflow con los cuatro jobs en verde | GitHub → Actions → CI/CD |
| `despliegue.png` | El Release con el APK publicado y la imagen en GHCR | GitHub → Releases y Packages |
| `sonar-dashboard.png` | El tablero de SonarQube de los dos proyectos | http://localhost:9000 |

Las métricas de SonarQube en crudo ya están exportadas en `reportes/sonar/`.
