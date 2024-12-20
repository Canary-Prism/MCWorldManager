package canaryprism.mcwm.swing.savedir;

import java.awt.Image;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import canaryprism.mcwm.savedir.SaveDirectory;
import canaryprism.mcwm.swing.file.UnknownFile;

public class SaveDirectoryView extends JComponent {

    private static final Image default_icon;
    static {
        try (var is = SaveFinderView.class.getResourceAsStream("/mcwm/missing.png")) {
            default_icon = ImageIO.read(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load texture placeholder", e);
        }
    }
    
    
    private final SaveDirectory instance;

    public SaveDirectoryView(SaveDirectory instance) {
        this.instance = instance;
    }

    public SaveDirectory getInstance() {
        return instance;
    }

    public void setFile(UnknownFile file) {
        throw new UnsupportedOperationException("Cannot change file");
    }


    @Override
    protected void paintComponent(java.awt.Graphics g1) {
        super.paintComponent(g1);

        var g = (java.awt.Graphics2D) g1;

        var icon = instance.icon().orElse(default_icon);

        float height = getHeight();

        g.drawImage(icon, 0, 0, Math.round(height), Math.round(height), null);

        var x = height + 5;
        var y_step = height / 3;
        var y = 0;

        var font = g.getFont();
        g.setFont(font.deriveFont(height / 2f));

        y += y_step;

        y += y_step / 15;

        g.drawString(instance.name(), x, y += y_step);
    }
}
