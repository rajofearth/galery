# Galery

A local photo and video gallery for Android, built with Kotlin, Compose, and Galery's own glass UI. Browse your device library offline.

## Features

- Aspect-preserving mosaic with a toggle to a square grid
- Albums from device folders
- Full-screen Viewer with swipe, zoom, tap-to-hide photo controls, and muted video playback
- Favorites, drag selection with edge scrolling, sharing, and media details
- Search by filename, album, and date
- Date browsing by year, month, and day, with a floating glass dock

Sharing supports up to 100 items at a time. Android 11 and later support moving media to system trash and restoring it.

## Build

Run `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug` on Windows. Run `.\gradlew.bat assembleRelease` for the shrunken release APK. The universal signed APK budget is 2,000,000 bytes.

## Updates

Open Settings from Albums to check for public GitHub releases or turn automatic checks off. Android asks for approval before installing a downloaded update.

Publish stable releases with a `vX.Y.Z` tag and a `galery-X.Y.Z.apk` asset. Match the APK version name to the tag, increment its version code, and sign it with the same key as the previous release.

## Docs

- [Glossary](GLOSSARY.md)
- [Deferred scope](docs/DEFERRED.md)
- [Architecture decisions](docs/adr/README.md)
