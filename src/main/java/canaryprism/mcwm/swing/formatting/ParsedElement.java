package canaryprism.mcwm.swing.formatting;

import java.awt.Color;

public sealed interface ParsedElement {
    record Text(String text) implements ParsedElement {
        public Text {
            if (text == null) {
                throw new IllegalArgumentException("text cannot be null");
            }
        }
    }
    enum ColorCode implements ParsedElement {
        BLACK(Color.BLACK),
        DARK_BLUE(new Color(0x0000AA)),
        DARK_GREEN(new Color(0x00AA00)),
        DARK_AQUA(new Color(0x00AAAA)),
        DARK_RED(new Color(0xAA0000)),
        DARK_PURPLE(new Color(0xAA00AA)),
        GOLD(new Color(0xFFAA00)),
        GRAY(Color.GRAY),
        DARK_GRAY(Color.DARK_GRAY),
        BLUE(new Color(0x5555FF)),
        GREEN(new Color(0x55FF55)),
        AQUA(new Color(0x55FFFF)),
        RED(new Color(0xFF5555)),
        LIGHT_PURPLE(new Color(0xFF55FF)),
        YELLOW(new Color(0xFFFF55)),
        WHITE(Color.WHITE),

        // might as well add the bedrock edition colours for good measure
        MINECOIN_GOLD(new Color(0xDDD605)),
        MATERIAL_QUARTZ(new Color(0xE3D4D1)),
        MATERIAL_IRON(new Color(0xCECACA)),
        MATERIAL_NETHERITE(new Color(0x443A3B)),
        @SuppressWarnings("unused")
        MATERIAL_REDSTONE(new Color(0x971607)),
        @SuppressWarnings("unused")
        MATERIAL_COPPER(new Color(0xB4684D)),
        MATERIAL_GOLD(new Color(0xDEB12D)),
        MATERIAL_EMERALD(new Color(0x47A036)),
        MATERIAL_DIAMOND(new Color(0x2CBAA8)),
        MATERIAL_LAPIS(new Color(0x21497B)),
        MATERIAL_AMETHYST(new Color(0x9A5CC6)),
        ;

        public final Color color;

        ColorCode(Color color) {
            this.color = color;
        }
    }

    enum FormatCode implements ParsedElement {
        OBFUSCATED, BOLD, STRIKETHROUGH, UNDERLINE, ITALIC, RESET
    }

}
