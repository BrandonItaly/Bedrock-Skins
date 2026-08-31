- Added the ability to upload the selected skin to the signed-in Minecraft account.
- Improved skin and cape rendering consistency by sharing the same override logic across player rendering paths.
- Improved state persistence so favorites, selected skins, and selected capes are updated independently and saved atomically.
- Improved downloadable pack failure handling so unsuccessful downloads can be retried without incorrectly marking the pack as installed.
- Fixed duplicate cape and store-icon download requests, including retries after failed icon downloads.
- Improved preview and skin-pack resource cleanup to prevent stale textures and preview state.

**Full Changelog**: https://github.com/BrandonItaly/Bedrock-Skins/commits/master
