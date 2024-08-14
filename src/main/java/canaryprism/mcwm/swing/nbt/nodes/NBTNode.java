package canaryprism.mcwm.swing.nbt.nodes;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

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

public class NBTNode<T> extends DefaultMutableTreeNode {

    public NBTNode(Tag<T> tag) {
        super(tag);

        this.allowsChildren = tag instanceof CompoundTag || tag instanceof ListTag || tag instanceof ByteArrayTag || tag instanceof IntArrayTag || tag instanceof LongArrayTag;
    }

    public static String getTagName(Tag<?> tag) {
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
