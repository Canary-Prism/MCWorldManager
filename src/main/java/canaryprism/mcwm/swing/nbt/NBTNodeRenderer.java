package canaryprism.mcwm.swing.nbt;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static canaryprism.mcwm.swing.nbt.NBTNodeIcon.*;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.ByteArrayTag;
import net.querz.nbt.tag.ByteTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.DoubleTag;
import net.querz.nbt.tag.EndTag;
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

    private final HashMap<NBTNodeIcon, Image> icons = new HashMap<>();
    {
        try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {

            var icons = NBTNodeIcon.values();

            var futures = Stream.of(icons).map((icon) -> CompletableFuture.runAsync(() -> {
                try (var is = NBTNodeRenderer.class.getResourceAsStream("/mcwm/nbt/" + icon.file_name)) {
                    var image = ImageIO.read(is);
                    synchronized (this.icons) {
                        this.icons.put(icon, image);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, ex)).toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures).join();

        }
    }

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
            case NamedTag namedTag -> stringifyTag(node, namedTag);
            case Tag<?> tag -> stringifyTag(node, tag);
            default -> node.getUserObject().toString();
        };

        setText(str);

        var buffered_image = new BufferedImage(25, 25, BufferedImage.TYPE_INT_ARGB);

        var g = buffered_image.createGraphics();

        {

            var o = node.getUserObject();
            boolean named = false;
            if (o instanceof NamedTag named_tag) {
                o = named_tag.getTag();
                named = true;
            }
            var tag = o;

            //#region draw tag if is tag
            {
                var copy = tag;
                if (copy instanceof ListTag<?> list_tag) {
                    try {
                        if (list_tag.getTypeClass() != EndTag.class) {
                            copy = list_tag.getTypeClass().getDeclaredConstructor().newInstance();
                        }
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                    }
                }
                if (copy instanceof Tag && !(copy instanceof CompoundTag) && !(copy instanceof ListTag<?>)) {
                    g.drawImage(icons.get(tag_icon), 0, 0, null);
                }
            }
            //#endregion
    
            //#region draw the type
            {
                var copy = tag;
                if (copy instanceof ListTag<?> list_tag) {
                    try {
                        if (list_tag.getTypeClass() != EndTag.class) {
                            copy = list_tag.getTypeClass().getDeclaredConstructor().newInstance();
                        } else {
                            copy = new Object();
                        }
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                    }
                }
    
                var type = switch (copy) {
                    case ByteTag _ -> byte_icon;
                    case ShortTag _ -> short_icon;
                    case IntTag _ -> int_icon;
                    case LongTag _ -> long_icon;
                    case FloatTag _ -> float_icon;
                    case DoubleTag _ -> double_icon;
                    case ByteArrayTag _ -> byte_array_icon;
                    case StringTag _ -> string_icon;
                    case ListTag<?> _ -> list_icon;
                    case CompoundTag _ -> compound_icon;
                    case IntArrayTag _ -> int_array_icon;
                    case LongArrayTag _ -> long_array_icon;
    
                    case Byte _ -> byte_icon;
                    case Integer _ -> int_icon;
                    case Long _ -> long_icon;
                    default -> null; // should never happen
                };
    
                if (type != null)
                    g.drawImage(icons.get(type), 0, 0, null);
            }
            //#endregion
    
            //#region draw list bracket if is list
            if (tag instanceof ListTag<?>) {
                g.drawImage(icons.get(list_container_icon), 0, 0, null);
            }
            //#endregion
    
            //#region draw named tag if is named tag
            if (named) {
                g.drawImage(icons.get(named_tag_icon), 0, 0, null);
            }
            //#endregion
        }

        setIcon(new ImageIcon(buffered_image));

        return this;
    }
    public static String stringifyTag(DefaultMutableTreeNode node, NamedTag tag) {
        return strinfigyTag(node, tag.getName(), tag.getTag());
    }
    public static String strinfigyTag(DefaultMutableTreeNode node, String name, Tag<?> tag) {
        return name + ": " + stringifyTag(node, tag);
    }
    public static String stringifyTag(DefaultMutableTreeNode node, Tag<?> tag) {
        return switch (tag) {
            case ByteTag byteTag -> byteTag.valueToString();
            case ShortTag shortTag -> shortTag.valueToString();
            case IntTag intTag -> intTag.valueToString();
            case LongTag longTag -> longTag.valueToString();
            case FloatTag floatTag -> floatTag.valueToString();
            case DoubleTag doubleTag -> doubleTag.valueToString();
            case ByteArrayTag byteArrayTag -> "<" + node.getChildCount() + " bytes>";
            case StringTag stringTag -> stringTag.valueToString();
            case ListTag<?> listTag -> "<" + node.getChildCount() + " items>";
            case CompoundTag compoundTag -> "<" + node.getChildCount() + " entries>";
            case IntArrayTag intArrayTag -> "<" + node.getChildCount() + " ints>";
            case LongArrayTag longArrayTag -> "<" + node.getChildCount() + " longs>";
            default -> tag.getClass().getSimpleName();
        };
    }
}
