import js from "@eslint/js";
import tseslint from "typescript-eslint";
import globals from "globals";
import security from "eslint-plugin-security";

export default tseslint.config(
  { ignores: ["dist", "coverage", "src/generated", "node_modules"] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  security.configs.recommended,
  {
    languageOptions: { globals: globals.node },
    rules: {
      complexity: ["error", 10],
      "max-depth": ["error", 3],
      "max-params": ["error", 4],
      "no-console": ["warn", { allow: ["error", "info"] }],
      eqeqeq: "error",
      "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_" }]
    }
  }
);
