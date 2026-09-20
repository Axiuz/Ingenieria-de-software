# Capturas de la entrega

| Archivo | Qué muestra |
|---|---|
| `pipeline-exitoso.png` | Ejecución #5 del workflow CI/CD en la rama Proyecto-TuPastilla: los cuatro jobs en verde, despliegue incluido, y el resumen de Vitest con 87 pruebas |
| `despliegue.png` | *(pendiente)* Release `build-4` con el APK de prueba publicado y la imagen `ghcr.io/axiuz/tupastilla-api:latest` en GHCR |
| `despliegue-apk-en-emulador.png` | El APK de ese release instalado en el emulador, corriendo contra la imagen de GHCR levantada con `docker compose` |
| `sonar-dashboard.png` | Tablero de SonarQube Community con los dos proyectos: TuPastilla API (0 hallazgos, 97.7 % de cobertura) y TuPastilla Android (1 de seguridad, 48 de mantenibilidad, 97.4 % de cobertura), los dos con la Quality Gate en Passed |

Las métricas de SonarQube en crudo están exportadas en `reportes/sonar/`, y los reportes de ZAP
y MobSF en `reportes/seguridad-zap/` y `reportes/seguridad-movil/`.
