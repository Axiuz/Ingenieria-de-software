export const ROLES = ["AUTONOMO", "SUPERVISADO", "CUIDADOR", "ADMIN"] as const;
export type Role = (typeof ROLES)[number];

export const SELF_ASSIGNABLE_ROLES = ["AUTONOMO", "SUPERVISADO", "CUIDADOR"] as const;

export interface Permissions {
  puedeEditar: boolean;
  gestionaPersonas: boolean;
  administraUsuarios: boolean;
}

export const PERMISSIONS: Record<Role, Permissions> = {
  AUTONOMO: { puedeEditar: true, gestionaPersonas: false, administraUsuarios: false },
  SUPERVISADO: { puedeEditar: false, gestionaPersonas: false, administraUsuarios: false },
  CUIDADOR: { puedeEditar: true, gestionaPersonas: true, administraUsuarios: false },
  ADMIN: { puedeEditar: true, gestionaPersonas: true, administraUsuarios: true }
};

export interface User {
  id: string;
  email: string;
  name: string;
  passwordHash: string;
  role: Role;
  failedLogins: number;
  lockedUntil: Date | null;
  createdAt: Date;
}

export type NewUser = Pick<User, "email" | "name" | "passwordHash" | "role">;
export type UserPatch = Partial<Pick<User, "failedLogins" | "lockedUntil" | "role">>;

export interface RefreshTokenRecord {
  id: string;
  userId: string;
  tokenHash: string;
  expiresAt: Date;
  revokedAt: Date | null;
  createdAt: Date;
}

export type NewRefreshToken = Pick<RefreshTokenRecord, "userId" | "tokenHash" | "expiresAt">;

export interface UserRepository {
  findByEmail(email: string): Promise<User | null>;
  findById(id: string): Promise<User | null>;
  create(data: NewUser): Promise<User>;
  update(id: string, patch: UserPatch): Promise<User>;
  list(): Promise<User[]>;
}

export interface RefreshTokenRepository {
  create(data: NewRefreshToken): Promise<RefreshTokenRecord>;
  findByHash(tokenHash: string): Promise<RefreshTokenRecord | null>;
  revoke(id: string, at: Date): Promise<void>;
  revokeAllForUser(userId: string, at: Date): Promise<void>;
}

export interface PublicUser {
  id: string;
  email: string;
  name: string;
  role: Role;
  createdAt: string;
}

export function toPublicUser(user: User): PublicUser {
  return {
    id: user.id,
    email: user.email,
    name: user.name,
    role: user.role,
    createdAt: user.createdAt.toISOString()
  };
}
