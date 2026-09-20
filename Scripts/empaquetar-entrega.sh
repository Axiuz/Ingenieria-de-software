#!/usr/bin/env bash
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
EQUIPO="${1:-TuPastilla}"
DESTINO="$RAIZ/build/entrega-final-$EQUIPO"
ZIP="$RAIZ/build/entrega-final-$EQUIPO.zip"

if [ -n "$(git -C "$RAIZ" status --porcelain)" ]; then
  echo "AVISO: hay cambios sin commitear; el ZIP se arma desde el ultimo commit." >&2
fi

rm -rf "$DESTINO" "$ZIP"
mkdir -p "$DESTINO"/{codigo-fuente,pipeline,reportes,evidencias}

git -C "$RAIZ" archive --format=tar HEAD | tar -x -C "$DESTINO/codigo-fuente"
cp "$RAIZ/.github/workflows/ci-cd.yml" "$DESTINO/pipeline/"
cp -R "$RAIZ/reportes/." "$DESTINO/reportes/"
[ -d "$RAIZ/evidencias" ] && cp -R "$RAIZ/evidencias/." "$DESTINO/evidencias/"
cp "$RAIZ/docs/informe-cierre.docx" "$DESTINO/informe-cierre.docx"
cp "$RAIZ/docs/rubrica.docx" "$DESTINO/como-cumple-la-rubrica.docx"
cp "$RAIZ/docs/plan-mejora.docx" "$DESTINO/plan-mejora.docx"
cp "$RAIZ/README.md" "$DESTINO/README.md"

find "$DESTINO" -name ".env" -delete
find "$DESTINO" -name "local.properties" -delete
find "$DESTINO" -name ".DS_Store" -delete

# La entrega no puede llevar secretos: si aparece una variable con valor, se detiene antes del ZIP.
# Los .example quedan fuera porque llevan las claves vacias a proposito.
if grep -rIl -E '(JWT_SECRET|MYSQL_PASSWORD|MYSQL_ROOT_PASSWORD|ADMIN_PASSWORD)=[^[:space:]]' "$DESTINO" \
   --exclude="*.example" --exclude="*.md" --exclude="*.yml" --exclude="*.sh" | grep -q .; then
  echo "ERROR: hay valores de secretos en la entrega" >&2
  exit 1
fi

(cd "$RAIZ/build" && zip -qr "$ZIP" "$(basename "$DESTINO")")
echo "$ZIP"
unzip -l "$ZIP" | tail -1
