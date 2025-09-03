package canaryprism.mcwm.swing.nbt;

import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static canaryprism.mcwm.swing.nbt.NBTNodeIcon.*;

public class NBTNodeRenderer extends DefaultTreeCellRenderer {
    
    public static final int TAG_ICON_SIZE = 25;
    
    private final HashMap<NBTNodeIcon, Image> icons = new HashMap<>();
    {
        try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {

            var icons = NBTNodeIcon.values();

            var futures = Stream.of(icons).map((icon) -> CompletableFuture.runAsync(() -> {
                try (var is = NBTNodeRenderer.class.getResourceAsStream("/mcwm/nbt/" + icon.file_name)) {
                    assert is != null;
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

        var buffered_image = new BufferedImage(TAG_ICON_SIZE, TAG_ICON_SIZE, BufferedImage.TYPE_INT_ARGB);

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
                    g.drawImage(icons.get(TAG_ICON), 0, 0, null);
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
                            copy = EndTag.INSTANCE;
                        }
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                        e.printStackTrace();
                    }
                }
    
                var type = switch (copy) {
                    case ByteTag _ -> BYTE_ICON;
                    case ShortTag _ -> SHORT_ICON;
                    case IntTag _ -> INT_ICON;
                    case LongTag _ -> LONG_ICON;
                    case FloatTag _ -> FLOAT_ICON;
                    case DoubleTag _ -> DOUBLE_ICON;
                    case ByteArrayTag _ -> BYTE_ARRAY_ICON;
                    case StringTag _ -> STRING_ICON;
                    case ListTag<?> _ -> LIST_ICON;
                    case CompoundTag _ -> COMPOUND_ICON;
                    case IntArrayTag _ -> INT_ARRAY_ICON;
                    case LongArrayTag _ -> LONG_ARRAY_ICON;

                    case EndTag _ -> END_ICON;
    
                    case Byte _ -> BYTE_ICON;
                    case Integer _ -> INT_ICON;
                    case Long _ -> LONG_ICON;
                    default -> null; // should never happen
                };
    
                if (type != null)
                    g.drawImage(icons.get(type), 0, 0, null);
            }
            //#endregion
    
            //#region draw list bracket if is list
            if (tag instanceof ListTag<?>) {
                g.drawImage(icons.get(LIST_CONTAINER_ICON), 0, 0, null);
            }
            //#endregion
    
            //#region draw named tag if is named tag
            if (named) {
                g.drawImage(icons.get(NAMED_TAG_ICON), 0, 0, null);
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
            case ByteArrayTag _ -> "<" + node.getChildCount() + " bytes>";
            case StringTag stringTag -> stringTag.valueToString();
            case ListTag<?> _ -> "<" + node.getChildCount() + " items>";
            case CompoundTag _ -> "<" + node.getChildCount() + " entries>";
            case IntArrayTag _ -> "<" + node.getChildCount() + " ints>";
            case LongArrayTag _ -> "<" + node.getChildCount() + " longs>";
            default -> tag.getClass().getSimpleName();
        };
    }
}
