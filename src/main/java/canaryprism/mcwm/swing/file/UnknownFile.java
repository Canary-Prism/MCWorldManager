package canaryprism.mcwm.swing.file;

import java.io.File;

public record UnknownFile(File file, Exception exception) implements LoadedFile {
    public UnknownFile {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        if (exception == null) {
            throw new IllegalArgumentException("exception cannot be null");
        }
    }
}
