# EyeGesture — Eye-Tracking Automation for Android

EyeGesture is an on-device eye tracking and facial gesture automation app built with Kotlin, Jetpack Compose, CameraX, and Google ML Kit Face Detection.

## Automated APK Build via GitHub Actions

This repository includes a pre-configured GitHub Actions CI workflow (`.github/workflows/build-apk.yml`).

### How It Works:
1. **Push to GitHub**: Push your commits to any branch or create a pull request.
2. **Instant Build**: The workflow automatically triggers, sets up JDK 21 and the Android SDK, restores the debug signing key, and executes `./gradlew assembleDebug`.
3. **Download APK**:
   - Go to the **Actions** tab on your GitHub repository.
   - Click the latest workflow run.
   - Scroll down to the **Artifacts** section and download `EyeGesture-Debug-APK`.
4. **Releases**: Creating a git tag like `v1.0.0` will automatically create a GitHub Release with the APK attached.

## Local Development

```bash
# Clone the repository
git clone <your-repo-url>
cd <repo-folder>

# Build the debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`
