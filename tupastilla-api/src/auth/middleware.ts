import type { RequestHandler } from "express";
import type { Role } from "../domain.js";
import { forbidden, unauthorized } from "../errors.js";
import type { AccessClaims, TokenService } from "./tokens.js";

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Express {
    interface Request {
      auth?: AccessClaims;
    }
  }
}

// Exige exactamente tres segmentos base64url antes de intentar verificar.
const BEARER = /^Bearer ([A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+)$/;

/** Rechaza con 401 las peticiones sin token Bearer valido y deja los claims en la peticion. */
export function requireAuth(tokens: TokenService): RequestHandler {
  return (req, _res, next) => {
    const match = BEARER.exec(req.headers.authorization ?? "");
    if (!match?.[1]) {
      next(unauthorized());
      return;
    }
    req.auth = tokens.verifyAccess(match[1]);
    next();
  };
}

/** 403 si el rol del token no esta entre los permitidos. Va despues de requireAuth. */
export function requireRole(...roles: Role[]): RequestHandler {
  return (req, _res, next) => {
    if (!req.auth) {
      next(unauthorized());
      return;
    }
    next(roles.includes(req.auth.role) ? undefined : forbidden());
  };
}
