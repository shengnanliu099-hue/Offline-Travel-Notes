# Offline Travel Notes Android App

## Features
- Fully offline local storage (SQLite)
- Create notes with title and content
- Up to 9 photos per note
- Long-press and drag photos to reorder in the editor
- Selected photos are copied into app private storage for offline viewing
- Automatic `#tag` extraction
- Search by `#tag` or keyword
- Edit notes (title/content/photos)
- Delete notes (also cleans local image files)
- Dark minimal UI inspired by X, with note card entrance animation
- UI text follows system language (Chinese/English)

## Open and Run
1. Open this folder in Android Studio.
2. Wait for Gradle Sync to complete.
3. Run the app on an emulator or a physical Android device.

## Usage
- Add tags in content, for example: `#tokyo #travel`
- In the search box:
  - `#tokyo`: exact tag search
  - `tokyo`: keyword search across title/content/tags
- Tap **Edit** on a note card to enter edit mode.
- In edit mode, you can remove existing images, add new images, and tap **Update**.
- In edit mode, long-press an image and drag left/right to reorder.
