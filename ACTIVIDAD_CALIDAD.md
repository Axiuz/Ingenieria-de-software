# Actividad 4 — Unit Testing, Code Review y Análisis Estático

**Alumno:** Saúl Benjamín Hernández Torres
**Entrega:** individual
**Repositorio:** _(pendiente: URL de GitHub)_
**Rama:** `actividad-calidad`
**Último commit:** _(pendiente)_
**Resultado final → BUILD SUCCESS:** _(pendiente)_

---

## 1. Contexto

Proyecto Java 17 con Maven Wrapper y JUnit 5. La clase bajo prueba es
`ParkingFeeCalculator.calculateFee(int minutes, boolean lostTicket)`, que aplica las
siguientes reglas de negocio:

| Situación | Cobro |
| --- | --- |
| 0–15 min | $0 |
| 16–60 min | $20 |
| Más de 60 min | $20 + $15 por cada hora adicional **iniciada** |
| Tope del cobro normal | $80 |
| Boleto perdido | $150, sin importar el tiempo |
| Minutos negativos | `IllegalArgumentException` |

---

## 2. Pruebas unitarias (Partes 1 a 8)

`ParkingFeeCalculatorTest` quedó con **11 pruebas**: las 5 originales más 6 nuevas.
Todas siguen la estructura Arrange / Act / Assert y un nombre que describe la regla
que verifican.

### Pruebas originales

| # | Prueba | Entrada | Esperado | Regla que cubre |
| --- | --- | --- | --- | --- |
| 1 | `fifteenMinutesShouldBeFree` | 15, false | 0 | Último minuto gratuito |
| 2 | `sixtyOneMinutesShouldChargeOneStartedAdditionalHour` | 61, false | 35 | Hora adicional iniciada |
| 3 | `normalFeeShouldNotBeGreaterThanEighty` | 600, false | 80 | Tope de $80 |
| 4 | `lostTicketShouldCostOneHundredFifty` | 10, true | 150 | Boleto perdido |
| 5 | `negativeMinutesShouldBeRejected` | -1, false | excepción | Entrada inválida |

### Pruebas nuevas

| # | Prueba | Entrada | Esperado | Por qué la agregué |
| --- | --- | --- | --- | --- |
| 6 | `zeroMinutesShouldBeFree` | 0, false | 0 | Frontera inferior del rango gratuito |
| 7 | `sixteenMinutesShouldChargeFlatRate` | 16, false | 20 | Primer minuto que ya se cobra (frontera 15/16) |
| 8 | `sixtyMinutesShouldStillChargeOnlyFlatRate` | 60, false | 20 | Último minuto de tarifa plana (frontera 60/61) |
| 9 | `partialAdditionalHourShouldBeChargedAsAWholeHour` | 121, false | 50 | Confirma que una hora iniciada se cobra completa |
| 10 | `feeShouldBeCappedAtEightyOnTheFirstMinuteThatExceedsTheCap` | 241, false | 80 | El primer caso donde el tope realmente recorta (95 → 80) |
| 11 | `lostTicketShouldTakePrecedenceOverNegativeMinutes` | -30, true | 150 | Combina boleto perdido con un dato inválido |

### Reto de TDD

`ParkingReservationPolicy.canReserve(String plate, int hoursAhead)` se escribió
**después** de sus pruebas (`ParkingReservationPolicyTest`), en ciclos rojo → verde →
refactor. Regla: se acepta la reserva si la placa no es nula ni está en blanco y la
anticipación está entre 1 y 72 horas.

### Resultado de la ejecución

