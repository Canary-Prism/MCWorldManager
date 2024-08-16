package canaryprism.mcwm.swing;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.imageio.ImageIO;

import canaryprism.mcwm.swing.file.WorldFile;
import canaryprism.mcwm.swing.formatting.MCFormattedLabel;

public final class WorldEntry extends WorldListEntry {

    private static final Image default_icon;
    static {
        try {
            default_icon = ImageIO.read(WorldEntry.class.getResourceAsStream("/mcwm/default_world_icon.png"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load default world icon", e);
        }
    }

    private final MCFormattedLabel world_label;

    private WorldFile file;
    public WorldEntry(WorldFile file) {
        this.world_label = new MCFormattedLabel(file.data().worldName());
        add(world_label);
        setWorldFile(file);
    }

    public WorldFile getWorldFile() {
        return file;
    }

    public void setWorldFile(WorldFile file) {
        this.file = Objects.requireNonNull(file);
        world_label.setText(file.data().worldName());

        revalidate();
        repaint();
    }

    @Override
    public void doLayout() {
        super.doLayout();

        var height = getHeight();
        var x = height + 5;
        var y_step = height / 3;

        world_label.setSize(getWidth() - x, getHeight() - y_step);
        world_label.setTextSize(height / 4f);
        world_label.setTextLocation(new Point(x, y_step));
        world_label.validate();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g1) {
        super.paintComponent(g1);

        var g = (java.awt.Graphics2D) g1;


        var data = file.data();

        var icon = data.image().orElse(default_icon);

        float height = getHeight();

        g.drawImage(icon, 0, 0, Math.round(height), Math.round(height), null);

        var x = height + 5;
        var y_step = height / 3;
        var y = 0;

        var font = g.getFont();
        g.setFont(font.deriveFont(height / 4f));

        if (!Files.isDirectory(file.path())) {
            g.setColor(Color.yellow);
        }
        
        // g.drawString(data.worldName(), x, y += y_step);
        y += y_step;

        g.setColor(g.getColor().darker());

        y += y_step / 15;

        g.drawString(file.path().toFile().getName() + " " + data.lastPlayed().format(DateTimeFormatter.ofPattern("(uuuu/MM/dd HH:mm)")), x, y += y_step);

        y -= y_step / 15;

        var builder = new StringBuilder();
        builder
            .append(data.gamemode().name)
            .append(", ");
        if (data.cheats()) {
            builder.append("Cheats, ");
        } 
        if (data.experimental()) {
            builder.append("Experimental, ");
        }
        builder
            .append("Version: ")
            .append(data.version());
    
        g.drawString(builder.toString(), x, y += y_step);
    }
}
