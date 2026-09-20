# Informe de cierre del proyecto
20 de septiembre de 2026

## 1. Resumen
TuPastilla es una app Android, con una API, para que adultos mayores, pacientes autónomos y cuidadores lleven el control de sus tomas de medicamentos. Cinco de las seis épicas tienen código en el repositorio. La API tiene autenticación JWT con 4 roles y 87 pruebas en verde. La app funciona sin red gracias a Room. Falta la sincronización con el servidor (KAN-22 a KAN-25), que vence entre el 19 y el 24 de septiembre.

## 2. Planeado contra ejecutado

| Elemento | Planeado | Ejecutado | Diferencia / causa |
|---|---|---|---|
| Módulo principal | Cuentas y roles: registro, login y consulta de usuarios | Registro, login, refresh con rotación, logout, `/api/me`, `/api/permisos`, lista de usuarios y cambio de rol | Se amplió: el refresh rotatorio y el bloqueo por intentos salieron de las pruebas de ataque |
| Duración del módulo de seguridad | 1 día (23 sep, KAN-26) | 2 días (18 y 19 sep) | Se adelantó 5 días, pero costó el doble: la prueba de extremo a extremo con MySQL real destapó 5 defectos |
| Autenticación | JWT con dos roles | JWT HS256 con 4 roles, issuer y audiencia propios, secreto solo por variable de entorno | Los roles del dominio (autónomo, supervisado, cuidador) ya existían en la app; se añadió ADMIN |
| Pruebas unitarias | 80 % de cobertura | API 100 % de líneas (87 pruebas); app 97.7 % en el paquete auth (55 pruebas) | Cumplido con margen. La cobertura del resto de la app sigue sin medirse |
| Pipeline de CI/CD | Pruebas, construcción y despliegue | 4 jobs: API, Android, seguridad y despliegue a GHCR y GitHub Releases | El job de seguridad (gitleaks, CodeQL, Trivy, ZAP) no estaba planeado y se añadió |
| Despliegue | Día 4 del sprint | Día 6: primero falló por versiones de acciones, después por la rama del disparador | El despliegue exigía una rama `main` que no existe en el repositorio |
| Análisis de calidad | SonarQube al final | SonarQube Community local sobre los dos proyectos, con métricas antes y después | Primero se usó solo ESLint y Android Lint; Sonar se integró en la última semana |
| Pruebas de seguridad | OWASP ZAP contra la API | ZAP baseline, ZAP con contrato OpenAPI y ZAP como proxy del emulador, más MobSF sobre el APK | Se amplió: una app móvil necesita ver el tráfico real, no solo el escaneo del servidor |
| Sincronización de datos | 19 al 24 sep (KAN-22 a KAN-25) | Pendiente | En plazo al cierre de esta entrega |

## 3. Cronograma planeado vs real
Las fechas planeadas son las de vencimiento de Jira. Las reales son la fecha de creación de los archivos de cada módulo en el repositorio.

| Épica | Planeado | Real en el repositorio | Desviación |
|---|---|---|---|
| Gestión de medicamentos y horarios (KAN-31) | 2 al 10 sep | 16 sep, 23:10 a 23:42 | +6 días |
| Recordatorios y alertas inteligentes (KAN-32) | 3 al 25 sep | 16 sep, 23:18 a 23:29 | 9 días antes; KAN-9 y KAN-10 (prioridad máxima) +11 y +13 |
| Registro y seguimiento de tomas (KAN-33) | 4 al 23 sep | 16 sep 23:19 a 17 sep 07:26 | 6 días antes; KAN-11 y KAN-13 +5 y +4 |
| Monitoreo para cuidadores | 16 al 22 sep (historias) | 17 sep, 00:20 | en plazo |
| Seguridad y protección de información (KAN-26) | 23 sep | 18 sep, 15:18 a 16:13 | 5 días antes |
| Sincronización y servicios offline (KAN-22 a KAN-25) | 19 al 24 sep | Parcial: funciona sin red con Room y reprograma alarmas al reiniciar (16 sep). Falta la sincronización con el servidor | pendiente, en plazo |

Solo una épica, Gestión de medicamentos, terminó después de su cierre planeado. Las demás se adelantaron o van en plazo. El retraso real está en las historias de principios de septiembre: vencieron entre el 3 y el 12 de septiembre y el código de su épica apareció el 16.

