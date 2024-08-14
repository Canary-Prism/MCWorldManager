package canaryprism.mcwm.swing.nbt;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.ByteArrayTag;
import net.querz.nbt.tag.ByteTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.DoubleTag;
import net.querz.nbt.tag.FloatTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.LongTag;
import net.querz.nbt.tag.ShortTag;
import net.querz.nbt.tag.StringTag;
import net.querz.nbt.tag.Tag;

public class NBTNodeRenderer extends DefaultTreeCellRenderer {

    @Override
    public JComponent getTreeCellRendererComponent(
                        JTree tree,
                        Object value,
                        boolean sel,
                        boolean expanded,
                        boolean leaf,
                        int row,
                        boolean hasFocus) {

        super.getTreeCellRendererComponent(
                        tree, value, sel,
                        expanded, leaf, row,
                        hasFocus);

        var node = (DefaultMutableTreeNode) value;

        var str = switch (node.getUserObject()) {
            case NamedTag namedTag -> stringifyTag(namedTag);
            case Tag<?> tag -> stringifyTag(tag);
            default -> node.getUserObject().toString();
        };

        setText(str);

        return this;
    }
    public static String stringifyTag(NamedTag tag) {
        return strinfigyTag(tag.getName(), tag.getTag());
    }
    public static String strinfigyTag(String name, Tag<?> tag) {
        return name + ": " + stringifyTag(tag);
    }
    public static String stringifyTag(Tag<?> tag) {
        return switch (tag) {
            case ByteTag byteTag -> byteTag.valueToString();
            case ShortTag shortTag -> shortTag.valueToString();
            case IntTag intTag -> intTag.valueToString();
            case LongTag longTag -> longTag.valueToString();
            case FloatTag floatTag -> floatTag.valueToString();
            case DoubleTag doubleTag -> doubleTag.valueToString();
            case ByteArrayTag byteArrayTag -> "<" + byteArrayTag.length() + " bytes>";
            case StringTag stringTag -> stringTag.valueToString();
            case ListTag<?> listTag -> "<" + listTag.size() + " entries>";
            case CompoundTag compoundTag -> "<" + compoundTag.size() + " entries>";
            case IntArrayTag intArrayTag -> "<" + intArrayTag.length() + " ints>";
            case LongArrayTag longArrayTag -> "<" + longArrayTag.valueToString() + " longs>";
            default -> tag.getClass().getSimpleName();
        };
    }
}
