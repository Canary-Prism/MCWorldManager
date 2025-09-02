package canaryprism.mcwm.instance.launcher;

import canaryprism.mcwm.instance.InstanceFinder;
import canaryprism.mcwm.instance.SaveDirectory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * the vanilla Minecraft launcher save finder
 * <p>
 * This class is responsible for finding the saves directory of the vanilla Minecraft launcher, 
 * which is a constant path in any platform
 * <p>
 * this makes my job pretty easy
 */
public final class Vanilla implements InstanceFinder {

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
                saves_path.add(new SaveDirectory(icon, getLauncherName(), path));
        } catch (Exception e) {
        }
    }

    @Override
    public synchronized void findMac() {
        var home = System.getProperty("user.home");
        try {
            var path = Path.of(home, "Library", "Application Support", "minecraft", "saves");
            if (Files.exists(path))
                saves_path.add(new SaveDirectory(icon, getLauncherName(), path));
        } catch (Exception e) {
        }   
    }

    @Override
    public synchronized void findLinux() {
        var home = System.getProperty("user.home");
        try {
            var path = Path.of(home, ".minecraft", "saves");
            if (Files.exists(path))
                saves_path.add(new SaveDirectory(icon, getLauncherName(), path));
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
