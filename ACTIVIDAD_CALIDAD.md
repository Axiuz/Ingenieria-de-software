# Actividad de calidad de software

Actividad 4 — Unit Testing, Code Review y SonarQube for IDE · Ingeniería de Software, Tecmilenio

## Integrantes

- Nombre: Saúl Benjamín Hernández Torres (entrega individual)

## Repositorio

- Repositorio: https://github.com/Axiuz/Ingenieria-de-software
- Rama: `actividad-calidad`
- Último commit: `b061ae1`

---

## Parte 2 — Unit testing

Clase bajo prueba: `ParkingFeeCalculator.calculateFee(int minutes, boolean lostTicket)`.
Reglas: 0–15 min → $0 · 16–60 min → $20 · más de 60 min → $20 + $15 por hora adicional
**iniciada** · tope normal $80 · boleto perdido $150 · minutos negativos →
`IllegalArgumentException`.

### Pruebas agregadas

Agregué **seis** pruebas nuevas (se pedían cinco) sin tocar las cinco originales.
Ninguna repite un caso existente cambiándole el número.

| Prueba | Entrada | Esperado | Qué comportamiento agrega |
| --- | --- | --- | --- |
| `zeroMinutesShouldBeFree` | 0, false | 0 | Frontera inferior del rango gratuito |
| `sixteenMinutesShouldChargeFlatRate` | 16, false | 20 | **Frontera 15/16**: primer minuto que ya se cobra |
| `sixtyMinutesShouldStillChargeOnlyFlatRate` | 60, false | 20 | **Frontera 60/61**: último minuto de tarifa plana |
| `partialAdditionalHourShouldBeChargedAsAWholeHour` | 121, false | 50 | Una hora apenas iniciada se cobra completa |
| `feeShouldBeCappedAtEightyOnTheFirstMinuteThatExceedsTheCap` | 241, false | 80 | Complementa el tope: primer caso donde **sí recorta** (95 → 80) |
| `lostTicketShouldTakePrecedenceOverNegativeMinutes` | -30, true | 150 | Combina la condición especial con un dato inválido |

Total: **11 pruebas** en `ParkingFeeCalculatorTest`.

### P1. ¿Por qué probar muchos valores de una misma región no necesariamente mejora mucho una suite de pruebas?

Porque todos los valores de una misma región recorren **la misma rama del código**. Si
pruebo 20, 30, 40 y 50 minutos, las cuatro entran al mismo `if (minutes <= 60)` y pasan
o fallan juntas: la segunda, la tercera y la cuarta no aportan información nueva. Si
alguien escribe `<` donde iba `<=`, esas cuatro siguen en verde y el error pasa
desapercibido; una prueba en 60 y otra en 61 lo detectan de inmediato. Por eso gasté mis
seis pruebas en los bordes y en combinaciones, no en más puntos del mismo tramo: las
pruebas redundantes tardan más, dan falsa sensación de cobertura y hay que mantenerlas
igual.

### P2. Menciona dos fronteras importantes que identificaste y explica por qué vale la pena probar valores cercanos a ellas.

**Los 15 minutos** (gratis contra $20): a los 15 todavía es gratis y a los 16 ya se
cobra. Es donde el cliente pasa de pagar nada a pagar $20, así que un error se nota en
la caja. La cubrí con `fifteenMinutesShouldBeFree` (ya existía) y
`sixteenMinutesShouldChargeFlatRate` (agregada); juntas fijan el `<=`.

**Los 60 minutos** (tarifa plana contra hora adicional): a los 60 son $20 y a los 61 ya
$35, porque la regla dice hora **iniciada**, no cumplida. La cubrí con
`sixtyMinutesShouldStillChargeOnlyFlatRate` y la prueba de 61 que ya existía. Este borde
vale doble: además del `<=` verifica el redondeo hacia arriba, que es donde se colaría
un error si alguien usara división entera en lugar de `Math.ceil`.

En las dos el criterio es el mismo: los errores viven en el límite, no en medio del
rango. Un caso de 30 minutos no distingue la versión correcta de la equivocada.

### P3. ¿Que todas las pruebas estén en verde demuestra que el programa es correcto?

