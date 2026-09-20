# Cómo cumple TuPastilla cada punto de la rúbrica
20 de septiembre de 2026

Cada sección dice qué pide la rúbrica, dónde está resuelto en el repositorio, con qué comando
se comprueba y qué evidencia hay en `reportes/`.

| Criterio | Valor | Evidencia principal | Dónde |
|---|---|---|---|
| 1. Implementación del módulo y seguridad | 20 % | Módulo de cuentas y roles con JWT, 87 + 55 pruebas, cobertura 100 % y 97.7 % | `tupastilla-api/src`, `app/src/main/java/com/tupastilla/auth`, `reportes/cobertura/` |
| 2. Pipeline de CI/CD | 25 % | 4 jobs: pruebas, construcción, seguridad y despliegue | `.github/workflows/ci-cd.yml`, `evidencias/` |
| 3. Pruebas de seguridad y calidad | 25 % | SonarQube, OWASP ZAP (API y tráfico de la app) y MobSF, con hallazgos corregidos | `reportes/sonar/`, `reportes/seguridad-zap/`, `reportes/seguridad-movil/` |
| 4. Cierre del proyecto y análisis | 15 % | Planeado contra ejecutado y lecciones aprendidas | `docs/informe-cierre.md` |
| 5. Plan de mejora continua | 15 % | 8 acciones medibles con prioridad y 4 innovaciones | `docs/plan-mejora.md` |

---

## 1. Implementación del módulo y seguridad — 20 puntos

**Módulo elegido:** cuentas de usuario y roles de TuPastilla. Es el módulo del que dependen el
resto de pantallas: sin sesión la app no abre ninguna de sus cuatro pestañas.

| Requisito de la rúbrica | Cómo está resuelto | Archivo | Cómo se comprueba |
|---|---|---|---|
| Una operación de creación | `POST /api/auth/register` crea la cuenta y devuelve sesión. Desde la app, pantalla "Crea tu cuenta" | `tupastilla-api/src/auth/routes.ts`, `app/src/main/java/com/tupastilla/auth/RegistroFragment.kt` | `reportes/seguridad-zap/3-trafico-app/trafico-app.md` línea 3: 201 Created desde el emulador |
| Una operación de consulta | `GET /api/me`, `GET /api/permisos` y `GET /api/users` | `tupastilla-api/src/users/routes.ts` | `tupastilla-api/test/api.test.ts` |
| Autenticación con JWT | Access token HS256 de 15 min con issuer y audience propios; refresh token de 7 días rotatorio, guardado solo como hash SHA-256 | `tupastilla-api/src/auth/tokens.ts` | 87 pruebas en `pnpm test` |
| Al menos dos roles | Cuatro: `ADMIN`, `AUTONOMO`, `SUPERVISADO`, `CUIDADOR` | `tupastilla-api/src/domain.ts`, `prisma/schema.prisma` | `GET /api/permisos` devuelve los permisos del rol |
| Operación solo de administrador | `GET /api/users` y `PATCH /api/users/:id/role` exigen rol `ADMIN` | `requireRole("ADMIN")` en `src/users/routes.ts` | Prueba: un `SUPERVISADO` recibe 403 |
| Operación para cualquier autenticado | `GET /api/me` y `GET /api/permisos` | `src/users/routes.ts` | Prueba con token de cada rol |
| Sin token o sin rol no se pasa | `requireAuth` responde 401 y `requireRole` 403; se rechazan tokens con alg `none`, firma ajena, HS512, expirados, de otro issuer o audiencia, y los alterados para subir de rol | `tupastilla-api/src/auth/middleware.ts` | Matriz completa en `docs/seguridad.md`, sección 3 |
| El secreto no está en el código | `JWT_SECRET` se lee del entorno y se valida con Zod: la API no arranca con menos de 32 caracteres, y el mensaje de error nunca imprime el valor | `tupastilla-api/src/config.ts` | `test/config-validation.test.ts`; gitleaks revisa todo el historial en el pipeline |
| Aplicación móvil con backend | La app Android consume la API por HTTPS en release; el token se guarda cifrado con AES-256-GCM y clave del Android Keystore | `app/src/main/java/com/tupastilla/auth/AlmacenSesion.kt` | `docs/seguridad.md`, hallazgo V-03 |

