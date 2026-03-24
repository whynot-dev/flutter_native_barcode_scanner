# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Flutter plugin (`native_barcode_scanner`) providing real-time barcode, text, and MRZ scanning using native platform APIs. Published on pub.dev by Freedelity.

- **Android:** Google ML Kit (barcode + text recognition) + CameraX
- **iOS:** AVFoundation (native metadata output, no external SDKs)
- **Text/MRZ scanning is Android-only** — iOS only supports barcode detection

## Common Commands

```bash
# Analyze Dart code
cd example && flutter analyze

# Run example app (requires connected device/emulator)
cd example && flutter run

# Dry-run publish check
flutter pub publish --dry-run

# Build Android AAR
cd example && flutter build apk

# Build iOS (requires macOS)
cd example && flutter build ios --no-codesign
```

There are no unit tests in this project.

## Architecture

### Three-Channel Bridge Pattern

The plugin uses three Flutter platform channels for Dart ↔ native communication:

| Channel | Type | ID | Purpose |
|---------|------|----|---------|
| Method | `MethodChannel` | `be.freedelity/native_scanner/method` | Imperative commands (toggleTorch, flipCamera, stopScanner, startScanner, closeCamera) |
| Event | `EventChannel` | `be.freedelity/native_scanner/imageStream` | Streaming detection results (barcode, text, MRZ) + MRZ progress |
| View | PlatformView | `be.freedelity/native_scanner/view` | Native camera preview embedding (avoids frame copying overhead) |

### Dart Layer (lib/)

- `barcode_scanner.dart` — Enums (`BarcodeFormat`, `ScannerType`, `CameraSelector`, `CameraOrientation`), `Barcode` model, `BarcodeScanner` static methods for camera control
- `barcode_scanner.widget.dart` — `BarcodeScannerWidget` using `PlatformViewLink`/`AndroidViewSurface` (Android) and `UiKitView` (iOS)

### Android Native (android/src/main/kotlin/be/freedelity/barcode_scanner/)

Four-class architecture mirroring Flutter's platform view pattern:
- `BarcodeScannerPlugin` — FlutterPlugin + ActivityAware entry point
- `BarcodeScannerViewFactory` — Creates platform views
- `BarcodeScannerView` — PlatformView wrapper, handles camera permissions
- `BarcodeScannerController` — Core logic: CameraX lifecycle, ML Kit image analysis, frame debouncing (2s barcode, 500ms text, 100ms MRZ), autofocus configuration

Utilities:
- `util/MrzUtil.kt` — MRZ parsing for TD1 (3×30), TD2 (2×36), and passport (2×44) formats with checksum validation
- `util/BarcodeScannerUtil.kt` — YUV_420_888 → ARGB_8888 bitmap conversion (integer-only, no floats)

### iOS Native (ios/Classes/)

Same four-class pattern, Swift implementation:
- `BarcodeScannerPlugin.swift` — FlutterPlugin entry point
- `BarcodeScannerViewFactory.swift` — Creates platform views
- `BarcodeScannerView.swift` — FlutterPlatformView wrapper, permission handling
- `BarcodeScannerController.swift` — AVCaptureSession + AVCaptureMetadataOutput for barcode detection

### Platform Feature Parity

| Feature | Android | iOS |
|---------|---------|-----|
| Barcode scanning | 11 formats (incl. Codabar, UPC-A) | 9 formats (no Codabar, no UPC-A) |
| Text recognition | Yes (ML Kit) | No |
| MRZ scanning | Yes (ML Kit + MrzUtil) | No |
| Camera orientation param | Ignored (auto-detects) | Required |

### Event Message Formats

Barcode: `{"barcode": String, "format": int}` — format maps to `BarcodeFormat` enum index (0-10)
Text: `{"text": String}`
MRZ: `{"mrz": String, "img": Uint8List}` (PNG bytes of cropped document)
Progress: `{"progress": int}` (MRZ only: 5, 25, 75, 90)

## Build Configuration

- **Android:** compileSdk 35, minSdk 21, Kotlin 1.7.22, Gradle plugin 8.1.1
- **iOS:** deployment target 11.0, Swift 5.0
- **Dart/Flutter SDK:** >=3.0.0 <4.0.0