No. Verde solo significa que el programa se comporta como **yo** dije que debía, en **los
casos que yo escribí**. Mis 11 pruebas no dicen nada sobre `Integer.MAX_VALUE` minutos ni
sobre otros casos que no se me ocurrieron. Y lo más importante: si entendí mal la regla
de negocio, escribo la prueba equivocada y el código equivocado, y los dos concuerdan. Si
yo creyera que después de 60 minutos se cobra por hora **cumplida**, mi prueba pediría
$20 a los 61 minutos, el código lo devolvería, todo saldría verde y el estacionamiento
cobraría de menos. Las pruebas verdes demuestran consistencia entre mi código y mis
expectativas, no que mis expectativas sean correctas. Lo que sí me dan es una red de
seguridad ante cambios futuros, que es distinto de una demostración de correctitud.

---

## Code review manual

Revisión hecha a mano, **antes** de instalar o consultar SonarQube for IDE. Estas son las
observaciones a las que llegué yo solo, leyendo el código.

| Archivo | Hallazgo | Tipo | Severidad | Propuesta |
| --- | --- | --- | --- | --- |
| `ParkingFeeCalculator.java` | Números mágicos: 15, 60, 20, 15, 80 y 150 aparecen sueltos en el método; no se sabe cuál es el límite gratuito y cuál el tope sin ir al enunciado | Maintainability | Media | Extraer cada uno a una constante `private static final` con nombre |
| `ParkingFeeCalculatorTest.java` | Las cinco pruebas originales no cubrían ninguna de las dos fronteras (15/16 y 60/61), justo donde se esconden los errores de `<` contra `<=` | Testing | Media | Agregar pruebas a ambos lados de cada frontera (hecho: pruebas 2 y 3 de las agregadas) |
| `LegacyParkingReceipt.java` | `System.out.println("Creating receipt for " + plate)` deja rastro de depuración dentro de la lógica de negocio | Maintainability | Media | Quitarlo o sustituirlo por un `Logger` |
| `LegacyParkingReceipt.java` | `boolean free = fee == 0 ? true : false;` — el ternario no hace nada: `fee == 0` ya **es** el booleano que se quiere guardar | Maintainability | Baja | `boolean free = (fee == 0);` |
| `LegacyParkingReceipt.java` | El literal `"ERROR"` se repite en las cuatro validaciones, y las cuatro están en `if` sueltos y dispersos | Design | Baja | Extraer `"ERROR"` a una constante y agrupar las condiciones relacionadas |

**Lo que se me pasó.** Anoto esto porque es el resultado más útil de la actividad: mi
revisión manual encontró **tres** de los defectos de `LegacyParkingReceipt`, y los tres
eran de los fáciles — el `println` que se ve a simple vista, el ternario redundante y el
literal repetido. Los que **no** vi están en la sección siguiente, y entre ellos está el
único que rompe el programa de verdad.

---

## Análisis con SonarQube for IDE

Extensión **SonarQube for IDE** (SonarSource) en VS Code con el Extension Pack for Java en
*Standard Mode*. Análisis local, sin conectar a Server ni a Cloud. Los archivos se
guardaron con `Ctrl+S` y los avisos se leyeron en el panel *Problems* filtrando los de
origen `sonarqube`.

| Archivo/línea | Regla o mensaje de Sonar | Explicación con mis palabras | ¿Estoy de acuerdo? |
| --- | --- | --- | --- |
| `LegacyParkingReceipt.java` : 14 | `java:S4973` — *Strings and Boxed types should be compared using "equals()".* | El `==` entre objetos pregunta "¿son el mismo objeto en memoria?", no "¿dicen lo mismo?". `plate == ""` solo acierta porque Java reutiliza el literal `""` desde una caché interna; si la placa viene de un `Scanner` o de la base de datos será otro objeto y la condición dará falso aunque el texto esté vacío | **Sí.** Es el único que no es de estilo: hoy pasa las pruebas y aun así está mal, y falla justo cuando reciba datos de verdad |
| `LegacyParkingReceipt.java` : 26 | `java:S106` — *Replace this use of System.out by a logger.* | Escribir directo a `System.out` desde la lógica de negocio no se puede apagar ni filtrar: sin nivel de severidad, sin marca de tiempo, sin poder redirigirlo, y en producción se mezcla con la salida real | **Sí**, y me corrigió el peso que le había dado. Yo lo anoté como suciedad de depuración; el problema real es que no hay forma de controlarlo sin recompilar |
| `LegacyParkingReceipt.java` : 28 | `java:S1125` — *Remove the unnecessary boolean literals. [+1 location]* | `fee == 0 ? true : false` da una vuelta para nada: la comparación ya produce el `true` o `false` que el ternario devuelve | **Sí.** No cambia el comportamiento, pero hace dudar al que lee si hay algún caso escondido |
| `LegacyParkingReceipt.java` : 30 | `java:S1125` — *Remove the unnecessary boolean literal.* | Mismo error en otra forma: `if (free == true)` compara el booleano contra `true` cuando `free` ya es la condición | **Sí**, el de menor impacto. Me llamó la atención que Sonar lo reportara dos veces por separado con la misma regla: marca cada ocurrencia, no una por archivo |

