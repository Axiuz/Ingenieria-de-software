# TuPastilla

App Android de seguimiento de medicamentos. Avisa a la hora exacta de cada toma,
registra lo que se tomó y lo que no, y lleva la cuenta de las pastillas que quedan
en la caja.

Está construida a partir del prototipo de diseño **TuPastilla · Paciente autónomo** y
de su especificación de implementación (proyecto de diseño `4825c2b6`).

## Cómo correrla

```bash
./gradlew :app:installDebug      # instala en el emulador o el teléfono conectado
./gradlew :app:testDebugUnitTest         # pruebas de lógica, en la JVM
./gradlew :app:connectedDebugAndroidTest # pruebas de Room, en dispositivo
```

Para llenar la app sin capturar nada: **Ajustes → Cargar datos de prueba**. Siembra
según el rol: al paciente le deja tres medicinas propias; al cuidador, seis residentes
con sus medicamentos repartidos entre las 07:00 y las 22:00. En los dos casos añade las
tomas de hoy y seis días de historial.

Para ver el aviso sin esperar a su hora: **Ajustes → Ver cómo se ve el aviso del
sistema**.

## Cómo está armada

Kotlin y vistas XML, sin Compose. Una sola `MainActivity` que intercambia fragments con
`FragmentManager` y pinta su propia barra de cuatro pestañas. La alerta de toma es una
Activity aparte porque se lanza desde una notificación de pantalla completa, con el
teléfono bloqueado.

```
com.tupastilla
├── MainActivity.kt          cuatro pestañas + navegación
├── onboarding/              C0 · C10 · C11 · C11b · C13 · C15
├── hoy/                     A1 · pestaña 1
├── medicinas/               A4b · B6 · B7 · B10 · pestaña 2
├── historial/               B8 + B9 · pestaña 3
├── ajustes/                 C16 · C12a · pestaña 4
├── alerta/                  A2 · A3
├── alarma/                  AlarmManager, canal y receivers
├── data/
│   ├── local/               Room: entidades, DAOs, base y datos de prueba
│   └── repo/                MedicamentoRepository + implementación Room
└── ui/                      tema, tarjeta de toma, hojas y extensiones
```

La UI solo conoce `MedicamentoRepository`. Hoy lo implementa `RoomMedicamentoRepository`;
sustituirlo por uno remoto no obliga a tocar ningún fragment.

## Modos de visión y densidad

Hay cuatro modos de visión (estándar, protan/deutan, tritan y sin color) y dos
densidades de texto. Los dos se eligen en el onboarding y se cambian en Ajustes.

Cada modo es un `ThemeOverlay` que redefine cuatro atributos de color
(`tpConfirmada`, `tpOmitida`, `tpPendiente`, `tpAccion`). La densidad es otro overlay
que redefine los tamaños, declarados como atributos de dimensión (`tpTextCuerpo`,
`tpTouchMin`, …) porque un `ThemeOverlay` no puede sobrescribir `dimen`. Cambiar
cualquiera de los dos guarda la preferencia y llama `recreate()`.

**La regla que sostiene los cuatro modos: ningún estado se distingue solo por color.**
La tarjeta de toma tiene doce variantes (tres estados × cuatro modos) y un único
`item_tarjeta_medicamento.xml`. El estado decide el glifo, si el icono va relleno o
calado, el grosor del borde y el texto de la etiqueta; el modo solo decide de qué
atributo sale el color.

## Avisos

Se programa **una alarma por toma**, no una por medicina. Al guardar una medicina se
crean sus tomas de las próximas 48 horas y se programa cada una con
`setExactAndAllowWhileIdle`. Desde Android 12 se comprueba `canScheduleExactAlarms()`
y, si no está concedido, se degrada a `setWindow`. Después de un reinicio,
`ReprogramarAlarmasReceiver` las vuelve a poner todas.

La notificación es de canal `IMPORTANCE_HIGH` con `setFullScreenIntent` y dos acciones
—confirmar y posponer— que resuelve un `BroadcastReceiver` sin abrir la app.

## Accesibilidad

- Área táctil mínima de 64 dp en densidad accesible (48 dp en compacta), fijada con
  `minHeight`, no con padding.
- `contentDescription` en la tarjeta de toma completa, no en sus partes: TalkBack lee
  *"Losartán, 50 mg, 08:00, pendiente"* de una vez.
- Todo el texto en `sp` y sin alturas fijas en contenedores de texto.
- La confirmación de una toma se anuncia con `announceForAccessibility`.
- En la alerta el foco inicial es el botón de confirmar.

## Alcance

Está implementado el flujo del paciente **autónomo**. La pantalla de selección de rol
ofrece los tres y el modelo de datos los guarda, pero las pantallas exclusivas de los
roles supervisado y cuidador (A4a, C12b, C14) quedan para una segunda entrega.

## Integrantes

- Saúl Benjamín Hernández Torres
- Yael Camberos Fernández
