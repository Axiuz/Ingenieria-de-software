import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "node",
    include: ["test/**/*.test.ts"],
    coverage: {
      provider: "v8",
      include: ["src/**/*.ts"],
      exclude: ["src/server.ts", "src/db.ts", "src/generated/**", "src/repositories/prisma.ts"],
      reporter: ["text", "html", "lcov", "json-summary"],
      thresholds: { lines: 80, functions: 80, branches: 80, statements: 80 }
    }
  }
});
