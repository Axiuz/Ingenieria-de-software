# Unit Testing examples

Ejemplos para proyectar en VS Code durante la clase.

## Requisitos

- JDK 17 instalado.
- Conexión a Internet durante la primera ejecución.
- **No es necesario instalar Maven**.

## Ejecutar las pruebas

### Windows / PowerShell

```powershell
.\mvnw.cmd test
```

o para limpiar y ejecutar todo:

```powershell
.\mvnw.cmd clean test
```

### Linux / macOS

```bash
./mvnw test
```

La primera ejecución descarga Maven 3.9.16 automáticamente y Maven descarga JUnit y las dependencias del `pom.xml`. Las siguientes ejecuciones reutilizan el caché local.

## Verificar Java

```powershell
java -version
javac -version
```

El proyecto está configurado para Java 17.

## Contenido

- `ParkingFeeCalculator`: ejemplo de pruebas para código existente.
- `UsernamePolicy`: ejemplo pequeño para explicar TDD.
