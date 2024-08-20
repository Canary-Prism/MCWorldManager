package canaryprism.mcwm.savedir.launcher;

import java.awt.Image;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import canaryprism.mcwm.savedir.SaveDirectory;
import canaryprism.mcwm.savedir.SaveFinder;

public class Vanilla implements SaveFinder {

    private static final Optional<Image> icon;
    static {
        Optional<Image> temp_icon;
        try (var is = Vanilla.class.getResourceAsStream("/mcwm/launcher/vanilla/icon.png")) {
            temp_icon = Optional.ofNullable(ImageIO.read(is));
        } catch (Exception e) {
            e.printStackTrace(); // swallowing exceptions is bad
            temp_icon = Optional.empty();
        }
        icon = temp_icon;
    }

    private final List<SaveDirectory> saves_path = new ArrayList<>();

    @Override
    public synchronized void findWindows() {
        var appdata = System.getenv("APPDATA");
        try {
            var path = Path.of(appdata, ".minecraft", "saves");
            if (Files.exists(path))
                saves_path.add(new SaveDirectory(Optional.empty(), getLauncherName(), path));
        } catch (Exception e) {
        }
    }

    @Override
    public synchronized void findMac() {
        var home = System.getProperty("user.home");
        try {
            var path = Path.of(home, "Library", "Application Support", "minecraft", "saves");
            if (Files.exists(path))
                saves_path.add(new SaveDirectory(Optional.empty(), getLauncherName(), path));
        } catch (Exception e) {
        }   
    }

    @Override
    public synchronized void findLinux() {
        var home = System.getProperty("user.home");
        try {
            var path = Path.of(home, ".minecraft", "saves");
            if (Files.exists(path))
                saves_path.add(new SaveDirectory(Optional.empty(), getLauncherName(), path));
        } catch (Exception e) {
        }
    }

    @Override
    public synchronized List<SaveDirectory> getSavesPaths() {
        return saves_path;
    }
    

    @Override
    public String getLauncherName() {
        return "Minecraft Launcher (Vanilla)";
    }

    @Override
    public Optional<Image> getIcon() {
        return icon;
    }
}