## 4. Detalle por historia
| Historia | Tipo de usuario | Épica | Vence | Prioridad | Responsable | Código de la épica en el repositorio | Días |
|---|---|---|---|---|---|---|---|
| KAN-4 | Adulto mayor | Gestión de medicamentos y horarios | 6 sep | Alta | Dalia Alejandra | 16 sep | +10 |
| KAN-5 | Adulto mayor | Gestión de medicamentos y horarios | 6 sep | Alta | Marco García | 16 sep | +10 |
| KAN-6 | Adulto mayor | Gestión de medicamentos y horarios | 16 sep | Media | Daniela Prudencio | 16 sep | 0 |
| KAN-7 | Adulto mayor | Gestión de medicamentos y horarios | 16 sep | Media | Michel García González | 16 sep | 0 |
| KAN-8 | Adulto mayor | Recordatorios y alertas inteligentes | 18 sep | Media | Marco García | 16 sep | −2 |
| KAN-9 | Adulto mayor | Recordatorios y alertas inteligentes | 5 sep | Máxima | Luis Yael Camberos Fernández | 16 sep | +11 |
| KAN-10 | Adulto mayor | Recordatorios y alertas inteligentes | 3 sep | Máxima | Daniela Prudencio | 16 sep | +13 |
| KAN-11 | Adulto mayor | Registro y seguimiento de tomas | 11 sep | Alta | Michel García González | 16 sep | +5 |
| KAN-12 | Adulto mayor | Registro y seguimiento de tomas | 20 sep | Media | Marco García | 17 sep | −3 |
| KAN-13 | Adulto mayor | Registro y seguimiento de tomas | 12 sep | Alta | Daniela Prudencio | 16 sep | +4 |
| KAN-14 | Adulto mayor | Recordatorios y alertas inteligentes | 11 sep | Alta | Marco García | 16 sep | +5 |
| KAN-15 | Adulto mayor | Recordatorios y alertas inteligentes | 18 sep | Media | Dalia Alejandra | 16 sep | −2 |
| KAN-16 | Familiar/Cuidador | Gestión de medicamentos y horarios | 12 sep | Alta | Dalia Alejandra | 17 sep | +5 |
| KAN-17 | Familiar/Cuidador | Monitoreo para cuidadores | 16 sep | Alta | Michel García González | 17 sep | +1 |
| KAN-18 | Familiar/Cuidador | Monitoreo para cuidadores | 16 sep | Alta | Daniela Prudencio | 17 sep | +1 |
| KAN-19 | Familiar/Cuidador | Monitoreo para cuidadores | 17 sep | Alta | Michel García González | 17 sep | 0 |
| KAN-20 | Familiar/Cuidador | Monitoreo para cuidadores | 21 sep | Media | Daniela Prudencio | 17 sep | −4 |
| KAN-21 | Familiar/Cuidador | Monitoreo para cuidadores | 22 sep | Media | Dalia Alejandra | 17 sep | −5 |
| KAN-22 | Sistema | Sincronización y servicios offline | 19 sep | Alta | Luis Yael Camberos Fernández | — | en plazo |
| KAN-23 | Sistema | Sincronización y servicios offline | 20 sep | Alta | Luis Yael Camberos Fernández | — | en plazo |
| KAN-24 | Sistema | Sincronización y servicios offline | 21 sep | Alta | Saúl Hernández | — | en plazo |
| KAN-25 | Sistema | Sincronización y servicios offline | 24 sep | Media | Saúl Hernández | — | en plazo |
| KAN-26 | Usuario | Seguridad y protección de información | 23 sep | Media | Saúl Hernández | 18 sep | −5 |
| KAN-28 | Paciente autónomo | Gestión de medicamentos y horarios | 20 sep | Media | Saúl Hernández | — | en plazo |
| KAN-29 | Paciente autónomo | Recordatorios y alertas inteligentes | 22 sep | Media | Daniela Prudencio | — | en plazo |
| KAN-30 | Paciente autónomo | Registro y seguimiento de tomas | 22 sep | Media | Daniela Prudencio | — | en plazo |

*Días: diferencia entre la fecha en que aparece en el repositorio el código de la épica y el vencimiento de la historia (positivo = después). No mide el estado de la historia en Jira. KAN-27 se eliminó del tablero a propósito.*

## 5. Desviaciones y causas
- **Historias tempranas sin código a tiempo.** KAN-4, KAN-5, KAN-9, KAN-10, KAN-11, KAN-13, KAN-14 y KAN-16 vencían entre el 3 y el 12 de septiembre. En el repositorio no hay código de TuPastilla antes del 16 de septiembre; hasta el 11 solo estaba la actividad de calidad anterior (proyecto de estacionamiento).
- **Desarrollo concentrado.** Todo el código se creó entre el 16 de septiembre a las 21:40 y el 18 a las 16:13, en lugar de repartirse a lo largo del mes.
- **Dependencias entre épicas.** La autenticación y el CI/CD entraron al final porque necesitaban la app base.
- **Integración probada tarde.** La primera prueba con MySQL real y la app en un emulador se hizo al final, el 18 de septiembre, y encontró cinco defectos que las pruebas unitarias no detectaban (detalle en `docs/calidad.md`, sección 6). Todos se corrigieron el mismo día.
- **Pipeline con dos intentos fallidos antes del primero en verde.** El primer push falló por versiones de acciones inexistentes; el segundo pasó los tres jobs de pruebas pero saltó el despliegue, porque la condición exigía la rama `main`. Se corrigió apuntando el disparador y la condición a `Proyecto-TuPastilla`.

