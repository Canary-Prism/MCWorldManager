package canaryprism.mcwm.swing.formatting;

import java.awt.Color;

public sealed interface ParsedElement {
    public record Text(String text) implements ParsedElement {
        public Text {
            if (text == null) {
                throw new IllegalArgumentException("text cannot be null");
            }
        }
    }
    public enum ColorCode implements ParsedElement {
        black(Color.BLACK),
        dark_blue(new Color(0x0000AA)),
        dark_green(new Color(0x00AA00)),
        dark_aqua(new Color(0x00AAAA)),
        dark_red(new Color(0xAA0000)),
        dark_purple(new Color(0xAA00AA)),
        gold(new Color(0xFFAA00)),
        gray(Color.GRAY),
        dark_gray(Color.DARK_GRAY),
        blue(new Color(0x5555FF)),
        green(new Color(0x55FF55)),
        aqua(new Color(0x55FFFF)),
        red(new Color(0xFF5555)),
        light_purple(new Color(0xFF55FF)),
        yellow(new Color(0xFFFF55)),
        white(Color.WHITE),

        // might as well add the bedrock edition colours for good measure
        minecoin_gold(new Color(0xDDD605)),
        material_quartz(new Color(0xE3D4D1)),
        material_iron(new Color(0xCECACA)),
        material_netherite(new Color(0x443A3B)),
        material_redstone(new Color(0x971607)),
        material_copper(new Color(0xB4684D)),
        material_gold(new Color(0xDEB12D)),
        material_emerald(new Color(0x47A036)),
        material_diamond(new Color(0x2CBAA8)),
        material_lapis(new Color(0x21497B)),
        material_amethyst(new Color(0x9A5CC6)),
        ;

        public final Color color;

        private ColorCode(Color color) {
            this.color = color;
        }
    }

    public enum FormatCode implements ParsedElement {
        obfuscated, bold, strikethrough, underline, italic, reset
    }

}
