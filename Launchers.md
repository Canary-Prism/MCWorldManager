# Launchers

since version 2.0.0 MCWM has had the *capability* of supporting lots of different launchers and their different save directories including instances  
however since each launcher puts them in different places and differenter places when in different OSes, and some don't even put them in consistent locations, i'll have to gradually add in support for each OS as i figure them out, if it doesn't support yours, it simply never shows in the launcher list

**if you use a launcher and OS that isn't supported please tell me how its saves directory works**

## Currently these launchers are supported

### Default (Vanilla) launcher
fully supported on all platforms (you'd hope so i guess :p)

### Prism Launcher
- Mac: Supported
- Windows: Mostly Supported (untested)
    - supports the default installer
    - supports the Scoop package manager
    - for portables, it assumes you put your prismlauncher.exe somewhere in Downloads, Documents, or Desktop, and is only nested at most 2 folders deep
    - if you have multiple instances of Prism Launcher in your computer only the one that happens to get detected first is used
- Linux: Kinda Supported (untested)
    - supports the Flatpak version
    - supports the installed version
    - **DOES NOT** support AppImages (i haven't figured those out yet :p)
    - for portables, it assumes you put your PrismLauncher somewhere in Downloads, Documents, or Desktop, and is only nested at most 2 folders deep
    - if you have multiple instances of Prism Launcher in your computer only the one that happens to get detected first is used

### MultiMC
- Mac: Supported
    - assumes you put your MultiMC.app in your Applications folder (user or root)
    - i'm honestly not sure if this assumption is reasonable
- Windows: Kinda Supported (untested)
    - assumes you put your MultiMC.exe somewhere in Downloads, Documents, or Desktop, and is only nested at most 2 folders deep
    - if you have multiple instances of MultiMC in your computer only the one that happens to get detected first is used
    - i also worry that this will be slow as fuck especially on older computers 
- Linux: NOT Supported