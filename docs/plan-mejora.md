# Plan de mejora continua
20 de septiembre de 2026

## 1. Acciones de mejora

| # | Mejora propuesta | Acción concreta | Indicador de éxito | Meta | Prioridad | Responsable | Fecha límite |
|---|---|---|---|---|---|---|---|
| 1 | Cerrar la sincronización pendiente (KAN-22 a KAN-25) | Enviar al servidor las tomas registradas sin red en cuanto vuelva la conexión, con reintentos | Historias KAN-22 a KAN-25 cerradas | 4 de 4 | Alta | Luis Yael Camberos y Saúl Hernández | 9 oct 2026 |
| 2 | Probar la integración en cada push, no a mano al final | Automatizar el flujo de extremo a extremo en el pipeline: `docker compose up` y registro, login, roles y refresh de los 4 roles | Flujos de extremo a extremo automatizados por ejecución | 4 de 4 roles | Alta | Luis Yael Camberos | 2 oct 2026 |
| 3 | Medir la cobertura de toda la app, no solo de `auth` | Extender JaCoCo a todos los paquetes y escribir pruebas de los repositorios y del cálculo de horarios | Cobertura de líneas de la app completa | 70 % o más | Alta | Daniela Prudencio | 2 oct 2026 |
| 4 | Revocar el acceso antes de que caduque el token | Guardar una versión de token por usuario y rechazar los tokens con versión vieja al cambiar el rol o cerrar sesión | Minutos que sobrevive un token tras revocar | 0 | Alta | Saúl Hernández | 9 oct 2026 |
| 5 | Bajar la deuda de Android Lint y actualizar dependencias | Actualizar AndroidX y Material, borrar los 14 recursos sin usar y corregir `SetTextI18n` y overdraw | Code smells del proyecto Android en SonarQube | 24 o menos (la mitad de 48) | Media | Marco García | 2 oct 2026 |
| 6 | Probar el login en un dispositivo real, no solo en la JVM | Escribir pruebas instrumentadas de login y registro y correrlas en el emulador del pipeline | Pruebas instrumentadas en verde en CI | Al menos 6 | Media | Michel García González | 9 oct 2026 |
| 7 | Fijar las acciones del pipeline por SHA | Sustituir las referencias por versión mayor por el SHA del commit y activar Dependabot | Acciones de terceros fijadas por SHA | 100 % | Media | Saúl Hernández | 16 oct 2026 |
| 8 | Mantener el tablero al día e integrar seguido | Revisar Jira los martes y viernes y que cada integrante integre a la rama principal al menos una vez por semana | Historias vencidas sin actualizar / integraciones por semana | 0 vencidas y 6 integraciones | Media | Dalia Alejandra y Saúl Hernández | 25 sep 2026 (revisión) y 16 oct 2026 (integración) |

## 2. Propuestas de innovación

| Innovación | Acción concreta | Indicador de éxito | Prioridad |
|---|---|---|---|
| Aviso al cuidador por toma omitida | Cuando una toma pasa a omitida, la API envía una notificación push al cuidador vinculado | Minutos entre la omisión y el aviso al cuidador | Alta |
| Lectura de la receta con la cámara | Fotografiar la receta y proponer medicamento, dosis y horario con reconocimiento de texto en el dispositivo | Campos correctos sin corrección manual sobre 20 recetas de prueba | Media |
| Recordatorios por voz | La alerta dice en voz alta el nombre del medicamento y la dosis con la síntesis de voz del sistema | Tomas confirmadas desde la alerta por usuarios con baja visión | Media |
| Reporte de cumplimiento para el médico | Generar un PDF con las tomas y omisiones del mes para llevarlo a la consulta | Reporte generado en menos de 5 segundos con 90 días de historial | Baja |

Las cuatro salen de necesidades vistas en el proyecto: el cuidador hoy tiene que abrir la app
para enterarse, la captura manual es donde más se equivocan los adultos mayores, y el historial
ya está en Room pero no sale de la app.

## 3. Seguimiento
En las revisiones de Jira de los martes y viernes se revisa también el avance de este plan. Cada
acción se da por cerrada cuando su indicador alcanza la meta. Las acciones 2, 3, 5, 6 y 7 se
comprueban directamente en el reporte del pipeline y en el tablero de SonarQube.
