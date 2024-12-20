package canaryprism.mcwm.swing.savedir;

import java.awt.Image;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

import canaryprism.mcwm.savedir.SaveFinder;
import canaryprism.mcwm.swing.file.UnknownFile;

public class SaveFinderView extends JComponent {

    private static final Image default_icon;
    static {
        try (var is = SaveFinderView.class.getResourceAsStream("/mcwm/missing.png")) {
            default_icon = ImageIO.read(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load texture placeholder", e);
        }
    }
    
    
    private final SaveFinder finder;

    public SaveFinderView(SaveFinder finder) {
        this.finder = finder;
    }

    public SaveFinder getFinder() {
        return finder;
    }

    public void setFile(UnknownFile file) {
        throw new UnsupportedOperationException("Cannot change file");
    }


    @Override
    protected void paintComponent(java.awt.Graphics g1) {
        super.paintComponent(g1);

        var g = (java.awt.Graphics2D) g1;

        var icon = finder.getIcon().orElse(default_icon);

        float height = getHeight();

        g.drawImage(icon, 0, 0, Math.round(height), Math.round(height), null);

        var x = height + 5;
        var y_step = height / 3;
        var y = 0;

        var font = g.getFont();
        g.setFont(font.deriveFont(height / 2f));

        y += y_step;

        y += y_step / 15;

        g.drawString(finder.getLauncherName(), x, y += y_step);
    }
}
