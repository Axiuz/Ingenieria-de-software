import { createApp } from "../src/app.js";
import { loadConfig, type Config } from "../src/config.js";
import type { PasswordHasher } from "../src/auth/password.js";
import { memoryRefreshTokenRepository, memoryUserRepository } from "../src/repositories/memory.js";

export const TEST_SECRET = "test-secret-que-tiene-mas-de-32-caracteres!!";

export function testConfig(overrides: Record<string, string> = {}): Config {
  return loadConfig({
    NODE_ENV: "test",
    DATABASE_URL: "mysql://test:test@localhost:3306/test",
    JWT_SECRET: TEST_SECRET,
    AUTH_RATE_LIMIT: "1000",
    ...overrides
  });
}

export const fastHasher: PasswordHasher = {
  hash: async (plain) => `hashed:${plain}`,
  verify: async (plain, hash) => hash === `hashed:${plain}`
};

export class Clock {
  constructor(public current = new Date("2026-09-18T12:00:00Z")) {}
  now = () => new Date(this.current);
  advance(ms: number) {
    this.current = new Date(this.current.getTime() + ms);
  }
}

export function buildTestApp(overrides: Record<string, string> = {}) {
  const clock = new Clock();
  const users = memoryUserRepository(clock.now);
  const refreshTokens = memoryRefreshTokenRepository(clock.now);
  const errors: unknown[] = [];
  const app = createApp({
    config: testConfig(overrides),
    users,
    refreshTokens,
    hasher: fastHasher,
    now: clock.now,
    log: (error) => errors.push(error)
  });
  return { app, users, refreshTokens, clock, errors };
}

export const VALID_PASSWORD = "Segura12345";
