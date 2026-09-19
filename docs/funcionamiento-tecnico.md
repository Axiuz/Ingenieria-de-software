# TuPastilla por dentro
18 de septiembre de 2026

Cómo funciona la app detrás de escenas: qué hace en el celular, cómo se autentica contra la API con JWT, qué guarda cada base de datos y cómo mantener el código, desde el entorno local hasta el pipeline de CI/CD.

TuPastilla tiene dos partes:

- **La app Android** (Kotlin, Room): guarda y gestiona todo lo relativo a medicamentos, horarios, tomas y alarmas, dentro del teléfono.
- **La API** (Node 24, Express 5, TypeScript, Prisma 7, MySQL 8.4): solo se encarga de las cuentas, el inicio de sesión y los roles.

## 1. Qué hace la app en el celular

La app para adultos mayores, pacientes autónomos y cuidadores se centra en la gestión diaria de tomas de medicamentos desde el dispositivo móvil. Todo el proceso de registro, seguimiento y recordatorio se ejecuta localmente, sin depender de la API, que solo maneja autenticación y cuentas. La app sigue avisando a tiempo aunque el teléfono no tenga conexión.

### 1.1 Arquitectura: datos locales, sin dependencia de la API

Los datos de medicamentos, horarios y cumplimiento viven exclusivamente en el dispositivo, mediante `Room` y una base de datos SQLite privada. La API no almacena información de medicamentos, solo gestiona sesiones y autenticación. Por eso los avisos y el historial funcionan sin conexión. La contrapartida: si se borra la app o se cambia de teléfono, esos datos no se recuperan (la sincronización con el servidor está en el plan de mejora).

`data/Grafo.kt` actúa como un localizador mínimo: la app no usa un framework de inyección de dependencias, y este objeto es el único sitio donde se decide qué implementación recibe la UI (por ejemplo, el repositorio de Room o el cliente de la API). Si cambias una implementación, el cambio se hace ahí y las pantallas no se enteran.

Los paquetes están organizados por funcionalidad:
- `auth`: login, registro, gestión de sesiones
- `data`: Room, repositorios, calendario, cumplimiento, `ExpansorHorarios`
- `hoy`, `medicinas`, `historial`, `personas`, `ajustes`, `onboarding`, `alarma`, `alerta`, `ui`

Todas las rutas de este documento parten de `app/src/main/java/com/tupastilla/`.

### 1.2 Base de datos local (Room): estructura de tablas

La base de datos se compone de cinco tablas clave:

| Tabla | Qué guarda |
|---|---|
| `perfil` | Una sola fila: quién usa el teléfono y cómo quiere verlo (nombre, rol, modo de visión, densidad, preferencias de aviso y, para el cuidador, la persona que está filtrando). Si existe, la app arranca en Hoy; si no, en el onboarding. |
| `persona` | A quién le tocan las medicinas: nombre, una nota libre para ubicarla ("Mi mamá", "Habitación 4") y si está activa. El autónomo tiene una sola, él mismo; el cuidador puede tener varias. |
| `medicina` | Persona a la que pertenece, nombre, dosis, forma de la pastilla ("Blanca redonda"), instrucciones, existencias y umbral de aviso. |
| `horario` | Hora en formato 24 h, días de la semana y fechas desde y hasta. |
| `toma` | Una toma concreta: medicina, horario, momento programado, estado (pendiente, confirmada u omitida), cuándo se confirmó y motivo si se omitió. |

Cada medicina puede tener múltiples horarios. Cada horario se convierte en una toma concreta en el tiempo. Un índice único sobre horario y momento programado permite regenerar la ventana de 48 horas sin duplicar tomas. El historial y el porcentaje de cumplimiento se calculan a partir del estado de cada toma.

### 1.3 Del horario a la alarma: flujo paso a paso

1. El usuario guarda una medicina con horarios en la pantalla de medicinas.
2. `ExpansorHorarios` recibe los horarios y genera una lista de tomas para las próximas 48 horas.
3. `AlarmaScheduler` programa una alarma exacta (permiso `SCHEDULE_EXACT_ALARM`) para cada toma.
4. A la hora indicada, `AlarmaTomaReceiver` verifica que la toma aún esté pendiente. Si lo está, muestra una notificación.
5. `AlertaTomaActivity` ocupa la pantalla completa (usando `USE_FULL_SCREEN_INTENT`) incluso si el teléfono está bloqueado.
6. El usuario confirma o omite la toma desde la alerta o notificación, lo cual se procesa por `AccionTomaReceiver`.
7. Si el perfil tiene activada la repetición, se vuelve a avisar a los 10 minutos cuando la toma sigue pendiente.
8. Al reiniciar el dispositivo, `ReprogramarAlarmasReceiver` (activado por `RECEIVE_BOOT_COMPLETED`) vuelve a programar todas las alarmas, porque Android borra las alarmas al apagar el dispositivo.

