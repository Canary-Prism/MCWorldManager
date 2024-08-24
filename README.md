# MCWorldManager

a thing for importing, exporting, and just managing Minecraft Java Edition worlds

totally didn't just make this because i got tired of hearing people ask how to do this one very basic thing about minecraft

for more information about different launchers and their support see [Launchers.md](https://github.com/Canary-Prism/MCWorldManager/blob/main/Launchers.md)

NBT tag icons by Squidguset

### [Download](https://github.com/Canary-Prism/MCWorldManager/releases/)

I'm assuming you know how to use GitHub. If not then here:

### Download Steps

1. Click above link
2. Find latest release
3. Find "Assets" Section
4. Click the "MCWorldManager-x.y.z.jar" file

### Notice

This program uses Java 22 bc i'm weird like that  
(i did however resist the temptation to use preview features so that this remains double-clickable and so somewhat idiot friendly)

#### [Here's a handy link](https://adoptium.net/temurin/releases/?version=22)

(it's for Temurin bc nobody likes oracle)


## Historical Changelog

### 2.4.8
- more try catches

### 2.4.7
- added try catches to cache loading
- improved™ copy to clipboard function

### 2.4.6
- fixed mass editing not working
- edit popup selects the original value text

### 2.4.5
- double click on a tag to edit in NBT Viewer
- popup windows in NBT Viewer now appear in the middle of the main NBT Viewer window

### 2.4.4
- added support for MultiMC on Linux
- added caching of found launcher save directories

### 2.4.3
- hotfix for Modrinth Linux Fedora(?)

### 2.4.2
- improved (i think) Modrinth save locating in Linux

### 2.4.1
- fixed a bug where you could drag a tag into itself or its children and it would delete itself

### 2.4.0
- added search functionality in NBT Viewer
- multimc and prism now search in %APPDATA% for their instance folders

### 2.3.1
- curseforge..

### 2.3.0
- added tag icons to NBT Viewer (thanks Squidguset)
- fixed adding tags inside empty list tags
- fixed drag and drop type checking
- hehe about menu :3

### 2.2.0
- added Modrinth App support for Mac, Windows, and Linux

### 2.1.0
- added Prism Launcher support for Mac, Windows (mostly), and Linux (kinda)
- renamed MultiMC launcher name

### 2.0.3
- added MultiMC support for windows (kinda), see Launchers.md for more details

### 2.0.2
- forgot to close my InputStream ***again*** (this probably isn't a big enough deal to warrant a new release but whatever)

### 2.0.1
- you're now allowed to select a custom directory even if only 1 launcher was found
- stopped swallowing some exceptions and actually print them to stderr

### 2.0.0
- added multiple launcher and instance support (this is just that there is now a way of supporting these, each launcher needs to be handled on a per launcher basis)
- Vanilla launcher is now an option in the launchers
- added MultiMC launcher support (Mac only for now (because the directory is *somewhat* standard i think idk))
- fixed just... **so** many InputStream resource leaks

### 1.6.2
- lots of bugfixes, some of them my fault, some of them the fault of this other person and their hateful NBT library

### 1.6.1
- sorted world list
- changed a majority of IO operations to use NIO
- loading worlds is now non-blocking
- X button on the NBT editor now does its job
- duplicating tags that contain other tags now should work properly
- editing NBT is now allowed on all folders that have a `level.dat`, not just successfully loaded worlds
- files that failed to load present more useful information

### 1.6.0
- added NBT editor
- fixed formatted text underline and strikethroughs, improved obfuscated (probably, imo)

### 1.5.0
- added support for minecraft formatter codes in world name

### 1.4.1
- just really minor changes here and there 

### 1.4.0
- made importing multiple worlds run concurrently (also they don't show success dialogue for each one now)
- added Open Folder button

### 1.3.0
- added importing multiple worlds at the same time
- added drag and drop importing feature

### 1.2.2
- fixed import and delete system
- fixed autoreload race conditions

### 1.2.1
- fixed export system (oops)

### 1.2.0
- better import dialogue again
- added a hotfix fallback which just asks you where the minecraft saves folder is if it can't find one

### 1.1.0
- better import dialogue
- shows proper error dialogue if you don't have minecraft folder

### 1.0.0
- made the thing