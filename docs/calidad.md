# Análisis de calidad de código  
20 de septiembre de 2026  

## 1. Herramientas  
- Vitest con @vitest/coverage-v8 para pruebas unitarias y cobertura de código.  
- ESLint 10 con plugins typescript-eslint y eslint-plugin-security para validación de estilos, tipos y vulnerabilidades.  
- TypeScript con configuración estricta (strict, noUncheckedIndexedAccess) para detección de errores de tipos.  
- jscpd para análisis de duplicación de código.
- SonarQube Community Build en Docker para el tablero de métricas del proyecto completo (calidad, seguridad, duplicación y cobertura).  
- pnpm audit para escaneo de vulnerabilidades en dependencias.  
- Android Lint para análisis de calidad en interfaces y recursos.  
- JUnit 4 con MockWebServer para probar el cliente HTTP y la sesión contra un servidor simulado.  
- JaCoCo para medición de cobertura de código en pruebas.  

## 2. Métricas  

| Métrica | API | App Android (paquete auth) | Meta | Estado |  
|--------|-----|-----------------------------|------|--------|  
| Líneas de código de producción | 754 | 576 | — | — |  
| Líneas de pruebas | 712 | 533 | — | — |  
| Pruebas ejecutadas | 87 (0 fallos) | 55 en la app, 42 del paquete auth (0 fallos) | 0 fallos | Cumple |  
| Cobertura de líneas | 100 % | 97.7 % | 80 % | Superada |  
| Cobertura de ramas | 92.7 % | 97.0 % | 80 % | Superada |  
| Cobertura de sentencias/instrucciones | 97.8 % | 93.2 % | 80 % | Superada |  
| Cobertura de funciones/métodos | 94.7 % | 87.5 % | 80 % | Superada |  
| Complejidad ciclomática máxima por función | ≤ 10 (regla ESLint que rompe el build); 3 funciones pasan de 5 | 89.9 % de la complejidad cubierta por pruebas (JaCoCo) | ≤ 10 | Cumple |  
| Duplicación (jscpd, mínimo 40 tokens) | 0 clones, 0 % | — | < 3 % | Cumple |  
| Errores de tipos | 0 (tsc --noEmit) | 0 (compila sin errores) | 0 | Cumple |  
| ESLint | 0 errores, 0 avisos | — | 0 errores | Cumple |  
| Android Lint | — | 0 errores, 51 avisos (ninguno en el paquete auth) | 0 errores | Cumple |  
| Vulnerabilidades en dependencias | 0 (antes 9) | — | 0 altas | Cumple |  

*Nota: La cobertura se mide en código de dominio. Se excluyen server.ts, db.ts, cliente Prisma generado y repositorio Prisma (probados con base real). En Android, se excluyen Fragment y almacén con Keystore (requieren dispositivo o emulador).*  

## 3. Deuda técnica detectada  

| Elemento | Origen | Impacto | Prioridad |  
|---------|--------|--------|----------|  
| 51 avisos de Android Lint (desglose abajo) | Código previo al login | Rendimiento, mantenimiento e internacionalización | Media |  
| 15 dependencias con versión nueva (GradleDependency) | Dependencias de AndroidX y Material fijadas en 2024 | Parches de seguridad y correcciones pendientes | Alta |  
| 5 dependencias con versión más reciente disponible (NewerVersionAvailable) | Mismo origen | Mismo caso: versiones con correcciones sin adoptar | Media |  
| 14 recursos sin usar | Strings de placeholder, ic_mas, estilo | APK más grande y ruido al mantener | Baja |  
| 5 UseKtx | Código previo | Legibilidad: hay extensiones KTX equivalentes | Baja |  
| 4 Overdraw | Diseño de vistas | Pérdida de rendimiento en pantallas | Media |  
| 2 SetTextI18n | Concatenación en Historial y Detalle | Problema de internacionalización | Media |  
| targetSdk 35 con SDK 37 disponible (OldTargetApi) | Configuración inicial | El sistema aplica modos de compatibilidad; falta probar los cambios de comportamiento de API 36 y 37 | Alta |  
| NotifyDataSetChanged en adaptador | Código antiguo | Posible lag en actualizaciones de lista | Media |  
| Faltan pruebas instrumentadas | Pantallas de login y registro | Riesgo de fallos en flujo de usuario | Alta |  

