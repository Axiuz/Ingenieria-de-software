import jwt from "jsonwebtoken";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthService } from "../src/auth/authService.js";
import type { PasswordHasher } from "../src/auth/password.js";
import { hashRefreshToken, TokenService } from "../src/auth/tokens.js";
import { HttpError } from "../src/errors.js";
import { memoryRefreshTokenRepository, memoryUserRepository } from "../src/repositories/memory.js";
import { Clock, fastHasher, TEST_SECRET, VALID_PASSWORD } from "./helpers.js";

const ISSUER = "tupastilla-api";
const AUDIENCE = "tupastilla-app";
const DAY = 24 * 60 * 60 * 1000;

function tokenService(clock = new Clock()) {
  return new TokenService(
    { secret: TEST_SECRET, issuer: ISSUER, audience: AUDIENCE, accessTtlSeconds: 900, refreshTtlDays: 7 },
    clock.now
  );
}

function expectUnauthorized(action: () => unknown) {
  try {
    action();
  } catch (error) {
    expect(error).toBeInstanceOf(HttpError);
    expect((error as HttpError).status).toBe(401);
    return;
  }
  throw new Error("se esperaba un 401");
}

const base64url = (value: object) => Buffer.from(JSON.stringify(value)).toString("base64url");

describe("TokenService", () => {
  const tokens = tokenService();
  const user = { id: "u-1", email: "ana@correo.mx", role: "CUIDADOR" as const };

  it("firma y verifica sub, email y rol", () => {
    const claims = tokens.verifyAccess(tokens.signAccess(user));
    expect(claims).toEqual({ sub: "u-1", email: "ana@correo.mx", role: "CUIDADOR" });
  });

  it("incluye iss, aud, exp y jti", () => {
    const decoded = jwt.decode(tokens.signAccess(user)) as jwt.JwtPayload;
    expect(decoded.iss).toBe(ISSUER);
    expect(decoded.aud).toBe(AUDIENCE);
    expect(decoded.exp! - decoded.iat!).toBe(900);
    expect(decoded.jti).toMatch(/[0-9a-f-]{36}/);
  });

  it("rechaza un token firmado con otro secreto", () => {
    const forged = jwt.sign({ email: user.email, role: "ADMIN" }, "otro-secreto-de-mas-de-32-caracteres!!", {
      subject: "u-1", issuer: ISSUER, audience: AUDIENCE
    });
    expectUnauthorized(() => tokens.verifyAccess(forged));
  });

  it("rechaza alg none", () => {
    const header = base64url({ alg: "none", typ: "JWT" });
    const payload = base64url({ sub: "u-1", email: user.email, role: "ADMIN", iss: ISSUER, aud: AUDIENCE });
    expectUnauthorized(() => tokens.verifyAccess(`${header}.${payload}.`));
  });

  it("rechaza HS512 aunque el secreto sea el correcto", () => {
    const token = jwt.sign({ email: user.email, role: "AUTONOMO" }, TEST_SECRET, {
      algorithm: "HS512", subject: "u-1", issuer: ISSUER, audience: AUDIENCE
    });
    expectUnauthorized(() => tokens.verifyAccess(token));
  });

  it("rechaza otro issuer", () => {
    const token = jwt.sign({ email: user.email, role: "AUTONOMO" }, TEST_SECRET, {
      subject: "u-1", issuer: "otra-api", audience: AUDIENCE
    });
    expectUnauthorized(() => tokens.verifyAccess(token));
  });

  it("rechaza otra audiencia", () => {
    const token = jwt.sign({ email: user.email, role: "AUTONOMO" }, TEST_SECRET, {
      subject: "u-1", issuer: ISSUER, audience: "otra-app"
    });
    expectUnauthorized(() => tokens.verifyAccess(token));
  });

  it("rechaza un token expirado", () => {
    const token = jwt.sign({ email: user.email, role: "AUTONOMO" }, TEST_SECRET, {
      subject: "u-1", issuer: ISSUER, audience: AUDIENCE, expiresIn: -10
    });
    expectUnauthorized(() => tokens.verifyAccess(token));
  });

  it("rechaza un token alterado", () => {
    const [header, , signature] = tokens.signAccess(user).split(".");
    const payload = base64url({ sub: "u-1", email: user.email, role: "ADMIN", iss: ISSUER, aud: AUDIENCE });
    expectUnauthorized(() => tokens.verifyAccess(`${header}.${payload}.${signature}`));
  });

  it("rechaza un rol desconocido", () => {
    const token = jwt.sign({ email: user.email, role: "ROOT" }, TEST_SECRET, {
      subject: "u-1", issuer: ISSUER, audience: AUDIENCE
    });
    expectUnauthorized(() => tokens.verifyAccess(token));
  });

  it("rechaza un payload sin sub", () => {
    const token = jwt.sign({ email: user.email, role: "AUTONOMO" }, TEST_SECRET, {
      issuer: ISSUER, audience: AUDIENCE
    });
    expectUnauthorized(() => tokens.verifyAccess(token));
  });

  it("rechaza un payload que es texto", () => {
    const token = jwt.sign("solo-texto", TEST_SECRET);
    const strict = new TokenService({
      secret: TEST_SECRET, issuer: ISSUER, audience: AUDIENCE, accessTtlSeconds: 900, refreshTtlDays: 7
    });
    expectUnauthorized(() => strict.verifyAccess(token));
  });

  it("genera refresh tokens distintos, con su hash y 7 días de vigencia", () => {
    const clock = new Clock();
    const service = tokenService(clock);
    const a = service.newRefreshToken();
    const b = service.newRefreshToken();
    expect(a.token).not.toBe(b.token);
    expect(a.token.length).toBeGreaterThanOrEqual(43);
    expect(a.hash).toBe(hashRefreshToken(a.token));
    expect(a.hash).toMatch(/^[0-9a-f]{64}$/);
    expect(a.expiresAt.getTime()).toBe(clock.current.getTime() + 7 * DAY);
  });
});

