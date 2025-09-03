package canaryprism.mcwm.swing.formatting;

import canaryprism.mcwm.swing.formatting.ParsedElement.ColorCode;
import canaryprism.mcwm.swing.formatting.ParsedElement.FormatCode;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MCFormattedLabel extends JComponent {
    
    public static final int DEFAULT_TEXT_SIZE = 12;
    private volatile String text;
    private final List<ParsedElement> elements;

    public MCFormattedLabel(String text) {
        this.text = text;
        this.elements = new ArrayList<>();
        parse();
    }

    
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        parse();
        repaint();
    }

    private volatile float size = DEFAULT_TEXT_SIZE;

    public void setTextSize(float size) {
        this.size = size;
        repaint();
    }

    public float getTextSize() {
        return size;
    }

    private volatile Point location = new Point(0, 0);

    public void setTextLocation(Point location) {
        this.location = location;
        repaint();
    }


    private void parse() {
        elements.clear();

        var chars = text.toCharArray();

        var sb = new StringBuilder();

        for (int i = 0; i < chars.length; i++) {
            var c = chars[i];

            if (c == '§' && i + 1 < chars.length) {
                i++;
                var formatting = switch (chars[i]) {
                    // Java edition colour codes
                    case '0' -> ColorCode.BLACK;
                    case '1' -> ColorCode.DARK_BLUE;
                    case '2' -> ColorCode.DARK_GREEN;
                    case '3' -> ColorCode.DARK_AQUA;
                    case '4' -> ColorCode.DARK_RED;
                    case '5' -> ColorCode.DARK_PURPLE;
                    case '6' -> ColorCode.GOLD;
                    case '7' -> ColorCode.GRAY;
                    case '8' -> ColorCode.DARK_GRAY;
                    case '9' -> ColorCode.BLUE;
                    case 'a' -> ColorCode.GREEN;
                    case 'b' -> ColorCode.AQUA;
                    case 'c' -> ColorCode.RED;
                    case 'd' -> ColorCode.LIGHT_PURPLE;
                    case 'e' -> ColorCode.YELLOW;
                    case 'f' -> ColorCode.WHITE;

                    // Bedrock edition colour codes for good measure
                    case 'g' -> ColorCode.MINECOIN_GOLD;
                    case 'h' -> ColorCode.MATERIAL_QUARTZ;
                    case 'i' -> ColorCode.MATERIAL_IRON;
                    case 'j' -> ColorCode.MATERIAL_NETHERITE;
                    // these two clash with the java edition format codes so they are commented out
                    // case 'm' -> ColorCode.material_redstone;
                    // case 'n' -> ColorCode.material_copper; 
                    case 'p' -> ColorCode.MATERIAL_GOLD;
                    case 'q' -> ColorCode.MATERIAL_EMERALD;
                    case 's' -> ColorCode.MATERIAL_DIAMOND;
                    case 't' -> ColorCode.MATERIAL_LAPIS;
                    case 'u' -> ColorCode.MATERIAL_AMETHYST;

                    // Java edition format codes
                    case 'k' -> FormatCode.OBFUSCATED;
                    case 'l' -> FormatCode.BOLD;
                    case 'm' -> FormatCode.STRIKETHROUGH;
                    case 'n' -> FormatCode.UNDERLINE;
                    case 'o' -> FormatCode.ITALIC;
                    case 'r' -> FormatCode.RESET;
                    default -> null;
                };
                if (formatting != null) {
                    elements.add(new ParsedElement.Text(sb.toString()));
                    sb.setLength(0);
                    elements.add(formatting);
                } else {
                    sb.append('§');
                    sb.append(chars[i]);
                }
            } else {
                sb.append(c);
            }
        }

        elements.add(new ParsedElement.Text(sb.toString()));
    }

    @Override
    protected void paintComponent(java.awt.Graphics g1) {
        super.paintComponent(g1);
        var g = (java.awt.Graphics2D) g1;

        g.setFont(g.getFont().deriveFont(size));

        var font = g.getFont();
        var color = g.getColor();
        var strikethrough = false; // how dare java.awt.Font not have a strikethrough property
        var underline = false; // ok this is just getting ridiculous
        var obfuscated = false;

        var point = this.location;
        g.translate(point.x, point.y);

        var x = 0;

        for (var element : elements) {
            if (element instanceof ParsedElement.Text(String str)) {
                if (!obfuscated) {
                    g.drawString(str, x, 0);
                } else {
                    // i have no idea how else to do this
                    var chars = str.toCharArray();
                    for (int i = 0; i < 2; i++) {
                        ArrayUtils.shuffle(chars);

                        var shuffled = new String(chars);
                        g.drawString(shuffled, x, 0);
                    }
                }
                var text_width = g.getFontMetrics().stringWidth(str); // is an int really the most precise type for this?
                if (strikethrough) {
                    int y = g.getFontMetrics().getAscent() / 2;
                    g.drawLine(x, -y, x + text_width, -y);
                }
                if (underline) {
                    g.drawLine(x, 0, x + text_width, 0);
                }

                x += text_width;
            } else if (element instanceof ColorCode c) {
                g.setColor(c.color);
            } else if (element instanceof FormatCode format) {
                switch (format) {
                    case OBFUSCATED -> obfuscated = true;
                    case BOLD -> g.setFont(g.getFont().deriveFont(Font.BOLD));
                    case STRIKETHROUGH -> strikethrough = true;
                    case UNDERLINE -> underline = true;
                    case ITALIC -> g.setFont(g.getFont().deriveFont(Font.ITALIC));
                    case RESET -> {
                        g.setFont(font);
                        g.setColor(color);
                        strikethrough = false;
                        underline = false;
                        obfuscated = false;
                    }
                }
            }
        }
    }

}
