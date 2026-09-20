# Pruebas de seguridad  
18 de septiembre de 2026

## 1. Alcance

El alcance de las pruebas de seguridad abarca los siguientes componentes:

- La API `tupastilla-api` con los siguientes endpoints:  
  - POST `/api/auth/register`, `/login`, `/refresh`, `/logout`  
  - GET `/api/me`, `/api/permisos`, `/api/users`  
  - PATCH `/api/users/:id/role`  

- El paquete `com.tupastilla.auth` de la aplicación Android, responsable de la autenticación, gestión de tokens y validación de permisos.

El análisis se centra en la seguridad de la autenticación, autorización, protección contra inyecciones, consumo de recursos y configuración de cabeceras.

## 2. Metodología

Las pruebas se basan en las guías OWASP API Security Top 10 2023 y OWASP MASVS. Se aplican las siguientes fases:

1. **Pruebas automatizadas**: Se ejecutan 82 pruebas mediante Vitest y supertest en la API, cubriendo escenarios de autenticación, autorización, inyección y consumo de recursos.

2. **Análisis estático**:  
   - ESLint con `eslint-plugin-security` y regla de complejidad para detectar vulnerabilidades en el código.  
   - CodeQL security-extended en el pipeline CI/CD para identificar patrones de riesgo en el código.

3. **Gestión de dependencias**:  
   - `pnpm audit` para detectar vulnerabilidades en paquetes.  
   - Trivy aplicado a la imagen Docker para evaluar riesgos en el entorno de ejecución.

4. **Detección de secretos**:  
   - `gitleaks` escanea todo el historial del repositorio en busca de claves, tokens y credenciales.

5. **Pruebas dinámicas**:  
   - OWASP ZAP baseline aplicado en la API levantada con `docker compose` en el pipeline en modo pasivo: revisa cabeceras, cookies y respuestas de la API en ejecución. No sustituye a las pruebas de ataque de la suite, que cubren los casos activos.

6. **Pruebas manuales en vivo**:  
   - Uso de `curl` contra la API en ejecución (bcrypt real, coste 12): registro, `/api/me`, permisos por rol, 403 de un SUPERVISADO en `/api/users`, 401 genérico con contraseña mala y cabeceras de helmet.

7. **Revisión de código del cliente Android**:  
   - Validación de los flujos de autenticación, manejo de tokens y verificación de permisos en el cliente.

## 3. Matriz de pruebas de seguridad

| Ataque | Categoría OWASP | Prueba | Resultado esperado | Resultado |
|-------|----------------|--------|--------------------|-----------|
| Token con algoritmo `none` | API2 - Broken Authentication | test/auth.test.ts | 401 | 401 |
| Token firmado con otro secreto | API2 - Broken Authentication | test/auth.test.ts | 401 | 401 |
| Token HS512 con secreto correcto (confusión de algoritmo) | API2 - Broken Authentication | test/auth.test.ts | 401 | 401 |
| Token alterado para subir rol a ADMIN | API5 - Broken Function Level Authorization | test/auth.test.ts, test/api.test.ts | 401 | 401 |
| Token expirado, otro issuer, otra audiencia, rol desconocido | API2 - Broken Authentication | test/auth.test.ts | 401 | 401 |
| Reutilización de refresh token rotado | API2 - Broken Authentication | test/auth.test.ts | 401 y revocación de todas las sesiones | 401 y revocación |
| Usuario AUTONOMO o CUIDADOR accediendo a GET /api/users o modificando roles | API5 - Broken Function Level Authorization | test/api.test.ts | 403 | 403 |
| ADMIN intentando cambiar su propio rol | API5 - Broken Function Level Authorization | test/api.test.ts | 400 | 400 |
| Registro con rol ADMIN o campos extra (isAdmin, failedLogins) | API3 - Broken Object Property Level Authorization / Mass Assignment | test/api.test.ts, test/config-validation.test.ts | 400 | 400 |
| Fuerza bruta: 5 intentos fallidos seguidos | API2 - Broken Authentication | test/auth.test.ts | cuenta bloqueada 15 min, 401 genérico | bloqueo y 401 |
| Más de 20 peticiones a /api/auth en 15 min por IP | API4 - Unrestricted Resource Consumption | test/api.test.ts | 429 | 429 |
| Cuerpo de más de 10 kB | API4 - Unrestricted Resource Consumption | test/api.test.ts | 413 | 413 |
| Inyección SQL en correo e id ("' OR 1=1 --", "1 OR 1=1") o operador NoSQL {"$ne": null} en contraseña | API8 - Security Misconfiguration / Injection | test/api.test.ts, test/config-validation.test.ts | 400 o 401 sin autenticación | 400 o 401 |
| Enumeración de usuarios: correo inexistente vs contraseña mala | API2 - Broken Authentication | test/api.test.ts, test/auth.test.ts | mismo status y cuerpo | mismo status y cuerpo |
| Error interno con datos sensibles | API8 - Security Misconfiguration | test/api.test.ts | 500 genérico sin stack ni IP | 500 genérico sin stack ni IP |
| Cabeceras: sin x-powered-by, con HSTS, nosniff, CSP, X-Frame-Options | API8 - Security Misconfiguration | test/api.test.ts | presentes | presentes |
| CORS desde origen no permitido | API8 - Security Misconfiguration | test/api.test.ts | sin Access-Control-Allow-Origin | sin Access-Control-Allow-Origin |
| Secreto JWT de menos de 32 caracteres | API8 - Security Misconfiguration | test/config-validation.test.ts | API no arranca, mensaje no incluye valor | API no arranca, mensaje no incluye valor |