describe("AuthService", () => {
  let clock: Clock;
  let users: ReturnType<typeof memoryUserRepository>;
  let refreshTokens: ReturnType<typeof memoryRefreshTokenRepository>;
  let hasher: PasswordHasher;
  let auth: AuthService;

  const registro = { email: "ana@correo.mx", name: "Ana", password: VALID_PASSWORD, role: "AUTONOMO" as const };

  beforeEach(() => {
    clock = new Clock();
    users = memoryUserRepository(clock.now);
    refreshTokens = memoryRefreshTokenRepository(clock.now);
    hasher = { ...fastHasher };
    auth = new AuthService({
      users, refreshTokens, hasher, tokens: tokenService(clock), maxFailedLogins: 3, lockMs: 60_000, now: clock.now
    });
  });

  async function expectStatus(promise: Promise<unknown>, status: number, code?: string) {
    const error = await promise.then(() => null, (e: unknown) => e);
    expect(error).toBeInstanceOf(HttpError);
    expect((error as HttpError).status).toBe(status);
    if (code) expect((error as HttpError).code).toBe(code);
  }

  it("registra y devuelve una sesión sin el hash", async () => {
    const session = await auth.register(registro);
    expect(session.tokenType).toBe("Bearer");
    expect(session.expiresIn).toBe(900);
    expect(session.user).toMatchObject({ email: "ana@correo.mx", role: "AUTONOMO" });
    expect(JSON.stringify(session)).not.toContain("hashed:");
    expect((await users.findByEmail("ana@correo.mx"))?.passwordHash).toBe(`hashed:${VALID_PASSWORD}`);
  });

  it("rechaza un correo repetido con 409", async () => {
    await auth.register(registro);
    await expectStatus(auth.register(registro), 409, "EMAIL_TAKEN");
  });

  it("inicia sesión con la contraseña correcta", async () => {
    await auth.register(registro);
    const session = await auth.login({ email: registro.email, password: VALID_PASSWORD });
    expect(session.user.email).toBe(registro.email);
  });

  it("con un correo inexistente responde 401 y aun así compara un hash", async () => {
    const verify = vi.spyOn(hasher, "verify");
    await expectStatus(auth.login({ email: "nadie@correo.mx", password: "x" }), 401, "INVALID_CREDENTIALS");
    expect(verify).toHaveBeenCalledTimes(1);
    await expectStatus(auth.login({ email: "nadie@correo.mx", password: "x" }), 401);
  });

  it("suma los fallos y bloquea al llegar al máximo", async () => {
    const { user } = await auth.register(registro);
    await expectStatus(auth.login({ email: registro.email, password: "mala" }), 401);
    expect((await users.findById(user.id))?.failedLogins).toBe(1);
    await expectStatus(auth.login({ email: registro.email, password: "mala" }), 401);
    await expectStatus(auth.login({ email: registro.email, password: "mala" }), 401);
    const locked = await users.findById(user.id);
    expect(locked?.lockedUntil?.getTime()).toBe(clock.current.getTime() + 60_000);
    await expectStatus(auth.login({ email: registro.email, password: VALID_PASSWORD }), 401, "INVALID_CREDENTIALS");
  });

  it("desbloquea al vencer el bloqueo y reinicia los contadores", async () => {
    const { user } = await auth.register(registro);
    for (let i = 0; i < 3; i++) {
      await expectStatus(auth.login({ email: registro.email, password: "mala" }), 401);
    }
    clock.advance(60_001);
    await auth.login({ email: registro.email, password: VALID_PASSWORD });
    const after = await users.findById(user.id);
    expect(after?.failedLogins).toBe(0);
    expect(after?.lockedUntil).toBeNull();
  });

  it("rota el refresh token y detecta su reutilización", async () => {
    const first = await auth.register(registro);
    const second = await auth.refresh(first.refreshToken);
    expect(second.refreshToken).not.toBe(first.refreshToken);
    expect((await refreshTokens.findByHash(hashRefreshToken(first.refreshToken)))?.revokedAt).not.toBeNull();

    await expectStatus(auth.refresh(first.refreshToken), 401);
    expect((await refreshTokens.findByHash(hashRefreshToken(second.refreshToken)))?.revokedAt).not.toBeNull();
    await expectStatus(auth.refresh(second.refreshToken), 401);
  });

  it("rechaza un refresh expirado", async () => {
    const session = await auth.register(registro);
    clock.advance(8 * DAY);
    await expectStatus(auth.refresh(session.refreshToken), 401);
  });

  it("rechaza un refresh desconocido", async () => {
    await expectStatus(auth.refresh("no-existe-este-token-1234567890"), 401);
  });

  it("rechaza un refresh cuyo usuario ya no existe", async () => {
    const token = "token-de-un-usuario-borrado-123456";
    await refreshTokens.create({
      userId: "fantasma", tokenHash: hashRefreshToken(token), expiresAt: new Date(clock.current.getTime() + DAY)
    });
    await expectStatus(auth.refresh(token), 401);
  });

  it("logout revoca el refresh y es idempotente", async () => {
    const session = await auth.register(registro);
    await auth.logout(session.refreshToken);
    await auth.logout(session.refreshToken);
    await expectStatus(auth.refresh(session.refreshToken), 401);
  });

  it("logout de un token desconocido no falla", async () => {
    await expect(auth.logout("desconocido-1234567890123")).resolves.toBeUndefined();
  });

  it("usa el reloj del sistema si no se inyecta uno", async () => {
    const plain = new AuthService({
      users, refreshTokens, hasher, tokens: tokenService(), maxFailedLogins: 3, lockMs: 60_000
    });
    const session = await plain.register(registro);
    await plain.logout(session.refreshToken);
    expect((await refreshTokens.findByHash(hashRefreshToken(session.refreshToken)))?.revokedAt).toBeInstanceOf(Date);
  });
});
