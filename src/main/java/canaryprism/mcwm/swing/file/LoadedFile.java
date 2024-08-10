package canaryprism.mcwm.swing.file;

import java.io.File;

public sealed interface LoadedFile permits WorldFile, UnknownFile {
    File file();
}