Este flujo garantiza que el usuario no olvide una toma, incluso en caso de apagado o reinicio.

### 1.4 Roles en la app

- **AUTONOMO**: organiza sus medicinas, puede editar horarios y ver cumplimiento.
- **SUPERVISADO**: ve su lista de medicinas pero no puede modificarla; la estructura está definida por un familiar.
- **CUIDADOR**: gestiona múltiples personas desde la pestaña "Personas", en lugar de ver "Mis medicinas".
- **ADMIN**: existe solo en la API; en la app se comporta como un AUTONOMO porque no hay pantallas de administración.

El rol no se elige en el teléfono. Llega firmado en el token del servidor, lo que garantiza que no se pueda manipular el rol desde la app. `ui/Roles.kt` decide qué acciones se permiten en cada pantalla.

### 1.5 Sesión en el teléfono

El login y registro se hacen contra la API. La sesión (access token, refresh token, datos del usuario) se guarda cifrada con AES-256-GCM usando una clave almacenada en el Android Keystore (`AlmacenSesionCifrado`). Así, aunque alguien copie los archivos de la app, no puede leer los tokens sin la clave, que nunca sale del Keystore.

El archivo de sesión se excluye de respaldos en la nube. Si el dispositivo se reinstala o se restaura, la clave del Keystore puede no existir, por lo que se borra la sesión y se pide login nuevamente.

`CuentaLocal` recuerda de qué cuenta son los datos guardados en el teléfono. Si entra otra cuenta, se cancelan las alarmas y notificaciones y se vacían Room y las preferencias, para que no vea datos ajenos. Si los datos no tienen dueño (son de antes de que existiera el login), la primera cuenta que entra se los queda.

Cerrar sesión borra la sesión local primero, y luego envía una solicitud al servidor para cancelar la sesión. Esto permite que la acción funcione incluso sin conexión.

### 1.6 Accesibilidad

La app ofrece modos de visión para daltonismo (4 opciones elegidas en el onboarding), y densidad ("texto grande" o "cabe más en pantalla"). La fuente de verdad es la tabla `perfil`, pero se guarda una copia en `SharedPreferences` (`ui/Preferencias.kt`) porque el tema se aplica antes de dibujar la pantalla y en ese momento todavía no se puede esperar una consulta a Room.

## 2. Autenticación con JWT, la API y la base de datos

### 2.1 Qué es cada token

| Token | Formato | Vida | Dónde vive | Para qué |
|---|---|---|---|---|
| Access token | JWT firmado con HS256 | 15 minutos (`ACCESS_TOKEN_TTL_SECONDS=900`) | En el teléfono, cifrado; viaja en cada petición como `Authorization: Bearer <token>` | Demostrar quién eres y qué rol tienes |
| Refresh token | 32 bytes aleatorios en base64url, opaco | 7 días (`REFRESH_TOKEN_TTL_DAYS`) | En el teléfono, cifrado; en MySQL solo su hash SHA-256 | Pedir un access token nuevo sin volver a escribir la contraseña |

El access token es un JWT (JSON Web Token) firmado con HS256, que contiene datos clave como el `sub` (id del usuario), `email`, `role`, `iss` ("tupastilla-api"), `aud` ("tupastilla-app") y un `jti` (JWT ID) único. Esta firma permite que el servidor verifique que el token no fue alterado. Se envía en cada solicitud como `Authorization: Bearer <token>`, lo que lo convierte en un mecanismo de autenticación por defecto.

El refresh token es un valor opaco (no es un JWT): no lleva datos y solo vale si su hash está en la base y no está revocado, así que el servidor puede anularlo en cualquier momento. En MySQL se guarda solo su SHA-256: si se fuga la tabla `RefreshToken`, los hashes no sirven para renovar sesiones.


### 2.2 Flujo completo

