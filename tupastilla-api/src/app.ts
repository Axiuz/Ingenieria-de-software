import cors from "cors";
import express, { type ErrorRequestHandler } from "express";
import rateLimit from "express-rate-limit";
import helmet from "helmet";
import { AuthService } from "./auth/authService.js";
import { authRoutes } from "./auth/routes.js";
import { bcryptHasher, type PasswordHasher } from "./auth/password.js";
import { TokenService } from "./auth/tokens.js";
import type { Config } from "./config.js";
import type { RefreshTokenRepository, UserRepository } from "./domain.js";
import { HttpError } from "./errors.js";
import { userRoutes } from "./users/routes.js";

export interface AppDeps {
  config: Config;
  users: UserRepository;
  refreshTokens: RefreshTokenRepository;
  hasher?: PasswordHasher;
  now?: () => Date;
  log?: (error: unknown) => void;
}

/**
 * Recibe repositorios y reloj inyectados para que las pruebas usen repos en memoria
 * sin base de datos.
 */
export function createApp(deps: AppDeps) {
  const { config } = deps;
  const now = deps.now ?? (() => new Date());
  const tokens = new TokenService(
    {
      secret: config.JWT_SECRET,
      issuer: config.JWT_ISSUER,
      audience: config.JWT_AUDIENCE,
      accessTtlSeconds: config.ACCESS_TOKEN_TTL_SECONDS,
      refreshTtlDays: config.REFRESH_TOKEN_TTL_DAYS
    },
    now
  );
  const auth = new AuthService({
    users: deps.users,
    refreshTokens: deps.refreshTokens,
    hasher: deps.hasher ?? bcryptHasher(config.BCRYPT_COST),
    tokens,
    maxFailedLogins: config.MAX_FAILED_LOGINS,
    lockMs: config.LOCK_MINUTES * 60_000,
    now
  });

  const app = express();
  app.disable("x-powered-by");
  // Detras de nginx el rate limit necesita la IP real del cliente; sin proxy, confiar
  // en X-Forwarded-For dejaria falsificarla.
  app.set("trust proxy", config.TRUST_PROXY ? 1 : false);
  app.use(helmet());
  // La app movil no necesita CORS: cerrado salvo los origenes configurados.
  app.use(cors({ origin: config.CORS_ORIGINS.length > 0 ? config.CORS_ORIGINS : false }));
  // Ninguna peticion legitima pesa mas; corta abusos de memoria.
  app.use(express.json({ limit: "10kb" }));

  app.get("/health", (_req, res) => {
    res.json({ status: "ok" });
  });

  const authLimiter = rateLimit({
    windowMs: 15 * 60_000,
    limit: config.AUTH_RATE_LIMIT,
    standardHeaders: "draft-8",
    legacyHeaders: false,
    message: { error: { code: "RATE_LIMITED", message: "Demasiados intentos, espera unos minutos" } }
  });

  app.use("/api/auth", authLimiter, authRoutes(auth));
  app.use("/api", userRoutes({ users: deps.users, refreshTokens: deps.refreshTokens, tokens, now }));

  app.use((_req, res) => {
    res.status(404).json({ error: { code: "NOT_FOUND", message: "Ruta no encontrada" } });
  });

  app.use(errorHandler(deps.log ?? ((error) => console.error(error))));
  return app;
}

const BODY_ERRORS: Record<string, [number, string, string]> = {
  "entity.parse.failed": [400, "INVALID_JSON", "El cuerpo no es JSON válido"],
  "entity.too.large": [413, "PAYLOAD_TOO_LARGE", "El cuerpo es demasiado grande"]
};

/**
 * Un error inesperado se registra y se responde con un 500 generico: nunca se
 * devuelven su mensaje ni su stack.
 */
function errorHandler(log: (error: unknown) => void): ErrorRequestHandler {
  return (error, _req, res, _next) => {
    if (error instanceof HttpError) {
      res.status(error.status).json({
        error: { code: error.code, message: error.message, details: error.details }
      });
      return;
    }
    const known = BODY_ERRORS[(error as { type?: string })?.type ?? ""];
    if (known) {
      res.status(known[0]).json({ error: { code: known[1], message: known[2] } });
      return;
    }
    log(error);
    res.status(500).json({ error: { code: "INTERNAL", message: "Error interno" } });
  };
}