## 6. Entregables
- App Android con gestión de medicamentos, recordatorios y alertas, registro de tomas, historial, monitoreo para cuidadores y login con sesión cifrada.
- API Express con registro, login, refresh con rotación, logout, 4 roles y administración de roles.
- Pruebas: 87 en la API (100 % de líneas y 92.7 % de ramas) y 55 en la app (97.7 % de líneas y 97.0 % de ramas en el paquete auth).
- Prueba de extremo a extremo: API en Docker con MySQL 8.4 y la app en un emulador, con los cuatro roles.
- Seguridad: 9 vulnerabilidades de dependencias y 4 de la imagen Docker corregidas; cerradas una escalada de rol y una fuga de datos entre cuentas en el cliente. Detalle en `docs/seguridad.md`.
- Análisis de esta entrega: SonarQube, OWASP ZAP (API, contrato OpenAPI y tráfico del emulador) y MobSF, con los reportes en `reportes/` y su lectura en `docs/rubrica.md`.
- Calidad: métricas y deuda técnica en `docs/calidad.md`.
- Infraestructura: Dockerfile, `docker-compose.yml` y pipeline `.github/workflows/ci-cd.yml`.

## 7. Resultados de las pruebas de seguridad y calidad

| Herramienta | Alcance | Resultado |
|---|---|---|
| Vitest + JaCoCo | 87 pruebas de la API y 55 de la app | 0 fallos; 100 % de líneas en la API y 97.7 % en el paquete auth |
| SonarQube Community | Código de la API y de la app | API: 0 bugs, 0 vulnerabilidades, 0 code smells. App: de 4 vulnerabilidades y 53 code smells a 1 y 48; deuda de 286 a 250 minutos |
| OWASP ZAP baseline y API scan | API en Docker, 13 endpoints con token de ADMIN | 1 aviso corregido (contenido cacheable); segundo escaneo sin él; 118 reglas activas pasadas |
| OWASP ZAP como proxy | Tráfico real del emulador | 3 alertas informativas; login incorrecto responde 401 genérico |
| MobSF | APK de release | Puntuación 61/100; 2 hallazgos altos esperados o aceptados, 3 falsos positivos justificados |
| gitleaks, Trivy, CodeQL, pnpm audit | Historial, imagen y dependencias | 0 secretos, 0 vulnerabilidades HIGH o CRITICAL con parche |

Detalle e interpretación de cada hallazgo en `docs/rubrica.md` y `docs/seguridad.md`.

## 8. Lecciones aprendidas

**Decisiones de arquitectura.** Que la UI dependa solo de la interfaz `MedicamentoRepository`
permitió cambiar el origen de los datos sin tocar los fragments. En cambio, guardar los datos
únicamente en Room dejó al descubierto el problema de las cuentas compartidas en un mismo
teléfono (V-07): la arquitectura offline se decidió antes de que existiera el login.

**Autenticación y roles.** Poner el rol en el JWT firmado y no en preferencias locales fue lo
que cerró la escalada de privilegios (V-02). El precio: un cambio de rol no surte efecto hasta
que caduca el access token, y hubo que revocar los refresh para forzar un login nuevo.

**Automatizar pruebas y despliegues.** Lo que más costó no fue escribir las pruebas, sino el
entorno: coordinar JaCoCo con lint en el mismo comando, levantar MySQL con healthcheck antes de
ZAP y acertar con la rama del disparador. Tres intentos hasta tener el pipeline completo en verde.

**Utilidad de las pruebas unitarias.** Las 137 pruebas unitarias atraparon los casos de ataque
(algoritmo `none`, token de otro secreto, mass assignment), pero no vieron cinco defectos de
integración: los encontró en una hora la primera prueba con MySQL real y la app en el emulador.

**Defectos detectados por el análisis estático y de seguridad.** SonarQube señaló el tráfico en
claro implícito del manifiesto y literales duplicados; ZAP, respuestas con tokens que podían
quedar en caché; Trivy, cuatro vulnerabilidades altas en el npm de la imagen base; MobSF, la
copia de seguridad de los datos de la app. Ninguna la había visto el equipo leyendo el código.

**Qué cambiaríamos si empezáramos de nuevo.** Levantar el pipeline y el entorno con Docker el
primer día, aunque solo corra una prueba; escribir el login antes que las pantallas, porque
condiciona el modelo de datos; y actualizar el tablero de Jira al mismo tiempo que el código.

## 9. Conclusión

El módulo de cuentas y roles quedó completo, probado y desplegado de forma automática. Lo que
queda abierto —sincronización con el servidor, cobertura del resto de la app y la deuda de
Android Lint— está recogido con responsable, indicador y fecha en `docs/plan-mejora.md`.

## 10. Acuerdos de trabajo que salieron de este cierre
- Una historia con fecha necesita avances visibles en el repositorio antes de su vencimiento, no solo al cierre.
- Lo transversal (autenticación, CI/CD) conviene levantarlo pronto, aunque sea en una versión mínima.
- El entorno local debe reproducir el de producción (Docker y MySQL) desde el primer día: la prueba con base real y emulador encontró en una hora cinco defectos que 137 pruebas unitarias no detectaban.
- El tablero de Jira debe reflejar el estado real: sin actualizarlo, las fechas dejan de servir para decidir.