## 4. Cómo se mantiene  
- Umbral de 80 % en `vitest.config.ts` y en la tarea `verificarCoberturaAuth` de Gradle.  
- Si cualquiera de los umbrales se baja, el pipeline se rompe.  
- Reglas de complejidad: máximo 10 en ciclomática, profundidad máxima 3, máximo 4 parámetros por función.  
- ESLint corre en el job `api` del pipeline: una función con complejidad mayor a 10 o más de 4 parámetros lo detiene.  

## 5. Cómo reproducir  
- Ejecutar: `pnpm test:coverage` para cobertura de pruebas.  
- `pnpm lint` para validación de estilos y seguridad.  
- `pnpm typecheck` para verificación de tipos.  
- `pnpm dlx jscpd@4 src --min-tokens 40` para análisis de duplicación.  
- `./gradlew :app:verificarCoberturaAuth :app:lintDebug` para análisis de Android.  
- Reportes generados en: `tupastilla-api/coverage/` y `app/build/reports/`.

## 6. Defectos encontrados en la prueba de extremo a extremo
La prueba con MySQL real, Docker y la app en un emulador encontró cinco defectos que las pruebas unitarias no detectaban. Todos están corregidos y verificados.

| Defecto | Por qué no lo detectaban las pruebas unitarias | Corrección |
|---|---|---|
| La migración inicial de Prisma incluía el aviso de actualización de la CLI y MySQL la rechazaba | Las pruebas usan repositorios en memoria, sin base de datos | Se limpió `migration.sql`; migración y semilla se aplican en MySQL 8.4 |
| La app leía la respuesta de la API en el hilo principal y se cerraba al iniciar sesión | Las pruebas ejecutan todo en el mismo hilo | El cuerpo se lee en el dispatcher de IO; 2 pruebas de regresión en `AuthApiTest.kt` |
| Al cambiar de cuenta en el mismo teléfono, la nueva cuenta veía los datos de la anterior | No había pruebas del flujo completo de cambio de cuenta | `CuentaLocal` borra los datos locales si entra otra cuenta; 6 pruebas en `CuentaLocalTest.kt` |
| La tarea de cobertura de Gradle fallaba al correr junto con lint, como hace el pipeline | Cada tarea se había ejecutado por separado | `executionData` apunta al archivo exacto de JaCoCo |
| La imagen Docker traía 4 vulnerabilidades altas en su npm | Trivy solo corre sobre la imagen construida | Se quitaron npm, npx y corepack de la imagen final |

## 7. Tablero de SonarQube

Instancia local de SonarQube Community (`sonarqube:community` en Docker, puerto 9000) con dos
proyectos: `tupastilla-api`, analizado con el scanner CLI y la configuración versionada en
`tupastilla-api/sonar-project.properties`, y `tupastilla-android`, analizado con el plugin
`org.sonarqube` de Gradle declarado en `build.gradle.kts`. El token de análisis se pasa por la
variable `SONAR_TOKEN` y no está en el repositorio.

| Métrica | tupastilla-api | tupastilla-android |
|---|---|---|
| Líneas de código | 685 | 6 475 |
| Bugs | 0 | 0 |
| Vulnerabilities | 0 | 1 (aceptada: falta `verification-metadata.xml`) |
| Security Hotspots | 0 | 0 |
| Code Smells | 0 | 48 |
| Deuda técnica | 0 min | 250 min |
| Duplicación | 0.0 % | 0.0 % |
| Cobertura | 97.7 % | 97.4 % (paquete `auth`) |
| Quality Gate | OK | OK |

Las 48 code smells que quedan son avisos heredados de Android Lint —recursos sin usar, overdraw,
`SetTextI18n`, `NotifyDataSetChanged`— y están en la tabla de deuda técnica de la sección 3, con
su prioridad y su acción en `docs/plan-mejora.md`. Los hallazgos corregidos y los marcados como
falso positivo se detallan en `docs/rubrica.md`, sección 3.1.

Métricas exportadas: `reportes/sonar/1-inicial/` (primer análisis) y `reportes/sonar/2-despues/`
(tras las correcciones).
