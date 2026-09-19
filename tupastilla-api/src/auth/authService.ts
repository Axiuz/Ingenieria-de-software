import {
  toPublicUser,
  type PublicUser,
  type RefreshTokenRepository,
  type Role,
  type User,
  type UserRepository
} from "../domain.js";
import { HttpError, invalidCredentials, unauthorized } from "../errors.js";
import type { PasswordHasher } from "./password.js";
import { hashRefreshToken, type TokenService } from "./tokens.js";

export interface AuthDeps {
  users: UserRepository;
  refreshTokens: RefreshTokenRepository;
  hasher: PasswordHasher;
  tokens: TokenService;
  maxFailedLogins: number;
  lockMs: number;
  now?: () => Date;
}

export interface Session {
  tokenType: "Bearer";
  accessToken: string;
  expiresIn: number;
  refreshToken: string;
  user: PublicUser;
}

export interface RegisterInput {
  email: string;
  name: string;
  password: string;
  role: Exclude<Role, "ADMIN">;
}

export interface LoginInput {
  email: string;
  password: string;
}

export class AuthService {
  private readonly now: () => Date;
  private dummyHash?: Promise<string>;

  constructor(private readonly deps: AuthDeps) {
    this.now = deps.now ?? (() => new Date());
  }

  async register(input: RegisterInput): Promise<Session> {
    if (await this.deps.users.findByEmail(input.email)) {
      throw new HttpError(409, "EMAIL_TAKEN", "Ese correo ya está registrado");
    }
    const user = await this.deps.users.create({
      email: input.email,
      name: input.name,
      role: input.role,
      passwordHash: await this.deps.hasher.hash(input.password)
    });
    return this.issueSession(user);
  }

  /**
   * La contrasena se verifica antes de mirar el bloqueo para que el tiempo de respuesta
   * sea el mismo. Cuenta bloqueada, correo inexistente o contrasena mala dan el mismo 401
   * generico: no se revela si la cuenta existe.
   */
  async login(input: LoginInput): Promise<Session> {
    const user = await this.deps.users.findByEmail(input.email);
    if (!user) {
      await this.deps.hasher.verify(input.password, await this.getDummyHash());
      throw invalidCredentials();
    }
    const matches = await this.deps.hasher.verify(input.password, user.passwordHash);
    if (this.isLocked(user)) throw invalidCredentials();
    if (!matches) {
      await this.registerFailure(user);
      throw invalidCredentials();
    }
    if (user.failedLogins > 0 || user.lockedUntil) {
      await this.deps.users.update(user.id, { failedLogins: 0, lockedUntil: null });
    }
    return this.issueSession(user);
  }

  /**
   * Rotacion: cada refresh se usa una sola vez. Si llega uno ya revocado se asume robo
   * y se revocan todas las sesiones del usuario.
   */
  async refresh(refreshToken: string): Promise<Session> {
    const record = await this.deps.refreshTokens.findByHash(hashRefreshToken(refreshToken));
    if (!record) throw unauthorized("Sesión inválida");
    const now = this.now();
    if (record.revokedAt) {
      await this.deps.refreshTokens.revokeAllForUser(record.userId, now);
      throw unauthorized("Sesión inválida");
    }
    if (record.expiresAt <= now) throw unauthorized("Sesión expirada");
    await this.deps.refreshTokens.revoke(record.id, now);
    const user = await this.deps.users.findById(record.userId);
    if (!user) throw unauthorized("Sesión inválida");
    return this.issueSession(user);
  }

  async logout(refreshToken: string): Promise<void> {
    const record = await this.deps.refreshTokens.findByHash(hashRefreshToken(refreshToken));
    if (record && !record.revokedAt) {
      await this.deps.refreshTokens.revoke(record.id, this.now());
    }
  }

  private isLocked(user: User): boolean {
    return user.lockedUntil !== null && user.lockedUntil > this.now();
  }

  /**
   * Al llegar al maximo de fallos bloquea la cuenta y pone el contador a 0, para que
   * tras el desbloqueo haya otros N intentos.
   */
  private async registerFailure(user: User): Promise<void> {
    const failedLogins = user.failedLogins + 1;
    if (failedLogins >= this.deps.maxFailedLogins) {
      const lockedUntil = new Date(this.now().getTime() + this.deps.lockMs);
      await this.deps.users.update(user.id, { failedLogins: 0, lockedUntil });
    } else {
      await this.deps.users.update(user.id, { failedLogins });
    }
  }

  /**
   * Hash ficticio para comparar cuando el correo no existe: iguala el tiempo de
   * respuesta y evita enumerar correos.
   */
  private getDummyHash(): Promise<string> {
    this.dummyHash ??= this.deps.hasher.hash("tupastilla-dummy-password-0");
    return this.dummyHash;
  }

  private async issueSession(user: User): Promise<Session> {
    const refresh = this.deps.tokens.newRefreshToken();
    await this.deps.refreshTokens.create({
      userId: user.id,
      tokenHash: refresh.hash,
      expiresAt: refresh.expiresAt
    });
    return {
      tokenType: "Bearer",
      accessToken: this.deps.tokens.signAccess(user),
      expiresIn: this.deps.tokens.accessTtlSeconds,
      refreshToken: refresh.token,
      user: toPublicUser(user)
    };
  }
}
