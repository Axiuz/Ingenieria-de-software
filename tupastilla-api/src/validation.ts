import { z } from "zod";
import { ROLES, SELF_ASSIGNABLE_ROLES } from "./domain.js";
import { HttpError } from "./errors.js";

// bcrypt trunca en silencio lo que pase de 72 bytes.
const BCRYPT_MAX_BYTES = 72;

const email = z.string().trim().toLowerCase().pipe(z.email().max(254));

const password = z
  .string()
  .min(10, "Mínimo 10 caracteres")
  .refine((value) => Buffer.byteLength(value, "utf8") <= BCRYPT_MAX_BYTES, "Máximo 72 bytes")
  .refine((value) => /\p{L}/u.test(value), "Debe incluir al menos una letra")
  .refine((value) => /\d/.test(value), "Debe incluir al menos un número");

/** strictObject rechaza campos extra (mass assignment) y el rol solo admite los autoasignables, nunca ADMIN. */
export const registerSchema = z.strictObject({
  email,
  name: z.string().trim().min(1).max(80),
  password,
  role: z.enum(SELF_ASSIGNABLE_ROLES).default("AUTONOMO")
});

export const loginSchema = z.strictObject({
  email,
  password: z.string().min(1).max(128)
});

export const refreshSchema = z.strictObject({
  refreshToken: z.string().min(20).max(200)
});

export const roleChangeSchema = z.strictObject({
  role: z.enum(ROLES)
});

export const idSchema = z.uuid();

export function parse<T extends z.ZodType>(schema: T, data: unknown): z.infer<T> {
  const result = schema.safeParse(data);
  if (!result.success) {
    const details = result.error.issues.map((issue) => ({
      field: issue.path.join("."),
      message: issue.message
    }));
    throw new HttpError(400, "VALIDATION_ERROR", "Datos inválidos", details);
  }
  return result.data;
}
