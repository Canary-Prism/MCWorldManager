package canaryprism.mcwm.swing;

import java.awt.Color;
import java.nio.file.Files;

import canaryprism.mcwm.swing.file.UnknownFile;

public final class UnknownEntry extends WorldListEntry {
    private final UnknownFile file;


    public UnknownEntry(UnknownFile file) {
        this.file = file;
    }

    public UnknownFile getFile() {
        return file;
    }

    public void setFile(UnknownFile file) {
        throw new UnsupportedOperationException("Cannot change file");
    }


    @Override
    protected void paintComponent(java.awt.Graphics g1) {
        super.paintComponent(g1);

        var g = (java.awt.Graphics2D) g1;

        float height = getHeight();

        var x = 10;
        var y_step = height / 3;
        var y = 0;

        var font = g.getFont();
        g.setFont(font.deriveFont(height / 4f));

        g.setColor(Color.red);
        
        var path = file.path();
        if (Files.isDirectory(path) && Files.exists(path.resolve("level.dat"))) {
            g.drawString("<Potentially corrupted Minecraft world>", x, y += y_step);
        } else {
            g.drawString("<Not a Minecraft world>", x, y += y_step);
        }

        g.setColor(Color.white.darker());

        y += y_step / 15;

        g.drawString(file.path().toFile().getName(), x, y += y_step);

        y -= y_step / 15;
    
        g.drawString("Error: " + file.exception().getMessage(), x, y += y_step);
    }
}
