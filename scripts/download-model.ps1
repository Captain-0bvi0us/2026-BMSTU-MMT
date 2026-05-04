#requires -Version 5.1
<#
    Скачивает MediaPipe Face Landmarker модель в app/src/main/assets.
    Запуск из корня репозитория:
        powershell -ExecutionPolicy Bypass -File scripts/download-model.ps1
#>

$ErrorActionPreference = 'Stop'

$ModelUrl = 'https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/latest/face_landmarker.task'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$AssetsDir = Join-Path $RepoRoot 'app\src\main\assets'
$ModelPath = Join-Path $AssetsDir 'face_landmarker.task'

if (-not (Test-Path $AssetsDir)) {
    New-Item -ItemType Directory -Path $AssetsDir -Force | Out-Null
}

if (Test-Path $ModelPath) {
    $size = (Get-Item $ModelPath).Length
    Write-Host "Модель уже есть: $ModelPath ($size байт). Пропускаю." -ForegroundColor Yellow
    return
}

Write-Host "Скачиваю face_landmarker.task..." -ForegroundColor Cyan
Write-Host "  src: $ModelUrl"
Write-Host "  dst: $ModelPath"

# Включаем TLS 1.2 на старых PowerShell, иначе Google может отвалиться
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12

Invoke-WebRequest -Uri $ModelUrl -OutFile $ModelPath -UseBasicParsing

$size = (Get-Item $ModelPath).Length
Write-Host "OK, $size байт" -ForegroundColor Green
