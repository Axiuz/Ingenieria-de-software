import { Router } from "express";
import { loginSchema, parse, refreshSchema, registerSchema } from "../validation.js";
import type { AuthService } from "./authService.js";

export function authRoutes(auth: AuthService): Router {
  const router = Router();

  router.post("/register", async (req, res) => {
    res.status(201).json(await auth.register(parse(registerSchema, req.body)));
  });

  router.post("/login", async (req, res) => {
    res.json(await auth.login(parse(loginSchema, req.body)));
  });

  router.post("/refresh", async (req, res) => {
    res.json(await auth.refresh(parse(refreshSchema, req.body).refreshToken));
  });

  router.post("/logout", async (req, res) => {
    await auth.logout(parse(refreshSchema, req.body).refreshToken);
    res.status(204).end();
  });

  return router;
}
