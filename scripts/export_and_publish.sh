#!/usr/bin/env bash
set -euo pipefail

VERSION=${1:-""}
REPO_URL=${2:-""}

if [ -z "$VERSION" ] || [ -z "$REPO_URL" ]; then
    echo "Uso: $0 <version> <repo_url>"
    exit 1
fi

echo "==> Generando artefactos Maven (v$VERSION)..."
./gradlew :IDDigitalSDK:publishReleasePublicationToLocalDistRepository -PVERSION_NAME="$VERSION"

echo "==> Clonando repositorio destino..."
DIST_DIR=$(mktemp -d)
git clone "$REPO_URL" "$DIST_DIR"

echo "==> Copiando artefactos al repositorio de distribucion..."
cp -R IDDigitalSDK/build/repo/* "$DIST_DIR/"

echo "==> Publicando en el repositorio destino..."
cd "$DIST_DIR"
git add .
git commit -m "Release v$VERSION" || echo "No hay cambios para commitear"
git tag "v$VERSION" || echo "El tag ya existe"
git branch -M main
git push -u origin main
git push origin "v$VERSION"

if command -v gh &> /dev/null; then
    echo "==> Creando GitHub Release..."
    gh release create "v$VERSION" --title "Release v$VERSION" --generate-notes || echo "No se pudo crear el release (¿falta GH_TOKEN o permisos?)"
else
    echo "==> Herramienta 'gh' no encontrada. Saltando creacion de GitHub Release."
fi

echo "==> Proceso finalizado con exito!"
