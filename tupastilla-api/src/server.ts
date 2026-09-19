import { createApp } from "./app.js";
import { loadConfig } from "./config.js";
import { createPrismaClient } from "./db.js";
import { prismaRefreshTokenRepository, prismaUserRepository } from "./repositories/prisma.js";

const config = loadConfig();
const db = createPrismaClient(config.DATABASE_URL);

const app = createApp({
  config,
  users: prismaUserRepository(db),
  refreshTokens: prismaRefreshTokenRepository(db)
});

const server = app.listen(config.PORT, () => {
  console.info(`tupastilla-api escuchando en el puerto ${config.PORT}`);
});

function shutdown() {
  server.close(() => {
    void db.$disconnect().finally(() => process.exit(0));
  });
}

process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);
