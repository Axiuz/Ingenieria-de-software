export class HttpError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly details?: unknown
  ) {
    super(message);
    this.name = "HttpError";
  }
}

export const unauthorized = (message = "No autenticado") =>
  new HttpError(401, "UNAUTHORIZED", message);

export const invalidCredentials = () =>
  new HttpError(401, "INVALID_CREDENTIALS", "Correo o contraseña incorrectos");

export const forbidden = () =>
  new HttpError(403, "FORBIDDEN", "No tienes permiso para esta acción");

export const notFound = (what = "Recurso") =>
  new HttpError(404, "NOT_FOUND", `${what} no encontrado`);
