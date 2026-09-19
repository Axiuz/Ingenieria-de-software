import bcrypt from "bcryptjs";

export interface PasswordHasher {
  hash(plain: string): Promise<string>;
  verify(plain: string, hash: string): Promise<boolean>;
}

export function bcryptHasher(cost: number): PasswordHasher {
  return {
    hash: (plain) => bcrypt.hash(plain, cost),
    verify: (plain, hash) => bcrypt.compare(plain, hash)
  };
}
