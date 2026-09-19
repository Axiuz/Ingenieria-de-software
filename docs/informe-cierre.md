# Informe de cierre del proyecto
18 de septiembre de 2026

## 1. Resumen
TuPastilla es una app Android, con una API, para que adultos mayores, pacientes autónomos y cuidadores lleven el control de sus tomas de medicamentos. Cinco de las seis épicas tienen código en el repositorio. La API tiene autenticación JWT con 4 roles y 82 pruebas en verde. La app funciona sin red gracias a Room. Falta la sincronización con el servidor (KAN-22 a KAN-25), que vence entre el 19 y el 24 de septiembre.

## 2. Cronograma planeado vs real
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

## 3. Detalle por historia
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

## 4. Desviaciones y causas
- **Historias tempranas sin código a tiempo.** KAN-4, KAN-5, KAN-9, KAN-10, KAN-11, KAN-13, KAN-14 y KAN-16 vencían entre el 3 y el 12 de septiembre. En el repositorio no hay código de TuPastilla antes del 16 de septiembre; hasta el 11 solo estaba la actividad de calidad anterior (proyecto de estacionamiento).
- **Desarrollo concentrado.** Todo el código se creó entre el 16 de septiembre a las 21:40 y el 18 a las 16:13, en lugar de repartirse a lo largo del mes.
- **Dependencias entre épicas.** La autenticación y el CI/CD entraron al final porque necesitaban la app base.
- **Integración probada tarde.** La primera prueba con MySQL real y la app en un emulador se hizo al final, el 18 de septiembre, y encontró cinco defectos que las pruebas unitarias no detectaban (detalle en `docs/calidad.md`, sección 6). Todos se corrigieron el mismo día.
- **Pipeline sin ejecutar en GitHub.** Aún no hay push. Los pasos de los jobs `api`, `android` y `seguridad` se ejecutaron en local (contenedor `node:24` limpio, Gradle desde cero, gitleaks, Trivy y ZAP en Docker) y pasan. CodeQL y el despliegue a GHCR solo pueden correr en GitHub.

## 5. Entregables
- App Android con gestión de medicamentos, recordatorios y alertas, registro de tomas, historial, monitoreo para cuidadores y login con sesión cifrada.
- API Express con registro, login, refresh con rotación, logout, 4 roles y administración de roles.
- Pruebas: 82 en la API (100 % de líneas y 92.7 % de ramas) y 55 en la app (97.7 % de líneas y 97.0 % de ramas en el paquete auth).
- Prueba de extremo a extremo: API en Docker con MySQL 8.4 y la app en un emulador, con los cuatro roles.
- Seguridad: 9 vulnerabilidades de dependencias y 4 de la imagen Docker corregidas; cerradas una escalada de rol y una fuga de datos entre cuentas en el cliente. gitleaks, Trivy y ZAP sin hallazgos. Detalle en `docs/seguridad.md`.
- Calidad: métricas y deuda técnica en `docs/calidad.md`.
- Infraestructura: Dockerfile, `docker-compose.yml` y pipeline `.github/workflows/ci-cd.yml`.

## 6. Lecciones aprendidas
- Una historia con fecha necesita avances visibles en el repositorio antes de su vencimiento, no solo al cierre.
- Lo transversal (autenticación, CI/CD) conviene levantarlo pronto, aunque sea en una versión mínima.
- El entorno local debe reproducir el de producción (Docker y MySQL) desde el primer día: la prueba con base real y emulador encontró en una hora cinco defectos que 137 pruebas unitarias no detectaban.
- El tablero de Jira debe reflejar el estado real: sin actualizarlo, las fechas dejan de servir para decidir.
