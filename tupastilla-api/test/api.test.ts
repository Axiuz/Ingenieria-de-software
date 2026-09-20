import { randomUUID } from "node:crypto";
import jwt from "jsonwebtoken";
import request from "supertest";
import { describe, expect, it, vi } from "vitest";
import type { Role } from "../src/domain.js";
import { buildTestApp, TEST_SECRET, VALID_PASSWORD } from "./helpers.js";

type TestApp = ReturnType<typeof buildTestApp>;

async function registrar(t: TestApp, email: string, role: Exclude<Role, "ADMIN"> = "AUTONOMO") {
  const res = await request(t.app)
    .post("/api/auth/register")
    .send({ email, name: "Prueba", password: VALID_PASSWORD, role });
  expect(res.status).toBe(201);
  return res.body as { accessToken: string; refreshToken: string; user: { id: string } };
}

async function admin(t: TestApp) {
  const { user } = await registrar(t, "admin@correo.mx");
  await t.users.update(user.id, { role: "ADMIN" });
  const res = await request(t.app).post("/api/auth/login").send({ email: "admin@correo.mx", password: VALID_PASSWORD });
  return { token: res.body.accessToken as string, id: user.id };
}

const bearer = (token: string) => ({ Authorization: `Bearer ${token}` });

describe("infraestructura HTTP", () => {
  it("GET /health responde ok", async () => {
    const res = await request(buildTestApp().app).get("/health");
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ status: "ok" });
  });

  it("una ruta desconocida da 404", async () => {
    const res = await request(buildTestApp().app).get("/api/no-existe").set(bearer("x.y.z"));
    expect(res.status).toBe(401);
    const publica = await request(buildTestApp().app).get("/nada");
    expect(publica.status).toBe(404);
    expect(publica.body.error.code).toBe("NOT_FOUND");
  });

  it("no expone x-powered-by y manda cabeceras de helmet", async () => {
    const res = await request(buildTestApp().app).get("/health");
    expect(res.headers["x-powered-by"]).toBeUndefined();
    expect(res.headers["x-content-type-options"]).toBe("nosniff");
    expect(res.headers["strict-transport-security"]).toBeDefined();
  });

  it("CORS cerrado por defecto y abierto solo a los orígenes configurados", async () => {
    const cerrado = await request(buildTestApp().app).get("/health").set("Origin", "https://malo.com");
    expect(cerrado.headers["access-control-allow-origin"]).toBeUndefined();
    const t = buildTestApp({ CORS_ORIGINS: "https://tupastilla.mx" });
    const permitido = await request(t.app).get("/health").set("Origin", "https://tupastilla.mx");
    expect(permitido.headers["access-control-allow-origin"]).toBe("https://tupastilla.mx");
    const ajeno = await request(t.app).get("/health").set("Origin", "https://malo.com");
    expect(ajeno.headers["access-control-allow-origin"]).toBeUndefined();
  });

  it("JSON malformado da 400 INVALID_JSON", async () => {
    const res = await request(buildTestApp().app)
      .post("/api/auth/login").set("Content-Type", "application/json").send('{"email":');
    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe("INVALID_JSON");
  });

  it("un cuerpo de más de 10 kB da 413", async () => {
    const res = await request(buildTestApp().app)
      .post("/api/auth/login").send({ email: "a@b.mx", password: "x".repeat(11_000) });
    expect(res.status).toBe(413);
    expect(res.body.error.code).toBe("PAYLOAD_TOO_LARGE");
  });

  it("un error inesperado da 500 genérico y se registra", async () => {
    const t = buildTestApp();
    const { accessToken } = await registrar(t, "ana@correo.mx");
    vi.spyOn(t.users, "findById").mockRejectedValue(new Error("db caída en 10.0.0.5"));
    const res = await request(t.app).get("/api/me").set(bearer(accessToken));
    expect(res.status).toBe(500);
    expect(res.body).toEqual({ error: { code: "INTERNAL", message: "Error interno" } });
    expect(JSON.stringify(res.body)).not.toContain("10.0.0.5");
    expect(t.errors).toHaveLength(1);
  });
});

