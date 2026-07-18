#!/bin/bash
# Script de configuración de variables de entorno para compilación ARM64
set -euo pipefail

echo "========================================="
echo "Configurando variables del entorno Android..."
echo "========================================="

# Detectar ruta predefinida del SDK de Android
if [ -d "/opt/android/sdk" ]; then
    export ANDROID_SDK_ROOT="/opt/android/sdk"
else
    export ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-"/usr/lib/android-sdk"}
fi

# Buscar automáticamente una versión de NDK disponible en el SDK
if [ -d "$ANDROID_SDK_ROOT/ndk" ]; then
    NDK_DIR=$(find "$ANDROID_SDK_ROOT/ndk" -maxdepth 1 -mindepth 1 | head -n 1)
    export ANDROID_NDK_ROOT=${ANDROID_NDK_ROOT:-"$NDK_DIR"}
else
    export ANDROID_NDK_ROOT=${ANDROID_NDK_ROOT:-""}
fi

# Añadir herramientas de CMake y Ninja del SDK al PATH si están presentes
if [ -d "$ANDROID_SDK_ROOT/cmake" ]; then
    CMAKE_BIN_DIR=$(find "$ANDROID_SDK_ROOT/cmake" -name "cmake" -path "*/bin/cmake" | head -n 1 | xargs dirname)
    if [ -n "$CMAKE_BIN_DIR" ] && [ -d "$CMAKE_BIN_DIR" ]; then
        export PATH="${CMAKE_BIN_DIR}:${PATH}"
        echo "Añadido CMake/Ninja al PATH desde: ${CMAKE_BIN_DIR}"
    fi
fi

echo "Ruta ANDROID_SDK_ROOT: ${ANDROID_SDK_ROOT}"
echo "Ruta ANDROID_NDK_ROOT: ${ANDROID_NDK_ROOT}"

if [ ! -d "${ANDROID_SDK_ROOT}" ]; then
    echo "ERROR: No se encontró el SDK de Android en ${ANDROID_SDK_ROOT}"
    exit 1
fi

if [ -z "${ANDROID_NDK_ROOT}" ] || [ ! -d "${ANDROID_NDK_ROOT}" ]; then
    echo "ERROR: No se encontró el NDK de Android"
    exit 1
fi

echo "Variables configuradas correctamente."
