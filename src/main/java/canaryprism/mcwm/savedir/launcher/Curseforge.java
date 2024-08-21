package canaryprism.mcwm.savedir.launcher;

import java.awt.Desktop;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

import canaryprism.mcwm.savedir.SaveDirectory;
import canaryprism.mcwm.savedir.SaveFinder;

public class Curseforge implements SaveFinder {

    @Override
    public void findWindows() {
    }

    @Override
    public void findMac() {
    }

    @Override
    public void findLinux() {
    }

    @Override
    public List<SaveDirectory> getSavesPaths() {
        return new ArrayList<SaveDirectory>() {
            @Override
            public int size() {
                return 2;
            }

            @Override
            public boolean isEmpty() {
                return false;
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
        return Optional.empty();
    }

}