```
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 3. Code review manual (Parte 9)

Revisión a mano, antes de correr ninguna herramienta.

| # | Archivo | Línea aprox. | Observación | Severidad |
| --- | --- | --- | --- | --- |
| 1 | `LegacyParkingReceipt.java` | comparación de placa vacía | Compara `String` con `==` en lugar de `equals()` / `isEmpty()`; funciona solo por el caché de literales, no con cadenas construidas en tiempo de ejecución | Alta |
| 2 | `LegacyParkingReceipt.java` | variable `ticketCounter` | Variable local declarada y nunca usada; confunde al que lee | Media |
| 3 | `LegacyParkingReceipt.java` | `if (free == true)` | Comparar un booleano contra el literal `true` es redundante | Baja |
| 4 | `LegacyParkingReceipt.java` | armado del texto | El literal `"PARKING"` y el separador `" - "` se repiten; si cambia el formato hay que tocar varios lugares | Media |
| 5 | `ParkingFeeCalculator.java` | todo el método | Números mágicos (15, 60, 20, 15, 80, 150) sin nombre; el lector no sabe qué significa cada uno sin ir al enunciado | Media |
| 6 | `LegacyParkingReceipt.java` | cuatro `if` de validación | Las cuatro validaciones que devuelven `"ERROR"` están dispersas y podrían agruparse | Baja |

---

## 4. Análisis con SonarQube for IDE (Parte 9)

> **Borrador.** Esta tabla se reemplaza con lo que realmente reporte el panel
> *Problems* de VS Code (ver Fase C de la actividad).

| # | Archivo : línea | Regla | Mensaje de Sonar | Qué entendí | ¿Estoy de acuerdo? |
| --- | --- | --- | --- | --- | --- |
| 1 | `LegacyParkingReceipt.java` | _(pendiente)_ | _(pendiente)_ | _(pendiente)_ | _(pendiente)_ |
| 2 | `LegacyParkingReceipt.java` | _(pendiente)_ | _(pendiente)_ | _(pendiente)_ | _(pendiente)_ |
| 3 | `ParkingFeeCalculator.java` | _(pendiente)_ | _(pendiente)_ | _(pendiente)_ | _(pendiente)_ |

---

## 5. Correcciones realizadas (Parte 10)

### 5.1 `ParkingFeeCalculator.java` — eliminar números mágicos

Cada número suelto se convirtió en una constante con nombre
(`FREE_LIMIT_MINUTES`, `FLAT_RATE_LIMIT_MINUTES`, `MINUTES_PER_HOUR`, `FLAT_RATE`,
`ADDITIONAL_HOUR_RATE`, `MAX_NORMAL_FEE`, `LOST_TICKET_FEE`). El cálculo es el mismo;
lo que cambia es que ahora el código se lee como la regla de negocio y un cambio de
tarifa se hace en un solo lugar.

### 5.2 `LegacyParkingReceipt.java` — comparación de cadenas y limpieza

- `plate == ""` se sustituyó por `plate.isEmpty()`, que es la comparación correcta de
  contenido y no depende del caché de literales.
- Se eliminó la variable local sin uso.
- `if (free == true)` se simplificó a usar el booleano directamente.
- El literal `"PARKING"` pasó a ser la constante `LABEL` y el texto del recibo se arma
  en un solo `return`.

No se modificó el orden entre el boleto perdido y la validación de minutos negativos:
la regla de negocio dice que un boleto perdido cobra $150 sin importar el tiempo, y la
prueba `lostTicketShouldTakePrecedenceOverNegativeMinutes` lo fija así.

Después de ambas correcciones: `Tests run: 21, Failures: 0` → **BUILD SUCCESS**.

---

## 6. Preguntas de reflexión (P1–P8)

> Las preguntas P1–P8 se revisaron una por una con el alumno.

**P1. ¿Qué aporta una prueba unitaria que no aporta probar la aplicación a mano?**

La prueba queda escrita y se vuelve a correr sola cada vez. Probar a mano el
estacionamiento significa capturar 241 minutos, ver el total y confiar en que lo leí
bien; la prueba deja el caso guardado y lo vuelve a verificar en cada build, así que un
cambio futuro en la tarifa se detecta al instante y no cuando ya está en producción.

**P2. ¿Por qué elegiste esos casos de frontera?**

Porque los errores casi siempre viven en el límite, no en medio del rango. Entre 15 y
16 minutos cambia el cobro de $0 a $20, y entre 60 y 61 aparece la primera hora
adicional: si alguien escribe `<` donde iba `<=`, esas dos parejas de pruebas son las
que lo delatan. Un caso de 30 minutos no distingue entre la versión correcta y la
equivocada.

**P3. ¿Qué diferencia notaste entre escribir pruebas para código que ya existía y hacer TDD?**

En `ParkingFeeCalculator` el código ya estaba, así que leí la implementación y mis
pruebas terminaron parecidas a lo que el código ya hacía. En
`ParkingReservationPolicy` escribí primero la prueba de 72 horas, la vi fallar en rojo
y después la hice pasar: ahí pensé en la regla, no en el `if`. TDD me obligó a definir
qué debía pasar antes de tener dónde copiarlo.

**P4. ¿Qué problema encontró Sonar que tú no habías anotado en tu code review?**

_(Pendiente: se llena con lo que el alumno observe realmente en VS Code.)_

**P5. ¿Qué observación tuya no reportó Sonar?**

_(Pendiente: se llena con lo que el alumno observe realmente en VS Code.)_

**P6. ¿El análisis estático sustituye a las pruebas unitarias?**

No, revisan cosas distintas. Sonar ve la forma del código: números mágicos, una
comparación de cadenas con `==`, una variable sin usar. Lo que no puede saber es si el
estacionamiento debe cobrar $35 o $50 a los 121 minutos, porque esa es una regla de
negocio que solo está en el enunciado. Un método puede estar impecable para Sonar y
cobrar mal.

**P7. ¿Qué corrección te pareció más importante y por qué?**

La de `plate == ""`. Las demás son de legibilidad, pero esa es un error real: compara
referencias en vez de contenido, y hoy funciona solo porque las pruebas usan literales
que Java tiene en caché. En cuanto la placa venga de un formulario o de la base de
datos, la comparación falla y el recibo se emite con una placa vacía.

**P8. ¿Qué te llevas de la actividad para tus próximos proyectos?**

Que las pruebas y el análisis estático son baratos si se ponen desde el principio.
Nombrar las constantes me costó cinco minutos y dejó el método legible, y tener las 11
pruebas verdes me dejó refactorizar sin miedo: cambié los dos archivos y el build me
confirmó en segundos que no había roto nada.

---

## 7. Cómo reproducir

```bash
./mvnw clean test        # Linux / macOS
.\mvnw.cmd clean test    # Windows
```