describe("registro e inicio de sesión", () => {
  it("registra y no expone el hash", async () => {
    const t = buildTestApp();
    const res = await request(t.app)
      .post("/api/auth/register").send({ email: " Ana@Correo.MX ", name: "Ana", password: VALID_PASSWORD });
    expect(res.status).toBe(201);
    expect(res.body.user).toMatchObject({ email: "ana@correo.mx", role: "AUTONOMO" });
    expect(res.body.user.passwordHash).toBeUndefined();
    expect(JSON.stringify(res.body)).not.toContain(VALID_PASSWORD);
  });

  it("no permite registrarse como ADMIN", async () => {
    const res = await request(buildTestApp().app)
      .post("/api/auth/register").send({ email: "x@correo.mx", name: "X", password: VALID_PASSWORD, role: "ADMIN" });
    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  it("rechaza campos extra (mass assignment)", async () => {
    const res = await request(buildTestApp().app)
      .post("/api/auth/register")
      .send({ email: "x@correo.mx", name: "X", password: VALID_PASSWORD, isAdmin: true, failedLogins: -99 });
    expect(res.status).toBe(400);
  });

  it("rechaza contraseñas débiles", async () => {
    const res = await request(buildTestApp().app)
      .post("/api/auth/register").send({ email: "x@correo.mx", name: "X", password: "solo-letras" });
    expect(res.status).toBe(400);
    expect(res.body.error.details[0].field).toBe("password");
  });

  it("trata la inyección SQL como texto y no autentica", async () => {
    const t = buildTestApp();
    await registrar(t, "ana@correo.mx");
    const inyeccion = await request(t.app).post("/api/auth/login").send({ email: "' OR 1=1 --", password: "x" });
    expect(inyeccion.status).toBe(400);
    const enPassword = await request(t.app)
      .post("/api/auth/login").send({ email: "ana@correo.mx", password: "' OR '1'='1" });
    expect(enPassword.status).toBe(401);
  });

  it("rechaza operadores tipo NoSQL en lugar de texto", async () => {
    const res = await request(buildTestApp().app)
      .post("/api/auth/login").send({ email: "ana@correo.mx", password: { $ne: null } });
    expect(res.status).toBe(400);
  });

  it("no distingue correo inexistente de contraseña mala", async () => {
    const t = buildTestApp();
    await registrar(t, "ana@correo.mx");
    const noExiste = await request(t.app).post("/api/auth/login").send({ email: "nadie@correo.mx", password: "Mala123456" });
    const mala = await request(t.app).post("/api/auth/login").send({ email: "ana@correo.mx", password: "Mala123456" });
    expect(noExiste.status).toBe(401);
    expect(mala.status).toBe(noExiste.status);
    expect(mala.body).toEqual(noExiste.body);
  });

  it("limita los intentos en /api/auth", async () => {
    const t = buildTestApp({ AUTH_RATE_LIMIT: "3" });
    const intento = () => request(t.app).post("/api/auth/login").send({ email: "a@correo.mx", password: "x" });
    for (let i = 0; i < 3; i++) expect((await intento()).status).toBe(401);
    const bloqueado = await intento();
    expect(bloqueado.status).toBe(429);
    expect(bloqueado.body.error.code).toBe("RATE_LIMITED");
  });

  it("refresh y logout por HTTP", async () => {
    const t = buildTestApp();
    const { refreshToken } = await registrar(t, "ana@correo.mx");
    const renovado = await request(t.app).post("/api/auth/refresh").send({ refreshToken });
    expect(renovado.status).toBe(200);
    const salida = await request(t.app).post("/api/auth/logout").send({ refreshToken: renovado.body.refreshToken });
    expect(salida.status).toBe(204);
    const despues = await request(t.app).post("/api/auth/refresh").send({ refreshToken: renovado.body.refreshToken });
    expect(despues.status).toBe(401);
  });
});

describe("rutas protegidas y roles", () => {
  it("/api/me devuelve al usuario con un token válido", async () => {
    const t = buildTestApp();
    const { accessToken } = await registrar(t, "ana@correo.mx", "CUIDADOR");
    const res = await request(t.app).get("/api/me").set(bearer(accessToken));
    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({ email: "ana@correo.mx", role: "CUIDADOR" });
  });

  it.each([
    ["sin cabecera", undefined],
    ["con esquema Basic", "Basic YWRtaW46YWRtaW4="],
    ["con basura", "Bearer basura"],
    ["con token de otro secreto", `Bearer ${jwt.sign({ email: "a@b.mx", role: "ADMIN" }, "x".repeat(40), { subject: "1" })}`]
  ])("/api/me responde 401 %s", async (_caso, header) => {
    const req = request(buildTestApp().app).get("/api/me");
    const res = header ? await req.set("Authorization", header) : await req;
    expect(res.status).toBe(401);
  });

  it("/api/me da 404 si el usuario del token ya no existe", async () => {
    const token = jwt.sign({ email: "fantasma@correo.mx", role: "AUTONOMO" }, TEST_SECRET, {
      subject: randomUUID(), issuer: "tupastilla-api", audience: "tupastilla-app", expiresIn: 60
    });
    const res = await request(buildTestApp().app).get("/api/me").set(bearer(token));
    expect(res.status).toBe(404);
  });

  it.each([
    ["AUTONOMO", { puedeEditar: true, gestionaPersonas: false }],
    ["SUPERVISADO", { puedeEditar: false, gestionaPersonas: false }],
    ["CUIDADOR", { puedeEditar: true, gestionaPersonas: true }]
  ] as const)("/api/permisos para %s", async (role, esperado) => {
    const t = buildTestApp();
    const { accessToken } = await registrar(t, "ana@correo.mx", role);
    const res = await request(t.app).get("/api/permisos").set(bearer(accessToken));
    expect(res.body).toMatchObject({ role, ...esperado, administraUsuarios: false });
  });

  it("GET /api/users es solo para ADMIN", async () => {
    const t = buildTestApp();
    const { accessToken } = await registrar(t, "ana@correo.mx", "CUIDADOR");
    expect((await request(t.app).get("/api/users").set(bearer(accessToken))).status).toBe(403);
    const { token } = await admin(t);
    const res = await request(t.app).get("/api/users").set(bearer(token));
    expect(res.status).toBe(200);
    expect(res.body).toHaveLength(2);
    expect(res.body[0].passwordHash).toBeUndefined();
  });

  it("un token con rol ADMIN falsificado no pasa", async () => {
    const t = buildTestApp();
    const { accessToken } = await registrar(t, "ana@correo.mx");
    const [header, payload, firma] = accessToken.split(".");
    const claims = JSON.parse(Buffer.from(payload!, "base64url").toString());
    const alterado = Buffer.from(JSON.stringify({ ...claims, role: "ADMIN" })).toString("base64url");
    const res = await request(t.app).get("/api/users").set(bearer(`${header}.${alterado}.${firma}`));
    expect(res.status).toBe(401);
  });

  it("ADMIN cambia un rol y el usuario pierde sus sesiones", async () => {
    const t = buildTestApp();
    const ana = await registrar(t, "ana@correo.mx");
    const { token } = await admin(t);
    const res = await request(t.app)
      .patch(`/api/users/${ana.user.id}/role`).set(bearer(token)).send({ role: "SUPERVISADO" });
    expect(res.status).toBe(200);
    expect(res.body.role).toBe("SUPERVISADO");
    const refresh = await request(t.app).post("/api/auth/refresh").send({ refreshToken: ana.refreshToken });
    expect(refresh.status).toBe(401);
  });

  it("ADMIN no puede cambiar su propio rol", async () => {
    const t = buildTestApp();
    const { token, id } = await admin(t);
    const res = await request(t.app).patch(`/api/users/${id}/role`).set(bearer(token)).send({ role: "AUTONOMO" });
    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe("SELF_ROLE_CHANGE");
  });

  it.each([
    ["id que no es uuid", "1 OR 1=1", { role: "CUIDADOR" }, 400],
    ["uuid inexistente", randomUUID(), { role: "CUIDADOR" }, 404],
    ["rol inválido", randomUUID(), { role: "ROOT" }, 400]
  ])("PATCH de rol con %s", async (_caso, id, body, status) => {
    const t = buildTestApp();
    const { token } = await admin(t);
    const res = await request(t.app).patch(`/api/users/${encodeURIComponent(id)}/role`).set(bearer(token)).send(body);
    expect(res.status).toBe(status);
  });

  it("un no ADMIN no puede cambiar roles, ni el suyo", async () => {
    const t = buildTestApp();
    const ana = await registrar(t, "ana@correo.mx");
    const res = await request(t.app)
      .patch(`/api/users/${ana.user.id}/role`).set(bearer(ana.accessToken)).send({ role: "ADMIN" });
    expect(res.status).toBe(403);
  });
});

describe("cabecera Cache-Control", () => {
  it.each([
    ["/health", 200],
    ["/nada", 404],
    ["/api/me", 401]
  ])("GET %s responde %i con no-store", async (ruta, status) => {
    const res = await request(buildTestApp().app).get(ruta);
    expect(res.status).toBe(status);
    expect(res.headers["cache-control"]).toBe("no-store");
  });

  it("es no-store en la respuesta de login que lleva los tokens", async () => {
    const t = buildTestApp();
    await registrar(t, "cache@correo.mx");
    const res = await request(t.app).post("/api/auth/login").send({ email: "cache@correo.mx", password: VALID_PASSWORD });
    expect(res.status).toBe(200);
    expect(res.body.accessToken).toBeDefined();
    expect(res.headers["cache-control"]).toBe("no-store");
  });

  it("es no-store en un 400 por JSON inválido", async () => {
    const res = await request(buildTestApp().app)
      .post("/api/auth/login")
      .set("Content-Type", "application/json")
      .send("{mal");
    expect(res.status).toBe(400);
    expect(res.headers["cache-control"]).toBe("no-store");
  });
});
