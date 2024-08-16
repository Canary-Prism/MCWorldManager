package canaryprism.mcwm.swing.file;

import java.nio.file.Path;

public record UnknownFile(Path path, Exception exception) implements LoadedFile {
    public UnknownFile {
        if (path == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        if (exception == null) {
            throw new IllegalArgumentException("exception cannot be null");
        }
    }
}
