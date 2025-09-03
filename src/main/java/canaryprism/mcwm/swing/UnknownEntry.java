package canaryprism.mcwm.swing;

import canaryprism.mcwm.saves.ParsingException;
import canaryprism.mcwm.swing.file.UnknownFile;

import java.awt.*;

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
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);

        var g = (Graphics2D) g1;

        float height = getHeight();
        
        var font = g.getFont();
        g.setFont(font.deriveFont(height / 4));
        var metrics = g.getFontMetrics();
        
        var x = 10;
        var y = height / 3 + metrics.getLineMetrics("mewo", g).getBaselineOffsets()[Font.ROMAN_BASELINE];

        g.setColor(Color.red);
        
        if (file.exception() instanceof ParsingException e) {
            var verdict = e.getVerdict();
            g.drawString("<" + verdict + ">", x, y);
        } else {
            g.drawString("<Unknown>", x, y);
        }
        
        y = height - metrics.getMaxDescent() - metrics.getHeight();
        
        g.setColor(Color.white.darker());

        g.drawString(file.path().toFile().getName(), x, y);

        y += metrics.getHeight();
    
        g.drawString("Error: " + file.exception().getMessage(), x, y);
    }
}
