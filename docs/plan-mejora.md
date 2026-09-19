# Plan de mejora
18 de septiembre de 2026

## 1. Acciones de mejora

| # | Problema detectado | Acción | Responsable | Métrica | Meta | Fecha límite |
|---|---|---|---|---|---|---|
| 1 | Pipeline de CI/CD sin ejecutar en GitHub | Hacer el primer push y corregir lo que falle hasta tener el pipeline en verde | Saúl Hernández | Ejecuciones del pipeline en verde | 1 ejecución completa en verde | 22 sep 2026 |
| 2 | La prueba de extremo a extremo con MySQL y emulador se hizo a mano y al final | Automatizarla en el pipeline: levantar la pila con `docker compose` y ejecutar el flujo de registro, login, roles y refresh con cada push | Luis Yael Camberos Fernández | Flujos de extremo a extremo automatizados | Los 4 roles en cada ejecución | 2 oct 2026 |
| 3 | La cobertura solo se mide en el paquete auth; el resto de la app no tiene métrica | Extender el reporte de JaCoCo a toda la app y escribir pruebas de los repositorios y del cálculo de horarios | Daniela Prudencio | Cobertura de líneas de la app completa | 70 % o más | 2 oct 2026 |
| 4 | Falta la sincronización con el servidor (KAN-22 a KAN-25) | Enviar al servidor las tomas registradas sin red cuando vuelva la conexión | Luis Yael Camberos Fernández y Saúl Hernández | Historias KAN-22 a KAN-25 cerradas | 4 de 4 | 9 oct 2026 |
| 5 | 20 dependencias de Android desactualizadas y 51 avisos de Android Lint | Actualizar AndroidX y Material y corregir los avisos | Marco García | Avisos de Android Lint | 25 o menos (la mitad) | 2 oct 2026 |
| 6 | Pantallas de login sin pruebas en dispositivo | Escribir pruebas instrumentadas de login y registro y correrlas en el emulador del pipeline | Michel García González | Pruebas instrumentadas en verde en CI | Al menos 6 | 9 oct 2026 |
| 7 | Tablero de Jira desactualizado frente al código | Revisar el tablero en equipo dos veces por semana y mover cada historia según su estado real | Dalia Alejandra | Historias vencidas sin actualizar | 0 en cada revisión | 25 sep 2026 |
| 8 | Código integrado al final del ciclo | Integrar a la rama principal al menos una vez por semana por integrante | Saúl Hernández | Integraciones por semana | 6 por semana | 16 oct 2026 |

## 2. Propuestas de innovación
- **Aviso al cuidador por toma omitida.** Si una toma queda omitida, se notifica al cuidador en su teléfono. Así puede intervenir a tiempo sin tener que revisar la app.
- **Lectura de la receta con la cámara.** El usuario fotografía la receta y la app propone el medicamento, la dosis y el horario. Evita errores al capturarlos a mano, que es donde más se equivocan los adultos mayores.
- **Recordatorios por voz.** La alerta dice en voz alta el nombre del medicamento y la dosis. Sirve a quien ve mal la pantalla.
- **Reporte de cumplimiento para el médico.** Un PDF con las tomas y omisiones del mes, para llevarlo a la consulta.

## 3. Seguimiento
En las revisiones de Jira de los martes y viernes se revisa también el avance de este plan. Cada acción se da por cerrada cuando su métrica alcanza la meta. Las acciones 1, 5 y 6 se comprueban en el reporte del pipeline.
