package canaryprism.mcwm.swing.file;

import java.io.File;
import canaryprism.mcwm.saves.WorldData;

public record WorldFile(File file, WorldData data) implements LoadedFile {
    public WorldFile {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
    }
}
