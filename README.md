# WebDisplays for Minecraft 1.12.2
---
WebDisplays is a mod for Minecraft 1.12.2. Even though the source code is public, it is not an "open source" project. Read the "LICENSE" file for more information. WebDisplays uses [MCEF](https://github.com/montoyo/mcef), an API to integrate Chromium Web Engine into Minecraft.

## Wiki
* The Wiki that details all blocks/items can be found on my website https://montoyo.net/wdwiki/

## Current state
Currently, WebDisplays can't play livestreams, music or movies (due to most video and audio codecs are not supported), and doesn't have any kind of synchronization between players. The last update was on July 24, 2019. The project is abandoned and is looking for the maintainer.

## FAQ
---
#### Q: My media doesn't play, what do I do?
You can't play normal .mp4, .mp3 or .mkv video files, as WebDisplays doesn't support any video or audio codecs apart from Google's open codecs: VP8 & VP9 for video and Vorbis & Opus for the audio, Theora too.

#### Q: Okay, but is there any workaround for it?
You can encode your movies into supported codecs using the appropriate program, like [HandBrake](https://handbrake.fr/). Encoding will take some time, depending on your computer's processing power, movie length, bitrate and frame count.

#### Q: When are we getting an upgrade with the support of the normal files?
When the time comes.

#### Q: My textures are black and purple, what do I do?
Remove Flan's mod and update Forge and Java.

## Delayed things
* Plugin API
* The Shop
* CC Interface, if CC gets updated...
* Center camera to screen when using keyboard
* minePad management: check GuiContainer.draggedStack for minePad
* In-game command to add/remove blacklisted domains
* Config: RPMP (Real pixels per Minecraft pixels)
* Disable miniserv in solo
