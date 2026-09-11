This release expands Bedrock Skins from a skin-pack skin selector into a complete Bedrock-style wardrobe for Java Edition. It adds support for Character Creator cosmetics and emotes, redesigns the wardrobe, improves multiplayer synchronization, and expands Bedrock geometry and texture compatibility.

### Persona cosmetics

- Added support for Bedrock Edition Character Creator (Persona) cosmetic pieces.
- Added support for external Persona content in the dedicated `persona` directory inside the Minecraft game directory.
- Added support for Persona content bundled with the mod and supplied by enabled resource packs.
- Added support for cube-based geometry and Bedrock `poly_mesh` geometry.
- Added support for per-face UV rendering.
- Added support for Persona texture atlases.
- Added support for Persona TGA tint-maps.
- Added color selection for hair and facial-hair pieces. Colors are stored globally per piece type.
- Added left, both, and right equipment modes for arm and leg cosmetics. Limb-side selection is also global per cosmetic type.
- Added an Equipped category for quickly finding active cosmetics.
- Added bundled default Persona clothing, hair, facial hair, prosthetic arms, prosthetic legs, and other Character Creator pieces.

### Cosmetic equipment and occlusion

- Cosmetics can be equipped and unequipped directly from the wardrobe.
- Equipped cosmetics render during gameplay, first-person arm rendering, wardrobe previews, and main-menu and pause-menu paper dolls.
- Added multiplayer synchronization for cosmetic geometry, textures, slim variants, zones, tint colors, and limb-side choices.
- Added Persona zone parsing and player-model occlusion.
- Cosmetics can hide the relevant base head, body, arm, and leg parts.
- Clothing zones independently hide the hat, jacket, sleeves, and pants layers.

### Persona emotes

- Added support for Bedrock Persona emote metadata and animation files from external folders, bundled content, and resource packs.
- Added a Bedrock-style radial emote wheel.
- Added a persistent six-slot emote loadout with Equip Emote and Unequip Emote actions.
- Number keys 1–6 play the corresponding equipped emote from the wheel.
- Movement and jumping cancel the active emote.
- Added an optional third-person emote camera that restores the previous perspective when the emote ends or is cancelled.
- Emotes animate the player model, held items, capes, armor, and equipped cosmetics.

### Dressing-room animations

- Added Bedrock dressing-room reactions for head, torso, arm, bottom, and back pieces.
- Selecting a cosmetic plays an appropriate dressing-room preview animation.

### Wardrobe redesign

- Added global wardrobe search for skins, cosmetics, emotes, and capes.
- Added tab icons for Skins, Cosmetics, Emotes, and Capes.
- Added a dedicated Imported Skins skin pack for custom skins.
- Added Add Custom Skin and delete actions for imported skins.
- Added a color palette button for supported cosmetics.
- Added a preview button that opens a full-screen preview of the selected skin.
- Removed the Download Packs tab and its related functionality.

### Mod compatibility

- Added a Sodium-compatible path for Bedrock cubes that require live per-face polygons.

### Performance and resource loading

- Improved cleanup for dynamic textures, thumbnails, preview players, remote cosmetics, and models.
- Improved skin-pack loading by validating geometry before native texture allocation.

### Multiplayer and render-state fixes

- Expanded appearance synchronization to include Persona cosmetics and emotes.
- Consolidated client payload sending across supported networking implementations.
- Improved cleanup of remote appearance state when players disconnect or resources reload.

**Full Changelog**: https://github.com/BrandonItaly/Bedrock-Skins/commits/master