1. El usuario realiza registro o login. La API responde con un access token y un refresh token.
2. La app guarda ambos tokens cifrados con AES-256-GCM y una clave del Android Keystore (`AlmacenSesionCifrado`).
3. Antes de cada petición, el repositorio `SesionRepository.tokenVigente()` lee el `exp` del JWT sin verificar la firma (porque el servidor lo hace), y si queda menos de 30 segundos, llama a `POST /api/auth/refresh` con el refresh token.
4. El servidor busca el hash del refresh token, comprueba que no esté vencido ni revocado y responde con un access token y un refresh token nuevos.
5. Cada refresh token se usa solo una vez y se revoca en la base de datos al usarlo. Si se intenta usar un token ya revocado, se asume un robo de sesión y se revocan *todas* las sesiones del usuario.
6. Al hacer logout, el refresh token se marca como revocado en la base de datos, lo que obliga a que el usuario inicie sesión nuevamente.

Este flujo evita que el cliente se quede sin acceso a recursos mientras el token expira. La rotación del refresh token previene que un atacante que obtenga un token viejo pueda usarlo indefinidamente. Si un atacante y el usuario legítimo usan el mismo refresh, el segundo en usarlo dispara la revocación total: ambos pierden la sesión y el usuario vuelve a entrar con su contraseña, que el atacante no tiene. Si el servidor dice que la sesión ya no vale, la app borra la sesión local y pide login; si el fallo es de red, conserva la sesión.


### 2.3 Cómo verifica la API cada petición

- `requireAuth` exige que la petición tenga `Authorization: Bearer <token>`, y que el token tenga tres segmentos (header, payload, signature).
- `TokenService.verifyAccess` valida que el algoritmo sea HS256 (no permite algoritmos como "none" que podrían ser usados para falsificar tokens).
- Se verifica la firma, la expiración (`exp`), el emisor (`iss`), el destinatario (`aud`), y que el rol sea uno de los cuatro permitidos: `AUTONOMO`, `SUPERVISADO`, `CUIDADOR`, `ADMIN`.
- `requireRole("ADMIN")` protege rutas como `GET /api/users` y `PATCH /api/users/:id/role`.
- Si se cambia el rol de un usuario, se revocan *todos* los refresh tokens del usuario, forzando una reinicio de sesión con el nuevo rol.
- Un usuario con rol `ADMIN` no puede cambiar su propio rol, para que el sistema nunca se quede sin administrador.

Este diseño evita que un usuario con rol inferior acceda a funciones de administración. La revocación de tokens al cambiar el rol asegura que el cambio tenga efecto como máximo en 15 minutos, cuando caduca el access token que aún lleva el rol viejo.


### 2.4 Protecciones del login

- Las contraseñas se guardan como hash `bcrypt` con coste 12 (lento a propósito, para encarecer la fuerza bruta), con longitud mínima de 10 caracteres (letra + número) y máxima de 72 bytes (límite de bcrypt).
- Se bloquea el login durante 15 minutos tras 5 intentos fallidos (`MAX_FAILED_LOGINS=5`, `LOCK_MINUTES=15`).
- Se limita a 20 peticiones por IP cada 15 minutos en `/api/auth` (`AUTH_RATE_LIMIT=20`).
- Si el correo no existe, la contraseña es incorrecta o la cuenta está bloqueada, se devuelve un 401 genérico.
- Para igualar el tiempo de respuesta, si el correo no existe, se compara un hash ficticio para evitar que el cliente sepa si el correo está registrado o no.
- `helmet` añade cabeceras de seguridad (HSTS, `nosniff`, CSP, `X-Frame-Options`) y se quita `x-powered-by`.
- CORS está cerrado por defecto: la app móvil no lo necesita, y solo se abre a los orígenes listados en `CORS_ORIGINS`.
- Los cuerpos de peticiones tienen máximo 10 kB.
- Los errores 500 no incluyen mensaje ni stack, para evitar información sensible.

Estas medidas evitan ataques de fuerza bruta, exceso de peticiones y revelación de información. El bloqueo temporal y el límite de peticiones por IP protegen contra escaneos masivos.


### 2.5 Endpoints

| Método y ruta               | Quién                 | Qué hace                                                                 |
|----------------------------|------------------------|---------------------------------------------------------------------------|
| POST /api/auth/register    | Público                | Registra un usuario con rol `AUTONOMO`, `SUPERVISADO` o `CUIDADOR`        |
| POST /api/auth/login       | Público                | Inicia sesión y devuelve access + refresh token                           |
| POST /api/auth/refresh     | Público (lleva el refresh en el cuerpo) | Renueva el access token usando el refresh token                          |
| POST /api/auth/logout      | Público (lleva el refresh en el cuerpo) | Revoca el refresh token y termina la sesión                              |
| GET /api/me                | Cualquier usuario con sesión | Devuelve id, correo, nombre, rol y fecha de alta del usuario                     |
| GET /api/permisos          | Cualquier usuario con sesión | Devuelve qué puede hacer su rol: `puedeEditar`, `gestionaPersonas`, `administraUsuarios`               |
| GET /api/users             | Solo ADMIN             | Lista todos los usuarios                                                 |
| PATCH /api/users/:id/role  | Solo ADMIN             | Cambia el rol de un usuario; revoca todos los refresh tokens del usuario |

