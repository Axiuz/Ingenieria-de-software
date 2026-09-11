#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROPS="$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.properties"

distributionUrl=$(sed -n 's/^distributionUrl=//p' "$PROPS" | tr -d '\r')
expectedSha256=$(sed -n 's/^distributionSha256Sum=//p' "$PROPS" | tr -d '\r')

fileName=${distributionUrl##*/}
distributionName=${fileName%-bin.zip}
mavenUserHome=${MAVEN_USER_HOME:-"$HOME/.m2"}
installDir="$mavenUserHome/wrapper/dists/$distributionName"
mavenBin="$installDir/bin/mvn"

if [ ! -x "$mavenBin" ]; then
  echo "Descargando $distributionName por única vez..."

  command -v unzip >/dev/null 2>&1 || {
    echo "Se necesita 'unzip' para la primera ejecución." >&2
    exit 1
  }

  tmpDir=$(mktemp -d)
  trap 'rm -rf "$tmpDir"' EXIT INT TERM
  zipFile="$tmpDir/$fileName"
  extractDir="$tmpDir/extract"
  mkdir -p "$extractDir"

  if command -v curl >/dev/null 2>&1; then
    curl -fL "$distributionUrl" -o "$zipFile"
  elif command -v wget >/dev/null 2>&1; then
    wget "$distributionUrl" -O "$zipFile"
  else
    echo "Se necesita curl o wget para descargar Maven la primera vez." >&2
    exit 1
  fi

  if [ -n "$expectedSha256" ]; then
    if command -v sha256sum >/dev/null 2>&1; then
      actualSha256=$(sha256sum "$zipFile" | awk '{print $1}')
    elif command -v shasum >/dev/null 2>&1; then
      actualSha256=$(shasum -a 256 "$zipFile" | awk '{print $1}')
    else
      echo "No hay herramienta SHA-256 para verificar la descarga." >&2
      exit 1
    fi

    [ "$actualSha256" = "$expectedSha256" ] || {
      echo "El SHA-256 de la distribución Maven no coincide." >&2
      exit 1
    }
  fi

  unzip -q "$zipFile" -d "$extractDir"
  extractedHome="$extractDir/$distributionName"

  [ -x "$extractedHome/bin/mvn" ] || {
    echo "La distribución descargada no contiene bin/mvn." >&2
    exit 1
  }

  mkdir -p "$(dirname "$installDir")"
  rm -rf "$installDir"
  mv "$extractedHome" "$installDir"
fi

exec "$mavenBin" "$@"
