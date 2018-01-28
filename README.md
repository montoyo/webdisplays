# WebDisplays for Minecraft 1.12.2
This is the unfinished port of the WebDisplays mod for Minecraft 1.12.2. The text below is my "TODO" list.

### Missing features
* Screen upgrade: "laser" mouse
* Screen upgrade: redstone input
* Screen upgrade: redstone output
* ~~Peripheral: ComputerCraft interface~~ (CC not up to date)
* Peripheral: OpenComputers interface
* Server blocks (to store some of the player's web pages)
* Read config (see "Config elements" below)

### TODO
* Achievements (minePad 2 and all that stuff)
* GuiSetURL2 missing buttons
* Plugin API
* Automatically add protocol to URLs
* Using the remote control tool too far away (with a chunk loader ofc) may trigger distance guard in SMessageScreenCtrl
* Recipes: why do I need to craft first to have them in my crafting book?
* French translations
* Embedded videos sound/distance
* minePad management: check GuiContainer.draggedStack for minePad
* Enhance crafts
* Enhance models
* minePad item texture seems to be transparent in some corners...

### Config elements
* Site blacklist
* minePad resolution
* Homepage
* RPMP (Real pixels per Minecraft pixels)
* Browser language
* Screen load/unload distance (max distance = 60.0)
* Disable ownership thief item
