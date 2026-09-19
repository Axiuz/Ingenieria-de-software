import { Router } from "express";
import {
  PERMISSIONS,
  toPublicUser,
  type RefreshTokenRepository,
  type UserRepository
} from "../domain.js";
import { HttpError, notFound } from "../errors.js";
import { requireAuth, requireRole } from "../auth/middleware.js";
import type { TokenService } from "../auth/tokens.js";
import { idSchema, parse, roleChangeSchema } from "../validation.js";

export interface UserRouteDeps {
  users: UserRepository;
  refreshTokens: RefreshTokenRepository;
  tokens: TokenService;
  now?: () => Date;
}

export function userRoutes(deps: UserRouteDeps): Router {
  const router = Router();
  const now = deps.now ?? (() => new Date());
  router.use(requireAuth(deps.tokens));

  router.get("/me", async (req, res) => {
    const user = await deps.users.findById(req.auth!.sub);
    if (!user) throw notFound("Usuario");
    res.json(toPublicUser(user));
  });

  router.get("/permisos", (req, res) => {
    res.json({ role: req.auth!.role, ...PERMISSIONS[req.auth!.role] });
  });

  router.get("/users", requireRole("ADMIN"), async (_req, res) => {
    res.json((await deps.users.list()).map(toPublicUser));
  });

  // Revoca los refresh del usuario para que vuelva a entrar y reciba un token con el rol
  // nuevo. Un ADMIN no puede cambiar su propio rol para no dejar el sistema sin administrador.
  router.patch("/users/:id/role", requireRole("ADMIN"), async (req, res) => {
    const id = parse(idSchema, req.params.id);
    const { role } = parse(roleChangeSchema, req.body);
    if (id === req.auth!.sub) {
      throw new HttpError(400, "SELF_ROLE_CHANGE", "No puedes cambiar tu propio rol");
    }
    if (!(await deps.users.findById(id))) throw notFound("Usuario");
    const updated = await deps.users.update(id, { role });
    await deps.refreshTokens.revokeAllForUser(id, now());
    res.json(toPublicUser(updated));
  });

  return router;
}
