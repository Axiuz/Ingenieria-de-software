import { PrismaMariaDb } from "@prisma/adapter-mariadb";
import { PrismaClient } from "./generated/prisma/client.js";

export function createPrismaClient(databaseUrl: string): PrismaClient {
  return new PrismaClient({ adapter: new PrismaMariaDb(databaseUrl) });
}
