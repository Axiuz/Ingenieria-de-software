import { z } from "zod";

const booleanFromEnv = z
  .enum(["true", "false", "1", "0"])
  .default("false")
  .transform((value) => value === "true" || value === "1");

const schema = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  PORT: z.coerce.number().int().min(1).max(65535).default(3000),
  DATABASE_URL: z.string().min(1),
  JWT_SECRET: z.string().min(32, "JWT_SECRET debe tener al menos 32 caracteres"),
  JWT_ISSUER: z.string().min(1).default("tupastilla-api"),
  JWT_AUDIENCE: z.string().min(1).default("tupastilla-app"),
  ACCESS_TOKEN_TTL_SECONDS: z.coerce.number().int().min(60).max(3600).default(900),
  REFRESH_TOKEN_TTL_DAYS: z.coerce.number().int().min(1).max(30).default(7),
  CORS_ORIGINS: z
    .string()
    .default("")
    .transform((value) => value.split(",").map((origin) => origin.trim()).filter(Boolean)),
  TRUST_PROXY: booleanFromEnv,
  BCRYPT_COST: z.coerce.number().int().min(10).max(14).default(12),
  MAX_FAILED_LOGINS: z.coerce.number().int().min(3).max(20).default(5),
  LOCK_MINUTES: z.coerce.number().int().min(1).max(1440).default(15),
  AUTH_RATE_LIMIT: z.coerce.number().int().min(1).max(1000).default(20)
});

export type Config = z.infer<typeof schema>;

/**
 * Falla al arrancar si falta o sobra algo. El error nombra las variables invalidas
 * pero no sus valores, para no filtrar el secreto.
 */
export function loadConfig(env: NodeJS.ProcessEnv = process.env): Config {
  const result = schema.safeParse(env);
  if (!result.success) {
    const problems = result.error.issues
      .map((issue) => `${issue.path.join(".")}: ${issue.message}`)
      .join("; ");
    throw new Error(`Configuración inválida: ${problems}`);
  }
  return result.data;
}