**Archivos sin hallazgos:** `ParkingFeeCalculator.java` y `ParkingFeeCalculatorTest.java`
se guardaron igual y **no generaron ningún aviso**. No es que estuvieran perfectos: los
números mágicos siguen ahí. La regla que los detectaría (`java:S109`) viene
**desactivada** en el perfil de calidad por defecto. Esto contesta P5.

---

## Code review vs. SonarQube

### P4. ¿Qué problema encontró Sonar que tú no habías identificado durante el code review manual?

**Dos, y uno de ellos es el más grave del archivo.**

El primero es `java:S4973`, el `plate == ""`. Yo leí esa línea y la di por buena: vi una
comparación contra una cadena vacía, me pareció una validación normal y seguí de largo.
Sonar la marcó y al abrir la descripción de la regla entendí por qué: `==` entre objetos
no compara el texto, compara si son el **mismo objeto en memoria**. Hoy funciona de pura
casualidad, porque Java guarda los literales como `""` en una caché común y el programa
solo recibe literales. En cuanto la placa venga de un formulario o de la base de datos va
a ser otro objeto, la condición dará falso aunque la cadena esté vacía, y se emitirá un
recibo con la placa en blanco. Es el único defecto del archivo que cambia el
comportamiento, y es justo el que se me escapó.

El segundo es la segunda ocurrencia de `java:S1125`, el `if (free == true)`. Este me
llama la atención por otra razón: yo **sí** había anotado el ternario
`fee == 0 ? true : false` dos líneas más arriba, así que ya tenía el ojo puesto en ese
tipo de error, y aun así no vi el mismo problema repetido inmediatamente después. Sonar
no se cansa ni da nada por revisado: marca cada ocurrencia por separado, una en la línea
28 y otra en la 30.

Lo que me llevo es que encontré los defectos **visibles** — el `println` que salta a la
vista, el ternario obviamente redundante, el literal repetido — y se me fueron los que
requieren saber cómo se comporta Java por dentro, que son precisamente los que llegan a
producción sin que nadie los note.

### P5. ¿Qué observación hiciste tú que Sonar no reportó?

Los **números mágicos de `ParkingFeeCalculator`**. Ese archivo salió del análisis con
**cero avisos**, aunque tiene seis números sueltos (15, 60, 20, 15, 80, 150) sin ningún
nombre que explique qué son. La regla que los detectaría es `java:S109`, pero viene
desactivada en el perfil de calidad por defecto, así que Sonar ni siquiera la evalúa.
Corregí ese hallazgo de todas formas, porque el método no se podía leer sin tener el
enunciado al lado.

Tampoco reportó que el literal `"ERROR"` se repita en cuatro validaciones dispersas. Eso
no es un error de sintaxis que se pueda marcar con una regla: es una observación de
diseño sobre cómo está organizado el método.

Y algo que noté al comparar las dos listas: Sonar no dijo **nada** sobre
`ParkingFeeCalculatorTest.java`. No puede saber que las cinco pruebas originales dejaban
sin cubrir las dos fronteras del cobro, que fue la observación que me llevó a agregar
seis pruebas nuevas. Para verlo hay que conocer las reglas del negocio y decidir qué
casos importan.

La lección es que Sonar reporta lo que su perfil tiene activado, no todo lo que está mal.
Si me hubiera quedado solo con la herramienta habría entregado el método de cobro con los
números sueltos y la suite sin pruebas de frontera, pensando que todo estaba limpio
porque el panel *Problems* estaba vacío.

### P6. ¿Consideras que todos los hallazgos de Sonar tienen la misma importancia? Explica un ejemplo.

