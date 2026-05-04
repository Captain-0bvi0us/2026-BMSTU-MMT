#!/usr/bin/env bash
# Скачивает MediaPipe Face Landmarker модель в app/src/main/assets.
# Запуск из корня репозитория:
#     bash scripts/download-model.sh

set -euo pipefail

MODEL_URL="https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/latest/face_landmarker.task"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSETS_DIR="$REPO_ROOT/app/src/main/assets"
MODEL_PATH="$ASSETS_DIR/face_landmarker.task"

mkdir -p "$ASSETS_DIR"

if [[ -f "$MODEL_PATH" ]]; then
    echo "Модель уже есть: $MODEL_PATH ($(wc -c < "$MODEL_PATH") байт). Пропускаю."
    exit 0
fi

echo "Скачиваю face_landmarker.task..."
echo "  src: $MODEL_URL"
echo "  dst: $MODEL_PATH"

if command -v curl >/dev/null 2>&1; then
    curl -L --fail -o "$MODEL_PATH" "$MODEL_URL"
elif command -v wget >/dev/null 2>&1; then
    wget -O "$MODEL_PATH" "$MODEL_URL"
else
    echo "Нужен curl или wget" >&2
    exit 1
fi

echo "OK, $(wc -c < "$MODEL_PATH") байт"