El primer administrador se crea con la semilla usando `ADMIN_EMAIL` y `ADMIN_PASSWORD`, porque el registro público nunca permite pedir el rol ADMIN.


### 2.6 Base de datos MySQL (Prisma)

- **Tabla `User`**:
  - `id`: UUID (clave primaria)
  - `email`: único
  - `name`: nombre del usuario
  - `passwordHash`: hash de la contraseña (bcrypt)
  - `role`: rol del usuario
  - `failedLogins`: intentos fallidos
  - `lockedUntil`: fecha de bloqueo (si está bloqueado)
  - `createdAt`: fecha de creación

- **Tabla `RefreshToken`**:
  - `id`: clave primaria
  - `userId`: referencia al usuario
  - `tokenHash`: hash SHA-256 del token (64 caracteres)
  - `expiresAt`: fecha de vencimiento
  - `revokedAt`: fecha de revocación (si está revocado)
  - `createdAt`: fecha de creación

Se borra la tabla `RefreshToken` en cascada cuando el usuario se borra. El esquema está en `tupastilla-api/prisma/schema.prisma`, y las migraciones en `prisma/migrations/`. La API usa repositorios como interfaz: implementación en Prisma en producción, y en memoria en pruebas.


### 2.7 Configuración

Las variables de entorno se validan con `zod` al arrancar en `src/config.ts`. Si falta `DATABASE_URL` o `JWT_SECRET` tiene menos de 32 caracteres, la API no inicia. El mensaje de error nombra la variable pero nunca su valor, para evitar exposición de secrets.

## 3. Mantener el código: entorno, pipeline y cambios frecuentes

### 3.1 Entorno local

Todo se levanta con un comando: `./Scripts/start.sh`. El script crea `tupastilla-api/.env` con un `JWT_SECRET` aleatorio si no existe (así nadie usa un secreto fijo copiado de un ejemplo), levanta MySQL en Docker, instala dependencias, genera el cliente de Prisma, aplica las migraciones, siembra la base y arranca la API en modo watch en el puerto 3000.

MySQL de desarrollo se define en `docker-compose.dev.yml`, así todos usan la misma versión (8.4). La app Android, al estar en modo debug, apunta a `http://10.0.2.2:3000/`, que es la IP del Mac vista desde el emulador. Esto es necesario porque el emulador de Android no puede acceder a `localhost` directamente.

El tráfico sin cifrado está permitido solo en modo debug hacia `10.0.2.2` y `localhost`. La versión release solo acepta HTTPS (`network_security_config.xml`), así que la URL de producción se pasa al compilar con `-Ptupastilla.apiUrl=...`.

En producción, el flujo es diferente. Se usa `docker compose up -d --build` con un `.env` en la raíz del proyecto, basado en `.env.example`. Este entorno levanta MySQL, un contenedor dedicado a migraciones y siembra de datos, y la API. El contenedor de la API corre como el usuario `node` (no root), sin capacidades de Linux, sin posibilidad de ganar privilegios y con el sistema de archivos en solo lectura: si alguien lograra ejecutar código dentro, no podría instalar nada ni modificar la aplicación.

### 3.2 Pipeline de CI/CD

El pipeline se ejecuta en cada push a `main` o a `Proyecto-TuPastilla`, en pull requests, en tags `v*` y por lanzamiento manual. Se divide en cuatro jobs, cada uno con un objetivo claro y un comportamiento en caso de falla.