No, y se ve con dos hallazgos del **mismo archivo**. `java:S4973` (`plate == ""`) es un
**defecto de comportamiento**: hoy devuelve el resultado correcto solo porque las pruebas
usan el literal `""`, pero el día que la placa llegue de un formulario la validación se
saltará y se emitirá un recibo con la placa en blanco. Es un error latente que pasa
desapercibido en pruebas y aparece en producción. `java:S1125` (`if (free == true)`) es
un **defecto de legibilidad**: el programa se comporta igual antes y después, solo gano
una línea más corta. Los dos salen en el panel con el mismo icono amarillo y parecen
pesar lo mismo. No pesan igual: si solo pudiera arreglar uno, arreglo el `S4973` sin
pensarlo. Por eso mi tabla de code review lleva su propia columna de severidad, y por eso
conviene mirar si Sonar clasifica la regla como bug, vulnerabilidad u *olor de código* en
lugar de tratar la lista como una fila homogénea de pendientes.

### P7. ¿Puede Sonar determinar por sí solo si "$20 de 16 a 60 minutos" es la regla correcta del negocio? ¿Por qué?

No, y no es una limitación que se arregle con una versión mejor: esa información **no
está en el código**. Sonar compara el código fuente contra un catálogo de reglas sobre
*cómo* se escribe Java — comparaciones mal hechas, variables sin usar, literales
redundantes — y todo eso se decide mirando el texto del programa. Pero que la tarifa sea
$20 y no $25, o que el rango llegue a 60 minutos y no a 45, es una decisión del dueño del
estacionamiento que solo existe en el enunciado o en un contrato; no hay nada dentro del
`.java` con qué contrastarla. La prueba está en lo que pasó aquí: si cambio el `20` por
un `25`, Sonar no dice nada (de hecho `ParkingFeeCalculator` salió con cero avisos tal
como estaba); lo que se pone rojo de inmediato es
`sixteenMinutesShouldChargeFlatRate`, porque esa prueba **sí** codifica la regla de
negocio: alguien la escribió sabiendo cuánto debe cobrarse. Sonar puede decir que un
método está bien escrito; no puede decir que esté calculando lo correcto.

### P8. Explica por qué el análisis estático NO sustituye: a) las pruebas unitarias; b) el code review humano.

**a) No sustituye a las pruebas unitarias.** El análisis estático nunca ejecuta el
programa: lee el texto y busca patrones conocidos. Las pruebas sí lo ejecutan, con
entradas concretas, y comparan el resultado contra el que debería dar. Por eso Sonar no
puede detectar que a los 121 minutos se cobren $35 en lugar de $50: para saberlo hay que
correr el método y conocer la regla. Un método puede estar impecable para Sonar y cobrar
mal todos los días. Al revés también: mis 11 pruebas estaban en verde con el
`plate == ""` ahí adentro, porque usaban literales. Ninguna cubre el hueco de la otra.

**b) No sustituye al code review humano.** Sonar solo encuentra lo que alguien ya
convirtió en regla, y solo las que su perfil tiene encendidas. Aquí se vio dos veces: no
reportó los números mágicos porque `java:S109` está desactivada, y no dijo nada sobre el
archivo de pruebas, donde faltaban las dos fronteras del cobro. Para ver eso segundo hay
que conocer las reglas del negocio y decidir qué casos importan, que no es algo que una
regla pueda evaluar. Un revisor humano juzga cosas que no son binarias: si un nombre
describe lo que hace, si las pruebas cubren lo que deben, si una decisión rara del código
es un error o algo deliberado. Lo que sí hace bien el análisis estático es quitarle al
humano el trabajo mecánico, para que pueda discutir el diseño y las reglas de negocio.
Por eso se usan los tres juntos, no uno en lugar de otro.

Y en esta actividad pasó también al revés, que es lo que me hizo entenderlo de verdad:
Sonar vio el `plate == ""` que yo había dado por bueno. No es que una revisión valga más
que la otra; es que cada una encuentra lo que la otra deja pasar.

---

## Correcciones realizadas

Después de cada cambio se ejecutó `./mvnw clean test` y se volvió a guardar el archivo en
VS Code para que Sonar lo reanalizara.

### Corrección 1 — `LegacyParkingReceipt.java`

