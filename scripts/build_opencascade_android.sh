#!/bin/bash
# Script para descargar y compilar OpenCASCADE para la arquitectura ARM64-v8a de Android
set -euo pipefail

# Importar variables de entorno de Android
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/setup_android_env.sh"

echo "========================================="
echo "Iniciando compilación cruzada de OpenCASCADE..."
echo "========================================="

ABI="arm64-v8a"
MIN_SDK="24"
BUILD_DIR="${SCRIPT_DIR}/../build_occt_${ABI}"
INSTALL_DIR="${SCRIPT_DIR}/../third-party/opencascade/${ABI}"

# Verificar si el NDK está configurado correctamente
if [ -z "${ANDROID_NDK_ROOT}" ] || [ ! -d "${ANDROID_NDK_ROOT}" ]; then
    echo "ERROR: ANDROID_NDK_ROOT no está definido o es una ruta inválida."
    exit 1
fi

# Descargar código fuente de OpenCASCADE si no existe
OCCT_SRC="${SCRIPT_DIR}/../third-party/opencascade/sources"
if [ ! -d "${OCCT_SRC}" ]; then
    echo "Descargando código fuente oficial de OpenCASCADE (versión ligera BRep)..."
    mkdir -p "${SCRIPT_DIR}/../third-party/opencascade"
    git clone --depth 1 https://github.com/Open-Cascade-SAS/OCCT.git "${OCCT_SRC}"
fi

mkdir -p "${BUILD_DIR}"
cd "${BUILD_DIR}"

echo "Configurando CMake con la cadena de herramientas (toolchain) de Android NDK..."
cmake "${OCCT_SRC}" \
  -G "Ninja" \
  -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK_ROOT}/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="${ABI}" \
  -DANDROID_NATIVE_API_LEVEL="${MIN_SDK}" \
  -DANDROID_STL="c++_shared" \
  -DBUILD_LIBRARY_TYPE="Shared" \
  -DCMAKE_INSTALL_PREFIX="${INSTALL_DIR}" \
  -DBUILD_MODULE_Draw="OFF" \
  -DBUILD_MODULE_Visualization="OFF" \
  -DBUILD_MODULE_ApplicationFramework="OFF"

echo "Ejecutando compilación cruzada con Ninja..."
ninja

echo "Instalando cabeceras y bibliotecas empaquetadas..."
ninja install

echo "========================================="
echo "Compilación exitosa. Artefactos guardados en: ${INSTALL_DIR}"
echo "========================================="
