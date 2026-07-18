#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
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

INSTALL_DIR="${SCRIPT_DIR}/../third-party/opencascade/${ANDROID_ABI}"

echo "========================================="
echo "Verificando artefactos de OpenCASCADE..."
echo "========================================="

# 1. Comprobar cabeceras
HEADER="${INSTALL_DIR}/include/opencascade/Standard.hxx"
if [ ! -f "${HEADER}" ]; then
    echo "ERROR: Cabecera crítica faltante: ${HEADER}"
    exit 1
else
    echo "OK: Cabecera encontrada: Standard.hxx"
fi

# 2. Comprobar bibliotecas fundamentales
REQUIRED_LIBS=(
    "libTKernel.so"
    "libTKMath.so"
    "libTKG2d.so"
    "libTKG3d.so"
    "libTKGeomBase.so"
    "libTKBRep.so"
    "libTKTopAlgo.so"
    "libTKPrim.so"
    "libTKBO.so"
    "libTKMesh.so"
    "libTKXSBase.so"
    "libTKSTEP.so"
    "libTKIGES.so"
)

MISSING=0
for lib in "${REQUIRED_LIBS[@]}"; do
    LIB_PATH="${INSTALL_DIR}/lib/${lib}"
    if [ ! -f "${LIB_PATH}" ]; then
        echo "ERROR: Biblioteca faltante: ${LIB_PATH}"
        MISSING=$((MISSING + 1))
    else
        echo "OK: Biblioteca encontrada: ${lib}"
    fi
done

if [ ${MISSING} -gt 0 ]; then
    echo "ERROR: Faltan ${MISSING} bibliotecas de OpenCASCADE."
    exit 1
fi

# 3. Detectar todas las bibliotecas de OCCT realmente generadas
echo "Bibliotecas realmente generadas en ${INSTALL_DIR}/lib:"
ls -l "${INSTALL_DIR}/lib"/*.so

# 4. Inspeccionar dependencias dinámicas con readelf si está disponible
if command -v readelf &> /dev/null; then
    echo "========================================="
    echo "Analizando dependencias con readelf..."
    echo "========================================="
    for lib in "${REQUIRED_LIBS[@]}"; do
        LIB_PATH="${INSTALL_DIR}/lib/${lib}"
        echo "Dependencias de ${lib}:"
        readelf -d "${LIB_PATH}" | grep NEEDED || true
    done
else
    echo "Aviso: 'readelf' no disponible en el sistema para analizar dependencias."
fi

echo "========================================="
echo "Verificación completada con éxito."
echo "========================================="