- **Problema:** los cuatro hallazgos de Sonar. El grave es `java:S4973` (`plate == ""`),
  que compara referencias en lugar de contenido y que **yo no había visto** en mi
  revisión manual. Lo acompañan `java:S106` (el `println` imposible de apagar en
  producción) y dos ocurrencias de `java:S1125`. Aproveché para arreglar además el
  `"ERROR"` repetido que sí había anotado yo, y de paso el `String result = ""` que nunca
  se usa y el armado del recibo duplicado en las dos ramas del `if`.
- **Cambio:** `plate == ""` → `plate.isEmpty()` unido a la comprobación de `null`;
  `System.out.println(...)` → `LOGGER.fine(...)` con `java.util.logging.Logger`;
  `fee == 0 ? true : false` → `(fee == 0)`; `if (free == true)` → ternario solo para el
  monto; `"PARKING"` y `"ERROR"` a constantes `LABEL` y `ERROR_RESULT`; se eliminó
  `String result = ""` y el recibo se arma en un solo `return`.
- **Resultado:** el archivo pasó de 40 a 28 líneas y el formato del recibo queda en un
  único lugar. El comportamiento observable es idéntico para `null`, cadena vacía,
  minutos o cobro negativos, cobro cero y cobro normal.
- **¿Sonar dejó de reportarlo?** **Sí.** Tras guardar, el panel *Problems* quedó vacío
  para este archivo: desaparecieron `java:S4973`, `java:S106` y las dos de `java:S1125`.

### Corrección 2 — `ParkingFeeCalculator.java`

- **Problema:** los números mágicos (15, 60, 20, 15, 80, 150), incluido el `60` usado con
  dos significados. Este hallazgo **no lo reportó Sonar** — el archivo salió con cero
  avisos porque `java:S109` viene desactivada — y lo corregí igual, porque el código no
  se podía leer sin el enunciado al lado.
- **Cambio:** cada número a una constante `private static final`: `FREE_LIMIT_MINUTES`,
  `FLAT_RATE_LIMIT_MINUTES`, `MINUTES_PER_HOUR`, `FLAT_RATE`, `ADDITIONAL_HOUR_RATE`,
  `MAX_NORMAL_FEE` y `LOST_TICKET_FEE`. Los dos usos del `60` quedaron separados.
- **Resultado:** el método se lee como la regla de negocio
  (`if (minutes <= FREE_LIMIT_MINUTES) return 0;`) y un cambio de tarifa se hace en un
  solo lugar. La aritmética es la misma. **No corregí** el orden entre boleto perdido y
  minutos negativos: la regla dice "$150 sin importar el tiempo" y una prueba lo fija a
  propósito; cambiarlo la rompería.
- **¿Sonar dejó de reportarlo?** **No aplica**, nunca lo reportó: cero avisos antes y
  después. Esta corrección salió enteramente del code review humano, y para mí es la
  mejor prueba de por qué las dos revisiones se complementan (ver P5 y P8).

---

## Reto TDD (Parte 14, opcional)

`ParkingReservationPolicy.refundPercentage(int hoursBeforeStart)`, escrita con TDD:
primero la prueba, verla en rojo, lo mínimo para pasarla, verla en verde, refactorizar,
repetir. Reglas: 24 h o más → 100 % · de 2 a menos de 24 h → 50 % · menos de 2 h → 0 % ·
valor negativo → `IllegalArgumentException`. **7 pruebas**, con las fronteras de 24 h
(24 y 23) y de 2 h (2 y 1) cubiertas por ambos lados, con el mismo criterio de P2.

Escribir la prueba primero cambió la forma de pensarlo: en `ParkingFeeCalculator` el
código ya existía, lo leí y mis pruebas terminaron pareciéndose a lo que el código hacía.
Aquí no había dónde copiar, así que tuve que decidir qué debía pasar en las 24 horas
exactas **antes** de escribir el `if`.

---

## Resultado final

**BUILD SUCCESS: Sí**

```
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Desglose: 11 en `ParkingFeeCalculatorTest` (5 originales + 6 agregadas), 7 en
`ParkingReservationPolicyTest` (reto TDD) y 4 en `UsernamePolicyTest` (ya venían en el
proyecto).

### Cómo reproducir

```bash
./mvnw clean test        # Linux / macOS
.\mvnw.cmd clean test    # Windows
```