## 4. Vulnerabilidades encontradas y corregidas

| ID | Hallazgo | Severidad | Corrección | Evidencia |
|----|---------|----------|-----------|----------|
| V-01 | Dependencias vulnerables en el stack de API: 9 vulnerabilidades (4 altas, 5 moderadas) en paquetes transitivos de Prisma. Incluyen inyección en _.template (lodash), agotamiento de pila (deepmerge-ts), filtrado inadecuado de contraseñas (mariadb, mysql2). | Alta | Se aplicaron overrides en `pnpm-workspace.yaml`: lodash >=4.17.24, deepmerge-ts >=8, mariadb >=3.4.6, mysql2 >=3.23.1. | `pnpm audit` devuelve "No known vulnerabilities found"; 82 pruebas en verde. Pipeline ejecuta `pnpm audit --audit-level high` en cada push. |
| V-02 | Escalada de privilegios en el cliente: cualquier usuario podía cambiar su rol en Ajustes, permitiendo que un SUPERVISADO se convierta en AUTONOMO y edite medicamentos. | Alta | El rol se obtiene del JWT firmado por el servidor. Se eliminó la pantalla de selección de rol en onboarding y se muestra el rol en solo lectura. | `AjustesFragment.kt` sin acción de cambio de rol; `GET /api/users` responde 403 a un SUPERVISADO en la prueba en vivo. |
| V-03 | Riesgo de exponer los tokens de sesión guardados en el teléfono (almacenamiento local y respaldos en la nube). | Media | Se cifra el token con AES-256-GCM usando clave desde Android Keystore. El archivo de sesión se excluye de copias en nube y transferencias entre dispositivos. | `AlmacenSesion.kt`, `backup_rules.xml`, `data_extraction_rules.xml`. Si la clave no está (reinstalación o restauración), la sesión se borra y se pide login. |
| V-04 | Tráfico en claro permitido en release. | Media | `network_security_config` bloquea HTTP en release. HTTP solo permitido en debug hacia 10.0.2.2 y localhost. | `network_security_config.xml` en `src/main` y en `src/debug`. |
| V-05 | APK de release no ofuscado. | Baja | Se activó R8 con `isMinifyEnabled=true` y `isShrinkResources=true`. | `./gradlew :app:assembleRelease` compila con R8. |
| V-06 | Enumeración de usuarios por tiempo de respuesta en login: sin cuidado, un correo inexistente responde más rápido porque no se compara ningún hash. | Media | Cuando el correo no existe, se compara la contraseña contra un hash bcrypt ficticio y se responde el mismo 401 genérico, también con la cuenta bloqueada. | Prueba «con un correo inexistente responde 401 y aun así compara un hash» en `test/auth.test.ts`; misma respuesta en `test/api.test.ts`. |

V-07 a V-09 aparecieron en la prueba de extremo a extremo y se detallan en la sección 7. V-01 y V-02 eran las de mayor impacto: la primera exponía la conexión con la base de datos, la segunda anulaba la separación de roles en el propio teléfono. Las cuatro restantes son medidas de endurecimiento del cliente y del login.

## 5. Riesgos residuales aceptados

| Riesgo | Por qué se acepta | Mitigación actual | Acción en el plan de mejora |
|--------|-------------------|-------------------|-----------------------------|
| Token válido hasta 15 min tras cambio de rol o cierre de sesión (JWT sin estado) | Validar cada token contra la base quita la ventaja de un JWT sin estado. | Vida de 15 min; el cambio de rol revoca todos los refresh del usuario. | Guardar una versión de token por usuario y rechazar tokens con versión vieja. |
| Bloqueo de cuenta para negar servicio a una víctima conocida | Es una medida de seguridad para prevenir ataques de fuerza bruta. | Bloqueo temporal de 15 minutos y límite por IP. | Reemplazar bloqueo por CAPTCHA o retraso progresivo. |
| Registro responde 409 si el correo ya existe | Sin verificación de correo no hay otra forma de avisar que la cuenta existe. | Límite de 20 peticiones por IP cada 15 min en `/api/auth`. | Verificación por correo con respuesta idéntica exista o no la cuenta. |
| Datos de medicamentos solo en Room, no sincronizados | La app nació sin servidor; mover los datos es un cambio de arquitectura. | Room vive en el almacenamiento privado de la app. | Sincronizar datos de medicamentos con la API por usuario. |
| Referencias en GitHub por versión mayor, no por SHA | Una etiqueta movida en una acción de terceros ejecutaría código ajeno en el pipeline. | Permisos mínimos por job; el despliegue solo corre en `main`, tags `v*` o a mano. | Fijar referencias por SHA con Dependabot. |

