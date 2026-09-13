# Handpicked GPS Camera — Phone-only APK build

This project includes a GitHub Actions workflow that builds an installable **debug APK** in the cloud. No laptop or Android Studio is required.

## Phone-only steps
1. Create a GitHub account if you do not already have one.
2. Create a new **public or private repository** named `HandpickedGPSCamera`.
3. On the repository page, choose **Add file → Upload files**.
4. Extract this ZIP on your phone first, then upload the project files/folders inside `HandpickedGPSCamera` (including `.github`).
5. Commit the files to the `main` branch.
6. Open the repository's **Actions** tab. Select **Build Handpicked GPS Camera APK** and tap **Run workflow** if it did not start automatically.
7. When the workflow finishes successfully, open the run and scroll to **Artifacts**.
8. Download `Handpicked-GPS-Camera-debug`, extract it, and install `app-debug.apk` on Android.

The first install may require allowing your browser/file manager to install unknown apps.

## Important
- This is a debug APK for testing, not a Play Store release build.
- The app will ask for camera and location permissions.
- A production release should use a properly protected signing key before distribution.
