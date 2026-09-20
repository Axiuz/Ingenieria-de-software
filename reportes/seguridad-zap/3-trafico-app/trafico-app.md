# Tráfico de la app Android capturado con OWASP ZAP

Emulador Pixel_7 con ZAP como proxy HTTP. Cuerpos y cabeceras omitidos: llevan contraseñas y tokens de la cuenta de prueba.

| # | Método | URL | Respuesta | Nota |
|---|---|---|---|---|
| 1 | GET | `/health` | 200 OK | Comprobación manual del proxy |
| 2 | POST | `/api/auth/logout` | 429 Too Many Requests | 429: el rate limit seguía agotado por el escaneo activo previo |
| 3 | POST | `/api/auth/register` | 201 Created | Alta de cuenta desde la app: 201 con sesión |
| 4 | POST | `/api/auth/login` | 401 Unauthorized | Contraseña incorrecta: 401 con mensaje genérico, sin decir si el correo existe |
| 5 | POST | `/api/auth/login` | 200 OK | Login correcto: 200 con access y refresh token |
