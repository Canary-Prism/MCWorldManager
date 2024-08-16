package canaryprism.mcwm.swing.file;

import java.nio.file.Path;

import canaryprism.mcwm.saves.WorldData;

public record WorldFile(Path path, WorldData data) implements LoadedFile {
    public WorldFile {
        if (path == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
    }
}
