#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCK_FILE="${SCRIPT_DIR}/../third-party/versions.lock"

if [ ! -f "${LOCK_FILE}" ]; then
    echo "ERROR: lockfile not found at ${LOCK_FILE}"
    exit 1
fi

# Load variables
while IFS='=' read -r key value; do
    [[ -z "$key" || "$key" =~ ^# ]] && continue
    key=$(echo "$key" | tr -d '[:space:]')
    value=$(echo "$value" | tr -d '[:space:]')
    export "$key"="$value"
done < "${LOCK_FILE}"

echo "========================================="
echo "Fetching OCCT sources: ${OCCT_TAG}"
echo "========================================="

OCCT_SRC="${SCRIPT_DIR}/../third-party/opencascade/sources"
mkdir -p "${SCRIPT_DIR}/../third-party/opencascade"

if [ ! -d "${OCCT_SRC}/.git" ]; then
    echo "Cloning OpenCASCADE repository at tag ${OCCT_TAG}..."
    git clone --depth 1 --branch "${OCCT_TAG}" https://github.com/Open-Cascade-SAS/OCCT.git "${OCCT_SRC}"
else
    echo "OCCT source directory already exists."
fi

cd "${OCCT_SRC}"
ACTUAL_COMMIT=$(git rev-parse HEAD)
echo "Actual commit: ${ACTUAL_COMMIT}"
echo "Expected commit: ${OCCT_COMMIT}"

if [ "${ACTUAL_COMMIT}" != "${OCCT_COMMIT}" ]; then
    echo "ERROR: Commit mismatch! Expected ${OCCT_COMMIT} but got ${ACTUAL_COMMIT}."
    exit 1
fi

echo "Source verification SUCCESSFUL: Commit matches exactly."
