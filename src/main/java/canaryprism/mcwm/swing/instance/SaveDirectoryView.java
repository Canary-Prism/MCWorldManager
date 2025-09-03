package canaryprism.mcwm.swing.instance;

import canaryprism.mcwm.instance.SaveDirectory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;

public class SaveDirectoryView extends JComponent {

    private static final Image default_icon;
    static {
        try (var is = SaveFinderView.class.getResourceAsStream("/mcwm/missing.png")) {
            assert is != null;
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


    @Override
    protected void paintComponent(java.awt.Graphics g1) {
        super.paintComponent(g1);

        var g = (java.awt.Graphics2D) g1;

        var icon = instance.icon().orElse(default_icon);

        float height = getHeight();

        g.drawImage(icon, 0, 0, Math.round(height), Math.round(height), null);
        
        var font = g.getFont().deriveFont(height / 2);
        g.setFont(font);
        
        var x = height + 5;
        
        var y = height / 2 - this.getFontMetrics(font)
                .getLineMetrics(instance.name(), g)
                .getBaselineOffsets()[Font.CENTER_BASELINE];
        

        g.drawString(instance.name(), x, y);
    }
}
