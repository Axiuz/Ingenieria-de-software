import { randomUUID } from "node:crypto";
import { describe, expect, it } from "vitest";
import { requireRole } from "../src/auth/middleware.js";
import { bcryptHasher } from "../src/auth/password.js";
import { loadConfig } from "../src/config.js";
import { HttpError } from "../src/errors.js";
import { memoryRefreshTokenRepository, memoryUserRepository } from "../src/repositories/memory.js";
import { idSchema, loginSchema, parse, refreshSchema, registerSchema } from "../src/validation.js";

const BASE = { DATABASE_URL: "mysql://u:p@localhost/db", JWT_SECRET: "s".repeat(32) };

function validationError(action: () => unknown): HttpError {
  try {
    action();
  } catch (error) {
    expect(error).toBeInstanceOf(HttpError);
    return error as HttpError;
  }
  throw new Error("se esperaba un error de validación");
}

describe("loadConfig", () => {
  it("aplica los valores por defecto", () => {
    const config = loadConfig(BASE);
    expect(config).toMatchObject({
      NODE_ENV: "development", PORT: 3000, ACCESS_TOKEN_TTL_SECONDS: 900, REFRESH_TOKEN_TTL_DAYS: 7,
      CORS_ORIGINS: [], TRUST_PROXY: false, BCRYPT_COST: 12, MAX_FAILED_LOGINS: 5
    });
  });

  it("separa los orígenes de CORS", () => {
    expect(loadConfig({ ...BASE, CORS_ORIGINS: "https://a.com, https://b.com,," }).CORS_ORIGINS)
      .toEqual(["https://a.com", "https://b.com"]);
  });

  it.each(["true", "1"])("TRUST_PROXY=%s activa el proxy", (value) => {
    expect(loadConfig({ ...BASE, TRUST_PROXY: value }).TRUST_PROXY).toBe(true);
  });

  it("un secreto corto no arranca y no se filtra en el mensaje", () => {
    expect(() => loadConfig({ ...BASE, JWT_SECRET: "corto-secreto" })).toThrow(/JWT_SECRET/);
    expect(() => loadConfig({ ...BASE, JWT_SECRET: "corto-secreto" })).not.toThrow(/corto-secreto/);
  });

  it("falla sin DATABASE_URL", () => {
    expect(() => loadConfig({ JWT_SECRET: BASE.JWT_SECRET })).toThrow(/DATABASE_URL/);
  });

  it.each([["PORT", "abc"], ["BCRYPT_COST", "4"], ["ACCESS_TOKEN_TTL_SECONDS", "86400"]])(
    "rechaza %s=%s", (key, value) => {
      expect(() => loadConfig({ ...BASE, [key]: value })).toThrow(key);
    }
  );
});

describe("esquemas de validación", () => {
  const valido = { email: "  Ana@Correo.MX ", name: " Ana ", password: "Segura12345" };

  it("normaliza correo y nombre y pone AUTONOMO por defecto", () => {
    expect(parse(registerSchema, valido)).toEqual({
      email: "ana@correo.mx", name: "Ana", password: "Segura12345", role: "AUTONOMO"
    });
  });

  it.each([
    ["sin número", "SoloLetrasAqui"],
    ["sin letra", "1234567890"],
    ["corta", "Ab1"],
    ["de más de 72 bytes", "ñ".repeat(40) + "1"]
  ])("rechaza una contraseña %s", (_caso, password) => {
    const error = validationError(() => parse(registerSchema, { ...valido, password }));
    expect(error.status).toBe(400);
    expect(error.details).toEqual(expect.arrayContaining([expect.objectContaining({ field: "password" })]));
  });

  it("rechaza el rol ADMIN y los campos extra", () => {
    expect(validationError(() => parse(registerSchema, { ...valido, role: "ADMIN" })).code).toBe("VALIDATION_ERROR");
    expect(validationError(() => parse(registerSchema, { ...valido, admin: true })).status).toBe(400);
  });

  it("los detalles no repiten el valor enviado", () => {
    const error = validationError(() => parse(loginSchema, { email: "no-es-correo", password: "Secreto1" }));
    expect(JSON.stringify(error.details)).not.toContain("Secreto1");
    expect(JSON.stringify(error.details)).toContain("email");
  });

  it("refreshSchema rechaza tokens cortos", () => {
    expect(validationError(() => parse(refreshSchema, { refreshToken: "corto" })).status).toBe(400);
  });

  it("idSchema acepta un uuid y rechaza inyección", () => {
    const id = randomUUID();
    expect(parse(idSchema, id)).toBe(id);
    expect(validationError(() => parse(idSchema, "1 OR 1=1")).status).toBe(400);
  });
});

describe("repositorios en memoria", () => {
  it("crea, busca, actualiza y lista", async () => {
    const repo = memoryUserRepository();
    const user = await repo.create({ email: "a@b.mx", name: "A", passwordHash: "h", role: "AUTONOMO" });
    expect(await repo.findByEmail("a@b.mx")).toMatchObject({ id: user.id, failedLogins: 0 });
    expect(await repo.findByEmail("x@b.mx")).toBeNull();
    expect(await repo.findById("nada")).toBeNull();
    expect((await repo.update(user.id, { role: "CUIDADOR" })).role).toBe("CUIDADOR");
    expect(await repo.list()).toHaveLength(1);
    await expect(repo.update("nada", { role: "ADMIN" })).rejects.toThrow();
  });

  it("devuelve copias, no referencias", async () => {
    const repo = memoryUserRepository();
    const user = await repo.create({ email: "a@b.mx", name: "A", passwordHash: "h", role: "AUTONOMO" });
    user.role = "ADMIN";
    expect((await repo.findById(user.id))?.role).toBe("AUTONOMO");
  });

  it("revoca por id y por usuario sin tocar a los demás", async () => {
    const repo = memoryRefreshTokenRepository();
    const expiresAt = new Date(Date.now() + 1000);
    const a = await repo.create({ userId: "u1", tokenHash: "a", expiresAt });
    await repo.create({ userId: "u1", tokenHash: "b", expiresAt });
    await repo.create({ userId: "u2", tokenHash: "c", expiresAt });
    await repo.revoke("no-existe", new Date());
    await repo.revoke(a.id, new Date());
    await repo.revokeAllForUser("u1", new Date());
    expect((await repo.findByHash("b"))?.revokedAt).toBeInstanceOf(Date);
    expect((await repo.findByHash("c"))?.revokedAt).toBeNull();
    expect(await repo.findByHash("zzz")).toBeNull();
  });
});

describe("piezas sueltas", () => {
  it("bcryptHasher genera y verifica hashes reales", async () => {
    const hasher = bcryptHasher(10);
    const hash = await hasher.hash("Segura12345");
    expect(hash).toMatch(/^\$2[aby]\$10\$/);
    expect(await hasher.verify("Segura12345", hash)).toBe(true);
    expect(await hasher.verify("otra", hash)).toBe(false);
  });

  it("requireRole sin autenticación previa responde 401", () => {
    let recibido: unknown;
    requireRole("ADMIN")({} as never, {} as never, (error?: unknown) => { recibido = error; });
    expect((recibido as HttpError).status).toBe(401);
  });
});
