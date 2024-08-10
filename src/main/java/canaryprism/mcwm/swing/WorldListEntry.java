package canaryprism.mcwm.swing;

import javax.swing.JComponent;

import canaryprism.mcwm.swing.file.LoadedFile;
import canaryprism.mcwm.swing.file.UnknownFile;
import canaryprism.mcwm.swing.file.WorldFile;

public sealed abstract class WorldListEntry extends JComponent permits WorldEntry, UnknownEntry {
    public static WorldListEntry of(LoadedFile file) {
        return switch (file) {
            case WorldFile e -> new WorldEntry(e);
            case UnknownFile e -> new UnknownEntry(e);
        };
    }
}
