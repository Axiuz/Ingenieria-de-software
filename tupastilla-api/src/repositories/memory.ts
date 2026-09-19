import { randomUUID } from "node:crypto";
import type {
  RefreshTokenRecord,
  RefreshTokenRepository,
  User,
  UserRepository
} from "../domain.js";

/** Repositorio en memoria para pruebas. Devuelve copias para comportarse como una base de datos. */
export function memoryUserRepository(now: () => Date = () => new Date()): UserRepository {
  const users = new Map<string, User>();
  return {
    findByEmail: async (email) => [...users.values()].find((u) => u.email === email) ?? null,
    findById: async (id) => users.get(id) ?? null,
    create: async (data) => {
      const user: User = { id: randomUUID(), failedLogins: 0, lockedUntil: null, createdAt: now(), ...data };
      users.set(user.id, user);
      return { ...user };
    },
    update: async (id, patch) => {
      const current = users.get(id);
      if (!current) throw new Error(`Usuario ${id} no existe`);
      const updated = { ...current, ...patch };
      users.set(id, updated);
      return { ...updated };
    },
    list: async () => [...users.values()].map((u) => ({ ...u }))
  };
}

export function memoryRefreshTokenRepository(now: () => Date = () => new Date()): RefreshTokenRepository {
  const tokens = new Map<string, RefreshTokenRecord>();
  return {
    create: async (data) => {
      const record: RefreshTokenRecord = { id: randomUUID(), revokedAt: null, createdAt: now(), ...data };
      tokens.set(record.id, record);
      return { ...record };
    },
    findByHash: async (hash) => {
      const found = [...tokens.values()].find((t) => t.tokenHash === hash);
      return found ? { ...found } : null;
    },
    revoke: async (id, at) => {
      const current = tokens.get(id);
      if (current) tokens.set(id, { ...current, revokedAt: at });
    },
    revokeAllForUser: async (userId, at) => {
      for (const [id, token] of tokens) {
        if (token.userId === userId && !token.revokedAt) tokens.set(id, { ...token, revokedAt: at });
      }
    }
  };
}
