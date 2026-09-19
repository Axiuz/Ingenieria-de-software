import type { PrismaClient } from "../generated/prisma/client.js";
import type {
  NewRefreshToken,
  NewUser,
  RefreshTokenRecord,
  RefreshTokenRepository,
  User,
  UserPatch,
  UserRepository
} from "../domain.js";

export function prismaUserRepository(db: PrismaClient): UserRepository {
  return {
    findByEmail: (email) => db.user.findUnique({ where: { email } }) as Promise<User | null>,
    findById: (id) => db.user.findUnique({ where: { id } }) as Promise<User | null>,
    create: (data: NewUser) => db.user.create({ data }) as Promise<User>,
    update: (id, patch: UserPatch) => db.user.update({ where: { id }, data: patch }) as Promise<User>,
    list: () => db.user.findMany({ orderBy: { createdAt: "asc" } }) as Promise<User[]>
  };
}

export function prismaRefreshTokenRepository(db: PrismaClient): RefreshTokenRepository {
  return {
    create: (data: NewRefreshToken) =>
      db.refreshToken.create({ data }) as Promise<RefreshTokenRecord>,
    findByHash: (tokenHash) => db.refreshToken.findUnique({ where: { tokenHash } }),
    revoke: async (id, at) => {
      await db.refreshToken.update({ where: { id }, data: { revokedAt: at } });
    },
    revokeAllForUser: async (userId, at) => {
      await db.refreshToken.updateMany({ where: { userId, revokedAt: null }, data: { revokedAt: at } });
    }
  };
}
