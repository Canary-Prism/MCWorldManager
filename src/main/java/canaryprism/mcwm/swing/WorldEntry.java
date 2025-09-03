package canaryprism.mcwm.swing;

import canaryprism.mcwm.swing.file.WorldFile;
import canaryprism.mcwm.swing.formatting.MCFormattedLabel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class WorldEntry extends WorldListEntry {

    private static final Image default_icon;
    static {
        try (var is = WorldEntry.class.getResourceAsStream("/mcwm/default_world_icon.png")) {
            assert is != null;
            default_icon = ImageIO.read(is);
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
        world_label.setTextSize(height / ((float) 4));
        world_label.setTextLocation(new Point(x, y_step));
        world_label.validate();
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);

        var g = (Graphics2D) g1;


        var data = file.data();

        var icon = data.image().orElse(default_icon);

        float height = getHeight();

        g.drawImage(icon, 0, 0, Math.round(height), Math.round(height), null);
        
        var font = g.getFont().deriveFont(height / 4);
        g.setFont(font);
        var metrics = g.getFontMetrics();
        
        var x = height + 5;
//        var y = g.getFontMetrics(g.getFont()).getHeight();
        var y = height - metrics.getMaxDescent() - metrics.getHeight();

        if (!Files.isDirectory(file.path())) {
            g.setColor(Color.yellow);
        }
        
        g.setColor(g.getColor().darker());

        g.drawString(file.path().toFile().getName() + " " + data.lastPlayed().format(DateTimeFormatter.ofPattern("(uuuu/MM/dd HH:mm)")), x, y);

        y += metrics.getHeight();
        
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
    
        g.drawString(builder.toString(), x, y);
    }
}
