import { bcryptHasher } from "../src/auth/password.js";
import { loadConfig } from "../src/config.js";
import { createPrismaClient } from "../src/db.js";

const config = loadConfig();
const email = process.env.ADMIN_EMAIL?.trim().toLowerCase();
const password = process.env.ADMIN_PASSWORD;

if (!email || !password || password.length < 12) {
  console.info("Sin ADMIN_EMAIL/ADMIN_PASSWORD (mínimo 12 caracteres): no se crea administrador.");
  process.exit(0);
}

const db = createPrismaClient(config.DATABASE_URL);
const existing = await db.user.findUnique({ where: { email } });
if (existing) {
  console.info(`El administrador ${email} ya existe.`);
} else {
  const passwordHash = await bcryptHasher(config.BCRYPT_COST).hash(password);
  await db.user.create({ data: { email, name: "Administrador", passwordHash, role: "ADMIN" } });
  console.info(`Administrador ${email} creado.`);
}
await db.$disconnect();
