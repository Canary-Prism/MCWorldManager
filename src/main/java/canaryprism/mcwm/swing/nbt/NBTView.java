package canaryprism.mcwm.swing.nbt;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.jar.Attributes.Name;

import javax.swing.DropMode;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

import canaryprism.mcwm.swing.nbt.nodes.NBTNode;
import canaryprism.mcwm.swing.nbt.nodes.NamedNBTNode;
import canaryprism.mcwm.swing.nbt.nodes.primitive.IntNode;
import net.querz.nbt.io.NBTUtil;
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

public class NBTView {
    private final JFrame frame;

    public NBTView(NamedTag nbt) {
        this.frame = new JFrame("NBT Viewer");

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);


        // Create the root node
        var root = new DefaultMutableTreeNode(nbt);

        load(root);

        // Create the tree model and set the root node
        DefaultTreeModel treeModel = new DefaultTreeModel(root);

        // Create the tree
        JTree tree = new JTree(treeModel);

        tree.setShowsRootHandles(true);
        // tree.setEditable(true);

        tree.setToggleClickCount(0);

        // tree.addMouseListener(new MouseAdapter() {
        //     public void mousePressed(MouseEvent e) {
        //         if (e.getClickCount() == 2) {
        //             int row = tree.getRowForLocation(e.getX(), e.getY());
        //             if (row != -1) {
        //                 tree.startEditingAtPath(tree.getPathForRow(row));
        //             }
        //         }
        //     }
        // });

        var renderer = new NBTNodeRenderer();

        tree.setCellRenderer(renderer);

        var transfer_handler = new NBTTreeTransferHandler();
        tree.setTransferHandler(transfer_handler);
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.INSERT);


        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.putClientProperty("JTree.lineStyle", "Angled");

        // frame.populateTree(root, rootDirectory);
        root.add(new DefaultMutableTreeNode("mewo"));

        // Add the tree to a scroll pane
        JScrollPane scrollPane = new JScrollPane(tree);

        // Add the scroll pane to the frame
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    // public void load(NamedNBTNode node) {
    //     load(node, node.getNamedTag().getTag());
    // }
    // public void load(NBTNode<?> node) {
    //     load(node, node.getTag());
    // }
    public void load(DefaultMutableTreeNode node) {
        var o = node.getUserObject();
        if (o instanceof NamedTag named_tag) {
            o = named_tag.getTag();
        }
        switch (o) {
            case CompoundTag compound_tag -> {
                for (var entry : compound_tag) {
                    var child = new DefaultMutableTreeNode(new NamedTag(entry.getKey(), entry.getValue()));
                    load(child);
                    node.add(child);
                }
            }
            case ListTag<?> list_tag -> {
                for (var entry : list_tag) {
                    var child = new DefaultMutableTreeNode(entry);
                    load(child);
                    node.add(child);
                }
            }
            case IntArrayTag int_array_tag -> {
                for (var entry : int_array_tag.getValue()) {
                    var child = new IntNode(entry);
                    node.add(child);
                }
            }
            case LongArrayTag long_array_tag -> {
                for (var entry : long_array_tag.getValue()) {
                    var child = new DefaultMutableTreeNode(entry);
                    node.add(child);
                }
            }
            case ByteArrayTag byte_array_tag -> {
                for (var entry : byte_array_tag.getValue()) {
                    var child = new DefaultMutableTreeNode(entry);
                    node.add(child);
                }
            }
            default -> {}
        }
    }
    
    public static Tag<?> getTag(Object o) {
        return switch (o) {
            case Tag<?> tag -> tag;
            case NamedTag named_tag -> named_tag.getTag();
            default -> null;
        };
    }

    public static boolean canHold(Tag<?> tag, Object o) {
        return switch (tag) {
            case CompoundTag compound_tag -> o instanceof NamedTag;
            case ListTag<?> list_tag -> list_tag.getTypeClass().isInstance(o);
            case IntArrayTag int_array_tag -> o instanceof Integer;
            case LongArrayTag long_array_tag -> o instanceof Long;
            case ByteArrayTag byte_array_tag -> o instanceof Byte;
            default -> false;
        };
    }
}
