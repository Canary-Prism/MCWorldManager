package canaryprism.mcwm.savedir.launcher;

import java.awt.Image;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import canaryprism.mcwm.savedir.SaveDirectory;
import canaryprism.mcwm.savedir.SaveFinder;

public class MultiMC implements SaveFinder {

    private static final Optional<Image> icon;
    static {
        Optional<Image> temp_icon;
        try {
            temp_icon = Optional.ofNullable(ImageIO.read(Vanilla.class.getResourceAsStream("/mcwm/launcher/multimc.png")));
        } catch (Exception e) {
            e.printStackTrace(); // swallowing exceptions is bad
            temp_icon = Optional.empty();
        }
        icon = temp_icon;
    }

    private static final Optional<Image> default_world_icon;
    static {
        Optional<Image> temp_icon;
        try {
            temp_icon = Optional
                    .ofNullable(ImageIO.read(Vanilla.class.getResourceAsStream("/mcwm/grass.png")));
        } catch (Exception e) {
            e.printStackTrace(); // swallowing exceptions is bad
            temp_icon = Optional.empty();
        }
        default_world_icon = temp_icon;
    }

    private final List<SaveDirectory> saves_path = new ArrayList<>();

    @Override
    public synchronized void findWindows() {
        // TODO: implement windows
    }

    @Override
    public synchronized void findMac() {
        var home = System.getProperty("user.home");

        var multimc = Path.of(home, "Applications", "MultiMC.app");
        if (!Files.isDirectory(multimc)) { // give it 1 more try from root
            multimc = Path.of("/", "Applications", "MultiMC.app");
        }

        var instances = multimc.resolve("Data", "instances");
        if (Files.isDirectory(instances)) {
            try {
                Files.list(instances)
                    .filter(Files::isDirectory)
                    .filter((e) -> Files.isDirectory(e.resolve(".minecraft")))
                    .map((e) -> {
                        try {
                            return new SaveDirectory(
                                Optional.of(ImageIO.read(e.resolve(".minecraft", "icon.png").toFile())), 
                                e.getFileName().toString(), 
                                e.resolve(".minecraft", "saves")
                            );
                        } catch (IOException ex) {
                            return new SaveDirectory(
                                default_world_icon, 
                                e.getFileName().toString(), 
                                e.resolve(".minecraft", "saves")
                            );
                        }
                    })
                    .forEach(saves_path::add);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void findLinux() {
        // TODO: implement linux
    }

    @Override
    public synchronized List<SaveDirectory> getSavesPaths() {
        return saves_path;
    }

    @Override
    public String getLauncherName() {
        return "MultiMC Launcher <WIP>";
    }

    @Override
    public Optional<Image> getIcon() {
        return icon;
    }

    
}