## 6. Cómo reproducir

```bash
cd tupastilla-api && pnpm install && pnpm test:coverage && pnpm audit && pnpm lint
./gradlew :app:testDebugUnitTest :app:verificarCoberturaAuth :app:lintDebug
```

Los escaneos de seguridad se ejecutan en el job `seguridad` del workflow `.github/workflows/ci-cd.yml`, con estas herramientas:

- CodeQL (`security-extended`): análisis estático del TypeScript.
- gitleaks: secretos en todo el historial, con `.gitleaks.toml` en la raiz.
- Trivy: vulnerabilidades CRITICAL y HIGH de la imagen Docker; rompe el pipeline si hay alguna con parche.
- ZAP baseline: escaneo pasivo de la API levantada con `docker compose`.
- ZAP API scan: escaneo activo guiado por el contrato `tupastilla-api/openapi.yaml`.

Los reportes generados se almacenan como artefactos en el pipeline. gitleaks, Trivy y ZAP también se ejecutaron en local con sus imágenes oficiales de Docker; los resultados están en la sección 7.

## 7. Resultados de los escaneos y prueba de extremo a extremo
18 de septiembre de 2026
### 7.1 Escaneos
| Herramienta         | Alcance                              | Resultado                                  |
|---------------------|--------------------------------------|--------------------------------------------|
| gitleaks            | historial (7 commits), archivos pendientes | 0 secretos detectados                     |
| Trivy 0.57.1        | imagen de la API                    | 4 HIGH al inicio (npm: brace-expansion x2, ip-address, tar); tras eliminar npm, npx y corepack: 0 HIGH y 0 CRITICAL |
| OWASP ZAP baseline  | API en Docker con MySQL            | 66 reglas pasadas; 1 aviso (10049) corregido, ver sección 8 |
| pnpm audit --prod   | dependencias prod                  | 0 vulnerabilidades                       |
| CodeQL              | ejecución en GitHub               | pendiente de primera ejecución en pipeline |
### 7.2 Prueba de extremo a extremo
- Migración y semilla aplicadas; administrador creado desde variables de entorno.
- Registro de AUTONOMO, SUPERVISADO y CUIDADOR: 201; pedir rol ADMIN al registrarse: 400.
- GET /api/users: ADMIN 200, SUPERVISADO 403; ADMIN cambia rol de usuario: 200.
- Refresh: rota token; reutilizar viejo devuelve 401 y revoca el nuevo.
- 5 contraseñas malas: cuenta bloqueada; contraseña correcta responde con 401 genérico. Más de 20 intentos por IP: 429.
- App: validación de campos, contraseña incorrecta con mensaje genérico, registro desde app (cuenta en MySQL con hash bcrypt de coste 12). SUPERVISADO ve lista sin poder editarla. Cerrar sesión revoca refresh en MySQL y no regresa a sesión. CUIDADOR entra con pestaña Personas.
**Fallos encontrados en esta prueba**
| ID  | Hallazgo                                                                 | Severidad | Corrección                                                                 | Evidencia                                  |
|-----|--------------------------------------------------------------------------|-----------|-----------------------------------------------------------------------------|--------------------------------------------|
| V-07 | Al cerrar sesión y entrar con otra cuenta en el mismo teléfono, la nueva cuenta veía los medicamentos, el nombre y las alarmas de la anterior (MASVS-STORAGE / privacidad) | Alta      | `CuentaLocal` guarda de qué cuenta son los datos; si entra otra, se cancelan alarmas y notificaciones y se vacían Room y preferencias | Emulador: el cuidador entra al onboarding sin datos del autónomo; las 2 alarmas del autónomo aparecen como alarm_cancelled; 6 pruebas en `CuentaLocalTest.kt` |
| V-08 | 4 vulnerabilidades HIGH en el npm incluido en la imagen Docker           | Alta      | Se quitan npm, npx y corepack de la imagen final                            | Trivy 0 HIGH / 0 CRITICAL                 |
| V-09 | La app leía la respuesta de la API en hilo principal: cierre de app con NetworkOnMainThreadException al iniciar sesión | Alta      | Cuerpo leído dentro del dispatcher de IO; corte de red al leerlo es fallo de red | 2 pruebas nuevas en `AuthApiTest.kt` que fallan con código anterior y pasan con nuevo |

