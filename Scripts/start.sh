#!/usr/bin/env bash
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
API="$RAIZ/tupastilla-api"

if [ ! -f "$API/.env" ]; then
  cp "$API/.env.example" "$API/.env"
  secreto="$(openssl rand -base64 48 | tr -d '\n')"
  sed -i.bak "s|^JWT_SECRET=.*|JWT_SECRET=$secreto|" "$API/.env" && rm "$API/.env.bak"
  echo "Creado tupastilla-api/.env con un JWT_SECRET aleatorio."
fi

docker compose -f "$RAIZ/docker-compose.dev.yml" up -d --wait

cd "$API"
pnpm install --frozen-lockfile
set -a; source .env; set +a
pnpm db:generate
pnpm db:migrate
pnpm db:seed
pnpm dev
