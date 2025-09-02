package canaryprism.mcwm.instance.launcher;

import canaryprism.mcwm.instance.InstanceFinder;
import canaryprism.mcwm.instance.SaveDirectory;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * nobody likes curseforge
 * <p>
 * fuck you curseforge
 */
public final class Curseforge implements InstanceFinder {

    private static volatile boolean has = false;

    @Override
    public void findWindows() {
        findAny();
    }

    @Override
    public void findMac() {
        findAny();
    }

    @Override
    public void findLinux() {
        findAny();
    }

    /**
     * i have no idea if this is even remotely where they put it, but i don't care
     * <p>
     * worst case this option just never shows up, which is good enough
     */
    private void findAny() {
        var home = System.getProperty("user.home");

        var path = Path.of(home, "curseforge");
        
        if (Files.isDirectory(path)) {
            has = true;
        }
    }

    @Override
    public List<SaveDirectory> getSavesPaths() {
        return new ArrayList<>() {
            @Override
            public int size() {
                return 2;
            }
            
            @Override
            public boolean isEmpty() {
                return !has;
            }
            
            @Override
            public <E> E[] toArray(IntFunction<E[]> generator) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(new java.net.URI("https://youtu.be/Vhdwz5apiQQ"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return generator.apply(0);
            }
        };
    }

    @Override
    public String getLauncherName() {
        return "Curseforge";
    }

    @Override
    public Optional<Image> getIcon() {
        return Optional.empty(); // i don't care
    }

}
