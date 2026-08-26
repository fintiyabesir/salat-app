# Apple build pipeline

Salat treats Apple builds as first-class CI outputs.

## Pull request and main CI

The macOS job:

1. builds the Kotlin Multiplatform `SalatShared` framework for Apple Silicon iOS Simulator;
2. generates a deterministic Xcode project from `iosApp/project.yml` using XcodeGen;
3. invokes `xcodebuild` for the `SalatApp` scheme and a generic iOS Simulator destination;
4. restricts the simulator app build to `arm64`, matching the KMP `iosSimulatorArm64` framework slice and GitHub's Apple Silicon runner;
5. disables code signing for CI simulator builds;
6. verifies that the resulting `Salat.app` bundle and its `Info.plist` exist.

This catches Xcode target configuration, resource bundling, localization, Swift compilation, framework linking, architecture compatibility, and plist issues that a standalone Swift typecheck cannot catch.

## Device / TestFlight pipeline

A later release workflow will build an archive for `iphoneos`, sign it with Apple Developer credentials supplied through GitHub Actions secrets, and upload it to TestFlight. Signing credentials must never be committed to the repository.