## 8. Escaneos de esta entrega (20 de septiembre de 2026)

### 8.1 OWASP ZAP

Tres escaneos, con sus reportes en `reportes/seguridad-zap/`:

1. **Baseline pasivo sin reglas ignoradas** contra la API en Docker: 66 reglas pasadas y un
   aviso, `10049 Storable and Cacheable Content`, en `/health` y en las rutas 404.
2. **Escaneo activo con el contrato OpenAPI** y un token de ADMIN inyectado como cabecera
   `Authorization`, de modo que ZAP sí recorrió los endpoints protegidos: 13 rutas, 48 URLs,
   118 reglas pasadas, 0 fallos y 0 avisos.
3. **ZAP como proxy del emulador Android**, para ver el tráfico real de la app: registro,
   login fallido, login correcto y cierre de sesión. 3 alertas informativas, ninguna de riesgo.

| ID | Hallazgo | Severidad | Corrección | Evidencia |
|----|---------|-----------|-----------|-----------|
| V-10 | Las respuestas de la API no traían `Cache-Control`. Una caché intermedia podía almacenar la respuesta del login, que lleva el access token y el refresh token. | Baja | Middleware que añade `Cache-Control: no-store` a todas las respuestas, antes de cualquier ruta, en `src/app.ts`. Cinco pruebas nuevas cubren 200, 404, 401, login y 400. | El segundo escaneo reporta `Non-Storable Content` en lugar de `Storable and Cacheable Content`: `reportes/seguridad-zap/2-despues/` |

Con esa corrección dejó de hacer falta silenciar reglas: se borró `.zap/rules.tsv`. El aviso
`10020` (anti-clickjacking) que antes se ignoraba ya no aparece, porque helmet envía
`X-Frame-Options: SAMEORIGIN`.

Durante el escaneo móvil, el cierre de sesión respondió **429**: el límite de 20 peticiones cada
15 minutos por IP seguía agotado por el escaneo activo anterior. La app cerró la sesión
localmente de todos modos. Es el comportamiento esperado del rate limit, no un defecto.

### 8.2 SonarQube Community

| Métrica | API antes | API después | App antes | App después |
|---|---|---|---|---|
| Vulnerabilities | 0 | 0 | 4 | 1 |
| Bugs | 0 | 0 | 0 | 0 |
| Code Smells | 0 | 0 | 53 | 48 |
| Deuda técnica | 0 min | 0 min | 286 min | 250 min |
| Cobertura | 97.7 % | 97.7 % | sin medir | 97.4 % |

Corregidos: tráfico en claro implícito en el manifiesto (`xml:S5332`), tres literales duplicados
en los datos de prueba (`kotlin:S1192`) y tres pruebas repetidas en la API (`typescript:S5976`).
Marcados como falso positivo con su justificación: la difusión de intents de `AlarmaScheduler`
(el `Intent` es explícito hacia un receptor no exportado) y los dos dispatchers de los ViewModel
(`AlarmManager` es una llamada bloqueante del sistema). Aceptado con justificación: la clave del
Keystore utilizable sin autenticación del usuario, porque la sesión se renueva en segundo plano.

### 8.3 Falso positivo de gitleaks

En el primer push con los reportes, gitleaks detuvo el pipeline con más de cien hallazgos
`generic-api-key` en `reportes/sonar/*-hallazgos.json`. Son los identificadores de los hallazgos
que exporta SonarQube: cadenas tipo UUID con entropía alta, no credenciales. Se revisó el
contenido de `reportes/` en busca de tokens JWT, llaves privadas y contraseñas de prueba y no hay
ninguno. Se añadió `.gitleaks.toml` con una excepción acotada a esos dos archivos; el resto del
repositorio y del historial se sigue escaneando igual.

### 8.4 MobSF sobre el APK de release

Puntuación de seguridad 61/100 (`reportes/seguridad-movil/`). Hallazgos altos: la falta de
certificado de firma —se analizó el APK sin firmar; el que publica el pipeline va firmado— y el
`minSdk 24`, aceptado porque el público de la app usa teléfonos viejos. Aviso revisado:
`allowBackup=true`, mitigado porque `backup_rules.xml` y `data_extraction_rules.xml` excluyen el
archivo de sesión. Falsos positivos: generador aleatorio inseguro y registros en el log, los dos
en código de librerías ofuscado por R8, y "posibles secretos embebidos", que son una etiqueta de
interfaz y dos identificadores de recursos. El análisis confirma que no hay rastreadores de
privacidad y que el tráfico en claro está prohibido en release.
