package canaryprism.mcwm.swing.file;

import java.nio.file.Path;

public sealed interface LoadedFile permits WorldFile, UnknownFile {
    Path path();
}
