#!/bin/bash
# Script de configuración de variables de entorno para compilación ARM64
set -euo pipefail

echo "========================================="
echo "Configurando variables del entorno Android..."
echo "========================================="

# Detectar ruta predefinida del SDK de Android en contenedores o local
export ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-"/usr/lib/android-sdk"}

# Buscar automáticamente una versión de NDK disponible en el SDK
if [ -d "$ANDROID_SDK_ROOT/ndk" ]; then
    NDK_DIR=$(find "$ANDROID_SDK_ROOT/ndk" -maxdepth 1 -mindepth 1 | head -n 1)
    export ANDROID_NDK_ROOT=${ANDROID_NDK_ROOT:-"$NDK_DIR"}
else
    export ANDROID_NDK_ROOT=${ANDROID_NDK_ROOT:-""}
fi

echo "Ruta ANDROID_SDK_ROOT: ${ANDROID_SDK_ROOT}"
echo "Ruta ANDROID_NDK_ROOT: ${ANDROID_NDK_ROOT}"

if [ ! -d "${ANDROID_SDK_ROOT}" ]; then
    echo "ERROR: No se encontró el SDK de Android en ${ANDROID_SDK_ROOT}"
    exit 1
fi

echo "Variables configuradas correctamente."