### Pruebas unitarias y cobertura

| | API (`tupastilla-api`) | App, paquete `auth` | Mínimo de la rúbrica |
|---|---|---|---|
| Herramientas | Vitest + supertest, cobertura V8 | JUnit 4 + MockWebServer, JaCoCo | — |
| Pruebas | 87, 0 fallos | 55, 0 fallos | — |
| Cobertura de líneas | **100 %** | **97.7 %** | 80 % |
| Cobertura de ramas | 92.7 % | 97.0 % | 80 % |
| Cobertura de funciones | 94.7 % | 87.5 % | — |

Los umbrales están escritos en `tupastilla-api/vitest.config.ts` y en la tarea
`verificarCoberturaAuth` de `app/build.gradle.kts`: si la cobertura baja de 80 %, el comando
falla y el pipeline se detiene.

**Evidencia:** `reportes/pruebas-unitarias/api/junit.xml`, `reportes/pruebas-unitarias/android/`,
`reportes/cobertura/api/index.html` y `reportes/cobertura/android/html/index.html`.

**Reproducir:**

```bash
cd tupastilla-api && pnpm install --frozen-lockfile && pnpm test:coverage
./gradlew :app:testDebugUnitTest :app:verificarCoberturaAuth
```

---

## 2. Pipeline de CI/CD — 25 puntos

Archivo: `.github/workflows/ci-cd.yml`. Se dispara con `push` y `pull_request` a la rama
principal del proyecto (`Proyecto-TuPastilla`), con las etiquetas `v*` y a mano.

| Etapa de la rúbrica | Job | Qué hace |
|---|---|---|
| Pruebas | `api` | `pnpm lint`, `pnpm typecheck`, `pnpm test:coverage` y `pnpm audit --prod --audit-level high` |
| Pruebas | `android` | `testDebugUnitTest`, `verificarCoberturaAuth` y `lintDebug` |
| Construcción | `android` | `assembleRelease`: genera el APK |
| Construcción | `seguridad` | `docker build` de la imagen de la API |
| Despliegue | `deploy` | Sube la imagen a GHCR y publica el APK en GitHub Releases |

El job `deploy` declara `needs: [api, android, seguridad]`: no se ejecuta si alguna prueba falla.
Esa es la evidencia de que las pruebas corren antes del despliegue.

El job `seguridad` añade lo que pide el punto 3: gitleaks sobre todo el historial —con una
excepción documentada en `.gitleaks.toml` para los identificadores de hallazgos de SonarQube—, CodeQL
`security-extended`, Trivy sobre la imagen (rompe el pipeline con cualquier CRITICAL o HIGH con
parche) y OWASP ZAP —baseline y escaneo con el contrato OpenAPI— contra la API levantada con
`docker compose`.

**Entorno de prueba desplegado.** El pipeline publica dos artefactos instalables:

1. La imagen `ghcr.io/axiuz/tupastilla-api` con la etiqueta `latest` y la del commit.
2. El APK de prueba `TuPastilla-prueba-<rama>.apk` en GitHub Releases.

Para verificarlo sin compilar nada:

```bash
cp .env.example .env            # completar MYSQL_PASSWORD, MYSQL_ROOT_PASSWORD y JWT_SECRET
docker compose up -d            # baja la imagen de GHCR, migra y siembra MySQL
curl http://localhost:3000/health
adb install TuPastilla-prueba-Proyecto-TuPastilla.apk   # el APK apunta a http://10.0.2.2:3000
```

Si existen los secretos `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS` y `ANDROID_KEY_PASSWORD`, el mismo job publica además el APK de release
firmado. Sin esos secretos el despliegue no falla: publica solo el APK de prueba.

