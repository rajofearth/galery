# Deferred

Items explicitly left out of v1. Add to this only when the decision is made to defer, not when an idea is merely mentioned.

## Search

- EXIF tag, location, and on-device content recognition search. Revisit after the agreed v1 feature set has landed and behaves as intended.

## Details

- Location and camera model fields, and any other fields beyond filename, folder, path, size, date taken, dimensions, MIME type, favourite state, and video duration.

## Empty states

- Illustration or mascot for empty All media, album, and Favorites views. v1 uses text and icons.

## Media actions

- Set as wallpaper.
- Slideshow.
- Edit and crop.

## Other targets

- Wear OS, per ADR-0010.

## Known limitations of v1

- Favourites are app-local IDs and are cleared when Galery is uninstalled.
- Full media access is required. Denying it blocks the app rather than showing a partial gallery.
