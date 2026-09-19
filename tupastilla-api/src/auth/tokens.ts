import { createHash, randomBytes, randomUUID } from "node:crypto";
import jwt from "jsonwebtoken";
import { ROLES, type Role, type User } from "../domain.js";
import { unauthorized } from "../errors.js";

export interface AccessClaims {
  sub: string;
  email: string;
  role: Role;
}

export interface TokenOptions {
  secret: string;
  issuer: string;
  audience: string;
  accessTtlSeconds: number;
  refreshTtlDays: number;
}

export interface IssuedRefreshToken {
  token: string;
  hash: string;
  expiresAt: Date;
}

const ALGORITHM = "HS256";
const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * SHA-256 en hex del refresh token. En la base solo se guarda este hash,
 * asi que una fuga de la tabla no da tokens usables.
 */
export function hashRefreshToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}

/**
 * Firma y verifica los access token JWT. El algoritmo va fijado a HS256 en ambos
 * sentidos para bloquear `alg: none` y la confusion de algoritmos.
 */
export class TokenService {
  constructor(
    private readonly options: TokenOptions,
    private readonly now: () => Date = () => new Date()
  ) {}

  get accessTtlSeconds(): number {
    return this.options.accessTtlSeconds;
  }

  signAccess(user: Pick<User, "id" | "email" | "role">): string {
    return jwt.sign({ email: user.email, role: user.role }, this.options.secret, {
      algorithm: ALGORITHM,
      subject: user.id,
      issuer: this.options.issuer,
      audience: this.options.audience,
      expiresIn: this.options.accessTtlSeconds,
      jwtid: randomUUID()
    });
  }

  /** Solo acepta HS256 y claims con un rol de la lista; cualquier otra cosa es 401. */
  verifyAccess(token: string): AccessClaims {
    let payload: string | jwt.JwtPayload;
    try {
      payload = jwt.verify(token, this.options.secret, {
        algorithms: [ALGORITHM],
        issuer: this.options.issuer,
        audience: this.options.audience
      });
    } catch {
      throw unauthorized("Token inválido o expirado");
    }
    return parseClaims(payload);
  }

  /**
   * El refresh es aleatorio y opaco, no un JWT: solo vale si su hash esta en la base
   * y se puede revocar en cualquier momento.
   */
  newRefreshToken(): IssuedRefreshToken {
    const token = randomBytes(32).toString("base64url");
    return {
      token,
      hash: hashRefreshToken(token),
      expiresAt: new Date(this.now().getTime() + this.options.refreshTtlDays * DAY_MS)
    };
  }
}

/** Un token bien firmado pero con un rol desconocido se rechaza igual. */
function parseClaims(payload: string | jwt.JwtPayload): AccessClaims {
  if (typeof payload === "string") throw unauthorized("Token inválido o expirado");
  const { sub, email, role } = payload as Record<string, unknown>;
  const valid =
    typeof sub === "string" &&
    typeof email === "string" &&
    typeof role === "string" &&
    (ROLES as readonly string[]).includes(role);
  if (!valid) throw unauthorized("Token inválido o expirado");
  return { sub, email, role: role as Role };
}