**Verificación hecha el 20 de septiembre de 2026.** Con los artefactos que publicó la ejecución
[35495568857](https://github.com/Axiuz/Ingenieria-de-software/actions/runs/35495568857), sin
compilar nada: `docker compose up -d` bajó `ghcr.io/axiuz/tupastilla-api:latest`, migró y sembró
MySQL y respondió `{"status":"ok"}` en `/health` y 401 en `/api/me` sin token; el APK del release
[build-4](https://github.com/Axiuz/Ingenieria-de-software/releases/tag/build-4) se instaló en el
emulador y la cuenta creada desde la app quedó en MySQL con hash bcrypt de coste 12.

**Evidencia:** `evidencias/despliegue-apk-en-emulador.png` (APK del release corriendo contra esa
API), `evidencias/pipeline-exitoso.png` y el enlace a la ejecución en GitHub Actions.

---

## 3. Pruebas de seguridad y calidad del código — 25 puntos

### 3.1 Análisis de calidad con SonarQube

Instancia local de SonarQube Community en Docker (`sonarqube:community`, puerto 9000). Dos
proyectos: `tupastilla-api` (scanner CLI, lee `coverage/lcov.info`) y `tupastilla-android`
(plugin `org.sonarqube` de Gradle, lee el XML de JaCoCo). La configuración está versionada en
`tupastilla-api/sonar-project.properties` y en `build.gradle.kts`. El token nunca se escribe en
el repositorio: se pasa por la variable `SONAR_TOKEN`.

**Métricas del primer análisis y del análisis posterior a las correcciones**

| Métrica | API antes | API después | App antes | App después |
|---|---|---|---|---|
| Bugs | 0 | 0 | 0 | 0 |
| Vulnerabilities | 0 | 0 | 4 | 1 |
| Security Hotspots | 0 | 0 | 0 | 0 |
| Code Smells | 0 | 0 | 53 | 48 |
| Deuda técnica (min) | 0 | 0 | 286 | 250 |
| Duplicación | 0.0 % | 0.0 % | 0.0 % | 0.0 % |
| Cobertura | 97.7 % | 97.7 % | sin medir | 97.4 % |
| Líneas de código | 685 | 685 | 6 471 | 6 475 |
| Quality Gate | OK | OK | OK | OK |

Ficheros: `reportes/sonar/1-inicial/` y `reportes/sonar/2-despues/` (métricas y lista completa de
hallazgos en JSON, exportados con la API web de SonarQube).

**Hallazgos revisados**

| Regla | Dónde | Interpretación | Acción |
|---|---|---|---|
| `xml:S5332` — tráfico en claro | `AndroidManifest.xml` | Real: con `minSdk 24` el atributo queda implícito en dispositivos viejos | Corregido: `android:usesCleartextTraffic="false"`. Sonar lo marca CLOSED/FIXED |
| `kotlin:S1192` — literal duplicado (3 hallazgos críticos) | `data/local/SeedDataSource.kt` | Real de mantenimiento: `"1 pastilla"` aparecía 7 veces | Corregido: constantes `UNA_PASTILLA`, `UNA_CAPSULA`, `OVALADA_BLANCA` |
| `typescript:S5976` — pruebas repetidas | `test/api.test.ts` | Real: tres pruebas nuevas idénticas salvo la ruta | Corregido con `it.each`; la Quality Gate de la API volvió a OK |
| `kotlin:S5320` — difusión de intents | `alarma/AlarmaScheduler.kt:81` | Falso positivo: el `Intent` es explícito hacia `AlarmaTomaReceiver`, declarado `android:exported="false"` | Marcado como falso positivo con ese comentario |
| `kotlin:S6311` — dispatcher innecesario (2) | `HoyViewModel.kt`, `EdicionViewModel.kt` | Falso positivo: `reprogramarTodo` llama a `AlarmManager`, que es una llamada bloqueante al sistema, no una función suspend | Marcado como falso positivo |
| `kotlin:S6288` — clave usable sin autenticación | `auth/AlmacenSesion.kt:65` | Riesgo aceptado: exigir autenticación del usuario impediría renovar la sesión en segundo plano; la clave AES vive en el Android Keystore | Marcado como aceptado, documentado en `docs/seguridad.md` |
| `kotlin:S6474` — sin `verification-metadata.xml` | Proyecto Gradle | Real, pendiente: verificar las dependencias exige fijar checksums de todas | Acción 5 del plan de mejora |

Las 48 code smells restantes son avisos de Android Lint heredados (recursos sin usar, overdraw,
`SetTextI18n`) detallados en `docs/calidad.md`, sección 3, con su prioridad.

### 3.2 Pruebas de seguridad con OWASP ZAP

Tres escaneos, todos contra la propia API levantada con `docker compose`:

| # | Tipo | Alcance | Resultado |
|---|---|---|---|
| 1 | Baseline (pasivo), sin reglas ignoradas | `http://localhost:3000/health` | 66 reglas pasadas, **1 aviso**: `10049 Storable and Cacheable Content` en 3 URLs |
| 2 | API scan (activo) con `openapi.yaml` y token de ADMIN | 13 endpoints, 48 URLs | 118 reglas pasadas, 0 fallos y 0 avisos |
| 3 | Proxy del emulador Android | Tráfico real de la app | 3 alertas informativas, ninguna de riesgo |

**Hallazgos revisados y acciones**

| Hallazgo | Riesgo | Endpoint | Interpretación del equipo | Acción |
|---|---|---|---|---|
| `10049` Storable and Cacheable Content | Bajo (informativo) | `/health`, `/`, `/sitemap.xml` | Real: sin `Cache-Control` una caché intermedia puede guardar respuestas JSON; la del login lleva el access y el refresh token | Se añadió `Cache-Control: no-store` a todas las respuestas (`src/app.ts`) y una prueba parametrizada. El segundo escaneo reporta `Non-Storable Content` |
| `10020` Anti-clickjacking header | Bajo | todas | Falso positivo: helmet ya envía `X-Frame-Options: SAMEORIGIN` y es una API JSON sin HTML | Se borró el archivo `.zap/rules.tsv`: ya no hace falta silenciar ninguna regla |
| `100000` Client Error response | Informativo | 36 URLs | Esperado: son los 401, 403, 404 y 429 que devuelve el módulo ante peticiones inválidas | Sin acción; es el comportamiento buscado |
| `429 Too Many Requests` al cerrar sesión desde la app | — | `/api/auth/logout` | Observación del escaneo móvil: el límite de 20 peticiones cada 15 minutos por IP seguía agotado por el escaneo activo. La app cerró la sesión localmente de todos modos | Sin acción; confirma que el rate limit funciona |

Antes de la corrección el escaneo avisaba `Storable and Cacheable Content`; después avisa
`Non-Storable Content`, que es la confirmación de que la cabecera llegó. Evidencia:
`reportes/seguridad-zap/1-inicial/` y `reportes/seguridad-zap/2-despues/` (HTML, JSON y Markdown
de cada escaneo).

**Escaneo con autenticación.** El escaneo activo se ejecutó con un token de `ADMIN` inyectado
como cabecera `Authorization`, así que ZAP sí recorrió los endpoints protegidos: `/api/me`,
`/api/permisos`, `/api/users` y `PATCH /api/users/{id}/role`. El contrato
`tupastilla-api/openapi.yaml` se versiona en el repositorio y lo usa también el pipeline.

### 3.3 Aplicación móvil: tráfico y APK

**ZAP como proxy del emulador.** Emulador Pixel_7 arrancado con `-http-proxy` apuntando a ZAP.
Recorrido: cierre de sesión, registro de una cuenta nueva, intento de login con contraseña
incorrecta y login correcto. Lo capturado está en
`reportes/seguridad-zap/3-trafico-app/trafico-app.md` y el reporte de alertas en
`trafico-app.html`. Observaciones:

- El login con contraseña incorrecta responde **401 con un mensaje genérico**: no revela si el
  correo existe.
- La app no vuelve a pedir el perfil al servidor: lee el rol del JWT firmado, que es lo que
  cierra el hallazgo V-02 de escalada de privilegios.
- En debug el tráfico va en claro hacia `10.0.2.2` a propósito, para poder inspeccionarlo. En
  release `network_security_config` lo prohíbe.

**MobSF sobre el APK de release.** Puntuación de seguridad **61/100**. Reporte completo en
`reportes/seguridad-movil/mobsf-tupastilla.pdf` y resumen en `mobsf-resumen.json`.

| Hallazgo | Severidad | Interpretación | Acción |
|---|---|---|---|
| Falta certificado de firma | Alta | Esperado: se analizó `app-release-unsigned.apk`. El APK que publica el pipeline sí va firmado cuando existe el keystore | Sin acción |
| Instalable en Android 7.0 (`minSdk 24`) | Alta | Real y aceptado: es el público de la app, teléfonos viejos de adultos mayores | Riesgo aceptado, documentado |
| `allowBackup=true` | Media | Parcial: la copia permite restaurar medicinas al cambiar de teléfono, pero el archivo de sesión ya está excluido en `backup_rules.xml` y `data_extraction_rules.xml` | Mitigado; revisión en el plan de mejora |
| Generador aleatorio inseguro y registros en el log | Media / informativa | Falsos positivos: los cuatro archivos señalados son código ofuscado por R8 de librerías de terceros, no de `com.tupastilla` | Sin acción |
| "Posibles secretos embebidos" | Media | Falso positivo: son la cadena `"campo_password": "Contraseña"` y dos identificadores de recursos | Sin acción |
| Sin rastreadores de privacidad, tráfico en claro prohibido, pinning de certificados detectado | — | Confirmaciones positivas del análisis | — |

### 3.4 Ciclo completo

`analizar → interpretar → corregir → volver a analizar → documentar` se cumplió en los tres
frentes: ZAP (10049), SonarQube (S5332, S1192, S5976) y las correcciones previas de dependencias
e imagen Docker descritas en `docs/seguridad.md`.

---

## 4. Cierre del proyecto y análisis — 15 puntos

`docs/informe-cierre.md` (y su versión en Word) contiene la tabla comparativa planeado contra
ejecutado, el detalle por historia de Jira, las desviaciones con su causa y las lecciones
aprendidas agrupadas por los temas que pide la rúbrica: decisiones de arquitectura, problemas al
implementar autenticación y roles, dificultad para automatizar pruebas y despliegues, utilidad de
las pruebas unitarias, defectos detectados por el análisis estático y las pruebas de seguridad, y
qué cambiaría el equipo si empezara de nuevo.

## 5. Plan de mejora continua — 15 puntos

`docs/plan-mejora.md` contiene ocho acciones con responsable, indicador medible, meta, prioridad y
fecha límite, y cuatro propuestas de innovación ligadas a necesidades reales del proyecto (aviso
al cuidador por toma omitida, lectura de la receta con la cámara, recordatorios por voz y reporte
de cumplimiento para el médico), cada una con su acción concreta y su indicador de éxito.

---

## Entregables

| Pedido | Dónde |
|---|---|
| Código fuente completo del módulo | `tupastilla-api/src/`, `app/src/main/java/com/tupastilla/auth/` |
| Pruebas unitarias | `tupastilla-api/test/`, `app/src/test/`, `app/src/androidTest/` |
| Configuración de JWT y roles | `tupastilla-api/src/config.ts`, `src/auth/`, `prisma/schema.prisma` |
| Archivo del pipeline | `.github/workflows/ci-cd.yml` |
| Archivos para construir y ejecutar | `docker-compose.yml`, `docker-compose.dev.yml`, `Dockerfile`, `Scripts/start.sh` |
| README con instrucciones | `README.md` |
| Reportes técnicos | `reportes/` |
| Informe de cierre | `docs/informe-cierre.docx` |
| ZIP de entrega | `Scripts/empaquetar-entrega.sh` lo genera |

Ningún archivo del repositorio ni del ZIP contiene contraseñas, tokens ni llaves: los valores de
ejemplo están en `.env.example` y `tupastilla-api/.env.example`, los `.env` reales están
ignorados por git y gitleaks revisa el historial completo en cada ejecución del pipeline.
