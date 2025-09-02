package canaryprism.mcwm.instance;

import java.awt.Image;
import java.nio.file.Path;
import java.util.Optional;

/**
 * represents a Minecraft save instance/profile
 * <p>
 * specifically, this contains a name, path, and icon 
 * of the specific save instance/profile
 * <p>
 * an instance is a completely separate save profile common in alternative launchers
 * such as MultiMC, Modrinth, and others
 */
public record SaveDirectory(Optional<Image> icon, String name, Path path) {
    public SaveDirectory {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
    }
}
