package canaryprism.mcwm.swing.formatting;

import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

import org.apache.commons.lang3.ArrayUtils;

import canaryprism.mcwm.swing.formatting.ParsedElement.ColorCode;
import canaryprism.mcwm.swing.formatting.ParsedElement.FormatCode;

public class MCFormattedLabel extends JComponent {
    private volatile String text;
    private final List<ParsedElement> elements;

    public MCFormattedLabel(String text) {
        this.text = text;
        this.elements = new ArrayList<ParsedElement>();
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

    private volatile float size = 12;

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
                    case '0' -> ColorCode.black;
                    case '1' -> ColorCode.dark_blue;
                    case '2' -> ColorCode.dark_green;
                    case '3' -> ColorCode.dark_aqua;
                    case '4' -> ColorCode.dark_red;
                    case '5' -> ColorCode.dark_purple;
                    case '6' -> ColorCode.gold;
                    case '7' -> ColorCode.gray;
                    case '8' -> ColorCode.dark_gray;
                    case '9' -> ColorCode.blue;
                    case 'a' -> ColorCode.green;
                    case 'b' -> ColorCode.aqua;
                    case 'c' -> ColorCode.red;
                    case 'd' -> ColorCode.light_purple;
                    case 'e' -> ColorCode.yellow;
                    case 'f' -> ColorCode.white;

                    // Bedrock edition colour codes for good measure
                    case 'g' -> ColorCode.minecoin_gold;
                    case 'h' -> ColorCode.material_quartz;
                    case 'i' -> ColorCode.material_iron;
                    case 'j' -> ColorCode.material_netherite;
                    // these two clash with the java edition format codes so they are commented out
                    // case 'm' -> ColorCode.material_redstone;
                    // case 'n' -> ColorCode.material_copper; 
                    case 'p' -> ColorCode.material_gold;
                    case 'q' -> ColorCode.material_emerald;
                    case 's' -> ColorCode.material_diamond;
                    case 't' -> ColorCode.material_lapis;
                    case 'u' -> ColorCode.material_amethyst;

                    // Java edition format codes
                    case 'k' -> FormatCode.obfuscated;
                    case 'l' -> FormatCode.bold;
                    case 'm' -> FormatCode.strikethrough;
                    case 'n' -> FormatCode.underline;
                    case 'o' -> FormatCode.italic;
                    case 'r' -> FormatCode.reset;
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
            if (element instanceof ParsedElement.Text text) {
                if (!obfuscated) {
                    g.drawString(text.text(), x, 0);
                } else {
                    // i have no idea how else to do this
                    var chars = text.text().toCharArray();
                    for (int i = 0; i < 3; i++) {
                        ArrayUtils.shuffle(chars);

                        var shuffled = new String(chars);
                        g.drawString(shuffled, x, 0);
                    }
                }
                var text_width = g.getFontMetrics().stringWidth(text.text()); // is an int really the most precise type for this?
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
                    case obfuscated -> {
                        obfuscated = true;
                    }
                    case bold -> {
                        g.setFont(g.getFont().deriveFont(Font.BOLD));
                    }
                    case strikethrough -> {
                        strikethrough = true;
                    }
                    case underline -> {
                        underline = true;
                    }
                    case italic -> {
                        g.setFont(g.getFont().deriveFont(Font.ITALIC));
                    }
                    case reset -> {
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
