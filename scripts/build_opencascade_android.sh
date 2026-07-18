#!/bin/bash
# Script para descargar y compilar OpenCASCADE para la arquitectura ARM64-v8a de Android
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 1. Cargar variables de versiones
LOCK_FILE="${SCRIPT_DIR}/../third-party/versions.lock"
if [ ! -f "${LOCK_FILE}" ]; then
    echo "ERROR: lockfile no encontrado en ${LOCK_FILE}"
    exit 1
fi

while IFS='=' read -r key value; do
    [[ -z "$key" || "$key" =~ ^# ]] && continue
    key=$(echo "$key" | tr -d '[:space:]')
    value=$(echo "$value" | tr -d '[:space:]')
    export "$key"="$value"
done < "${LOCK_FILE}"

# 2. Configurar entorno Android
source "${SCRIPT_DIR}/setup_android_env.sh"

# Asegurar que ANDROID_NDK_HOME esté definido
export ANDROID_NDK_HOME="${ANDROID_NDK_ROOT}"

echo "========================================="
echo "Validando requisitos de herramientas..."
echo "========================================="

# Validar variables de entorno requeridas
if [ -z "${ANDROID_SDK_ROOT}" ] || [ ! -d "${ANDROID_SDK_ROOT}" ]; then
    echo "ERROR: ANDROID_SDK_ROOT no está definido o no es un directorio válido."
    exit 1
fi

if [ -z "${ANDROID_NDK_HOME}" ] || [ ! -d "${ANDROID_NDK_HOME}" ]; then
    echo "ERROR: ANDROID_NDK_HOME no está definido o no es un directorio válido."
    exit 1
fi

# Validar ejecutables en PATH
for cmd in cmake ninja git; do
    if ! command -v "$cmd" &> /dev/null; then
        echo "ERROR: No se encontró el comando '$cmd' en el PATH."
        exit 1
    fi
done

echo "Todas las herramientas y variables validadas correctamente."

ABI="${ANDROID_ABI}"
MIN_SDK="${ANDROID_API_LEVEL}"
BUILD_DIR="${SCRIPT_DIR}/../build_occt_${ABI}"
INSTALL_DIR="${SCRIPT_DIR}/../third-party/opencascade/${ABI}"
OCCT_SRC="${SCRIPT_DIR}/../third-party/opencascade/sources"

# Descargar y verificar fuentes
"${SCRIPT_DIR}/fetch_native_sources.sh"

mkdir -p "${BUILD_DIR}"
cd "${BUILD_DIR}"

echo "========================================="
echo "Configurando CMake para OpenCASCADE (${ABI})..."
echo "========================================="

cmake "${OCCT_SRC}" \
  -G "Ninja" \
  -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="${ABI}" \
  -DANDROID_NATIVE_API_LEVEL="${MIN_SDK}" \
  -DANDROID_STL="c++_shared" \
  -DCMAKE_BUILD_TYPE="Release" \
  -DBUILD_LIBRARY_TYPE="Shared" \
  -DCMAKE_INSTALL_PREFIX="${INSTALL_DIR}" \
  -DBUILD_MODULE_Draw="OFF" \
  -DBUILD_MODULE_Visualization="OFF" \
  -DBUILD_MODULE_ApplicationFramework="OFF" \
  -DBUILD_MODULE_FoundationClasses="ON" \
  -DBUILD_MODULE_ModelingData="ON" \
  -DBUILD_MODULE_ModelingAlgorithms="ON" \
  -DBUILD_MODULE_DataExchange="ON" \
  -DUSE_FREETYPE="OFF" \
  -DUSE_GLES2="OFF" \
  -DUSE_FREEIMAGE="OFF" \
  -DUSE_RAPIDJSON="OFF" \
  -DUSE_TBB="OFF"

echo "========================================="
echo "Compilando OpenCASCADE con Ninja..."
echo "========================================="
ninja

echo "========================================="
echo "Instalando cabeceras y bibliotecas..."
echo "========================================="
ninja install

echo "========================================="
echo "Compilación e instalación completadas en: ${INSTALL_DIR}"
echo "========================================="