| Job | Qué hace | Si falla |
|-----|---------|--------|
| api | `pnpm install --frozen-lockfile`, `prisma generate`, `ESLint` (con reglas de seguridad y complejidad ciclomática máxima de 10), `tsc`, 82 pruebas con cobertura mínima del 80%, `pnpm audit` sin vulnerabilidades altas (CRITICAL o HIGH) | El pipeline queda en rojo y no se despliega; el reporte de cobertura queda como artefacto |
| android | Pruebas unitarias, verificación de cobertura del paquete `auth` (mínimo 80% de líneas y ramas), análisis con Android Lint, generación de APK release con R8 | Queda en rojo; los reportes de pruebas, JaCoCo y Lint quedan como artefactos |
| seguridad | Escaneo de `gitleaks` en todo el historial, análisis con `CodeQL security-extended`, construcción de la imagen Docker y escaneo con `Trivy` (falla si hay vulnerabilidades CRITICAL o HIGH con parche disponible), levantamiento de la pila con `docker compose` y ejecución de OWASP ZAP baseline | Queda en rojo; el reporte de ZAP queda como artefacto y los hallazgos de CodeQL en la pestaña Security de GitHub |
| deploy | Solo se activa con `push a main`, `tag v*` o lanzamiento manual. Solo si los tres jobs anteriores pasan, se publica la imagen de la API en `ghcr.io/axiuz/tupastilla-api` y se sube un APK firmado en GitHub Releases | No publica nada; la versión anterior sigue siendo la vigente |

El despliegue usa el environment `produccion` de GitHub y cuatro secretos: `ANDROID_KEYSTORE_BASE64` (el keystore de firma en base64), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` y `ANDROID_KEY_PASSWORD`. El keystore original hay que guardarlo fuera del repositorio: sin él no se pueden publicar actualizaciones del APK con la misma firma.

Para publicar una versión: `git tag v1.0.0 && git push origin v1.0.0`. El tag dispara el pipeline completo y, si todo pasa, la imagen sale con la etiqueta de la versión y el APK aparece en Releases.

### 3.3 Cambios frecuentes

1. **Cambiar la base de datos**
   Edita `prisma/schema.prisma`. Con MySQL de desarrollo encendido, ejecuta `pnpm exec prisma migrate dev --name <nombre>`: genera el SQL en `prisma/migrations/` y lo aplica. Revisa ese SQL antes de subirlo: una vez se coló al final del archivo el aviso de actualización de la CLI de Prisma y MySQL rechazó la migración. En producción no se ejecuta nada a mano: el contenedor `migrate` aplica `prisma migrate deploy` antes de arrancar la API.

2. **Agregar un endpoint protegido**
   Define la ruta en `src/users/routes.ts` o crea un nuevo router tras `requireAuth`. Si el endpoint solo está disponible para ciertos roles, usa `requireRole(...)`. Valida el cuerpo de la petición con un esquema `z.strictObject` para evitar campos extra. Asegúrate de tener pruebas en `test/api.test.ts` que usen repositorios en memoria.

3. **Agregar o cambiar un rol**
   Añade el rol al `enum Role` de `prisma/schema.prisma` y genera la migración; en la API, a la lista `ROLES` de `src/domain.ts` (y a `SELF_ASSIGNABLE_ROLES` si se puede elegir al registrarse); en la app, a `ui/Roles.kt` y a `PerfilRepository.rolLocal`. Esto asegura que el sistema tenga consistencia en el acceso.

4. **Rotar el JWT_SECRET**
   Cambia el valor en el `.env` del servidor y reinicia la API. Todos los access tokens dejan de valer, pero los refresh no dependen del secreto: las apps renuevan su sesión solas, sin pedir la contraseña.

5. **Antes de subir**
   Ejecuta `pnpm lint && pnpm typecheck && pnpm test:coverage` en la API y `./gradlew :app:testDebugUnitTest :app:verificarCoberturaAuth :app:lintDebug` en la app. Estos comandos son los mismos que se ejecutan en el pipeline, así que si pasan en tu máquina, lo normal es que pasen en GitHub.

### 3.4 Dónde está cada cosa

| Necesito... | Archivo |
|------------|--------|
| login de la app | `auth/LoginFragment.kt` (rutas de la app bajo `app/src/main/java/com/tupastilla/`) |
| sesión cifrada | `auth/AlmacenSesion.kt` |
| cliente HTTP | `auth/AuthApi.kt` |
| renovación de tokens | `auth/SesionRepository.kt` |
| alarmas | `alarma/AlarmaScheduler.kt` |
| base local | `data/local/` |
| firma y verificación JWT | `tupastilla-api/src/auth/tokens.ts` |
| lógica de login y bloqueo | `tupastilla-api/src/auth/authService.ts` |
| permisos por rol | `tupastilla-api/src/auth/middleware.ts` |
| variables | `tupastilla-api/src/config.ts` |
| esquema | `tupastilla-api/prisma/schema.prisma` |
| pipeline | `.github/workflows/ci-cd.yml` |
