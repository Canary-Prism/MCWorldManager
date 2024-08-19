package canaryprism.mcwm.savedir;

import java.awt.Image;
import java.nio.file.Path;
import java.util.Optional;

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
