package canaryprism.mcwm.swing.nbt;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.text.StringEscapeUtils;

import javax.swing.border.EmptyBorder;

import canaryprism.mcwm.Main;
import net.querz.nbt.io.NBTUtil;
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

public class NBTView {
    private final JFrame frame;
    private final Main main;
    private final File file;
    private final NamedTag nbt;
    private final DefaultMutableTreeNode root;

    public NBTView(File file, Main main) throws IOException {
        this.frame = new JFrame("NBT Viewer");
        this.main = main;
        this.file = file;
        this.nbt = NBTUtil.read(file);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                var result = JOptionPane.showConfirmDialog(frame, "Do you want to save changes before closing?", "Save Changes", JOptionPane.YES_NO_CANCEL_OPTION);

                if (result == JOptionPane.YES_OPTION) {
                    try {
                        save();
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(frame, "Failed to save file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else if (result == JOptionPane.CANCEL_OPTION) {
                    return;
                }

                close();
            }
        });


        // Create the root node
        this.root = new DefaultMutableTreeNode(nbt);

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

        tree.setRootVisible(false);

        // tree.setFocusable(false);

        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.putClientProperty("JTree.lineStyle", "Angled");

        // frame.populateTree(root, rootDirectory);

        // Add the tree to a scroll pane
        JScrollPane scroll_pane = new JScrollPane(tree);

        // Add the scroll pane to the frame
        frame.add(scroll_pane, BorderLayout.CENTER);

        var bottom_panel = new JPanel();
        bottom_panel.setLayout(new BoxLayout(bottom_panel, BoxLayout.Y_AXIS));

        var options_panel = new JPanel();

        //#region Option panel nonsense
        {
            var add_in_button = new JButton("New Tag In...");
            add_in_button.addActionListener((e) -> {
                if (!(tree.getSelectionPath().getLastPathComponent() instanceof DefaultMutableTreeNode node))
                    throw new IllegalStateException("No node selected");

                var o = node.getUserObject();

                if (o instanceof NamedTag named_tag) {
                    o = named_tag.getTag();
                }

                var tag = o;
                var selected = tree.getSelectionPaths();
                
                Thread.ofVirtual().start(() -> {
                    try {
                        var new_tag = newTagOf(getTag(tag));
                        if (new_tag == null) {
                            return;
                        }
                        for (var path : selected) {
                            var parent = (DefaultMutableTreeNode) path.getLastPathComponent();

                            // change the list tag to the correct type if it was of type EndTag
                            {
                                var user_object = parent.getUserObject();
                                var named = false;
                                if (user_object instanceof NamedTag named_tag) {
                                    user_object = named_tag.getTag();
                                    named = true;
                                }
                                if (user_object instanceof ListTag<?> list_tag) {
                                    if (list_tag.getTypeClass() == EndTag.class) {
                                        @SuppressWarnings({ "unchecked", "rawtypes" })
                                        Object new_list = new ListTag(new_tag.getClass());
                                        if (named) {
                                            new_list = new NamedTag(((NamedTag) parent.getUserObject()).getName(), (Tag<?>)new_list);
                                        }
                                        parent.setUserObject(new_list);
                                    }
                                }
                            }

                            parent.add(new DefaultMutableTreeNode(clone(new_tag)));
                            treeModel.reload(parent);
                        }
                    } catch (CancellationException n) {
                        // do nothing
                    }
                });
            });
            options_panel.add(add_in_button);

            var add_before_button = new JButton("New Tag Before...");
            add_before_button.addActionListener((e) -> {
                if (!(tree.getSelectionPath().getLastPathComponent() instanceof DefaultMutableTreeNode node))
                    throw new IllegalStateException("No node selected");

                var o = ((DefaultMutableTreeNode) node.getParent()).getUserObject();

                if (o instanceof NamedTag named_tag) {
                    o = named_tag.getTag();
                }

                var tag = o;
                var selected = tree.getSelectionPaths();

                Thread.ofVirtual().start(() -> {
                    try {
                        var new_tag = newTagOf(getTag(tag));
                        if (new_tag == null) {
                            return;
                        }
                        for (var path : selected) {
                            var parent = (DefaultMutableTreeNode) path.getLastPathComponent();
                            var new_node = new DefaultMutableTreeNode(clone(new_tag));
                            ((DefaultMutableTreeNode) parent.getParent()).insert(new_node,
                                    parent.getParent().getIndex(parent));
                            treeModel.reload(parent.getParent());
                        }
                    } catch (CancellationException n) {
                        // do nothing
                    }
                });
            });
            options_panel.add(add_before_button);

            var add_after_button = new JButton("New Tag After...");
            add_after_button.addActionListener((e) -> {
                if (!(tree.getSelectionPath().getLastPathComponent() instanceof DefaultMutableTreeNode node))
                    throw new IllegalStateException("No node selected");

                var o = ((DefaultMutableTreeNode)node.getParent()).getUserObject();

                if (o instanceof NamedTag named_tag) {
                    o = named_tag.getTag();
                }

                var tag = o;
                var selected = tree.getSelectionPaths();
                
                Thread.ofVirtual().start(() -> {
                    try {
                        var new_tag = newTagOf(getTag(tag));
                        if (new_tag == null) {
                            return;
                        }
                        for (var path : selected) {
                            var parent = (DefaultMutableTreeNode) path.getLastPathComponent();
                            var new_node = new DefaultMutableTreeNode(clone(new_tag));
                            ((DefaultMutableTreeNode) parent.getParent()).insert(new_node, parent.getParent().getIndex(parent) + 1);
                            treeModel.reload(parent.getParent());
                        }
                    } catch (CancellationException n) {
                        // do nothing
                    }
                });
            });
            options_panel.add(add_after_button);

            var rename_button = new JButton("Rename");
            rename_button.addActionListener((e) -> {
                if (!(tree.getSelectionPath().getLastPathComponent() instanceof DefaultMutableTreeNode node))
                    throw new IllegalStateException("No node selected");
                
                var tag = (NamedTag)node.getUserObject();

                var selected = tree.getSelectionPaths();
                
                Thread.ofVirtual().start(() -> {
                    var new_name = JOptionPane.showInputDialog(frame, "Enter new name", tag.getName());
                    if (new_name == null) {
                        return;
                    }

                    for (var path : selected) {
                        var parent = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (parent.getUserObject() instanceof NamedTag named_tag) {
                            named_tag.setName(new_name);
                            treeModel.reload(parent);
                        }
                    }
                });
            });
            options_panel.add(rename_button);

            var edit_button = new JButton("Edit");
            edit_button.addActionListener((e) -> {
                if (!(tree.getSelectionPath().getLastPathComponent() instanceof DefaultMutableTreeNode node))
                    throw new IllegalStateException("No node selected");

                var o = node.getUserObject();
                var named = o instanceof NamedTag;
                var o1 = o;
                if (o instanceof NamedTag named_tag) {
                    o = named_tag.getTag();
                }

                var tag = o;
                var selected = tree.getSelectionPaths();

                Thread.ofVirtual().start(() -> {
                    try {
                        Object new_tag = edit(tag);
                        if (new_tag == null) {
                            return;
                        }
                        if (named) 
                            new_tag = new NamedTag(((NamedTag) o1).getName(), (Tag<?>)new_tag);
    
                        for (var path : selected) {
                            var parent = (DefaultMutableTreeNode) path.getLastPathComponent();
                            parent.setUserObject(clone(new_tag));
                        }
    
                        treeModel.reload(node);
                    } catch (CancellationException n) {
                        // do nothing
                    }
                });
            });
            options_panel.add(edit_button);

            var delete_button = new JButton("Delete");
            delete_button.addActionListener((e) -> {
                if (!(tree.getSelectionPath().getLastPathComponent() instanceof DefaultMutableTreeNode))
                    throw new IllegalStateException("No node selected");

                var selected = tree.getSelectionPaths();

                Thread.ofVirtual().start(() -> {
                    for (var path : selected) {
                        var child = (DefaultMutableTreeNode) path.getLastPathComponent();
                        var parent = (DefaultMutableTreeNode) child.getParent();
                        child.removeFromParent();
                        treeModel.reload(parent);
                    }
                    tree.setSelectionPaths(null);
                });
            });
            options_panel.add(delete_button);

            var duplicate_button = new JButton("Duplicate");
            duplicate_button.addActionListener((e) -> {
                if (!(tree.getSelectionPath().getLastPathComponent() instanceof DefaultMutableTreeNode node))
                    throw new IllegalStateException("No node selected");

                var selected = tree.getSelectionPaths();

                Thread.ofVirtual().start(() -> {
                    for (var path : selected) {
                        var parent = (DefaultMutableTreeNode) path.getLastPathComponent();
                        var node_tree = reconstructNbt(parent.getUserObject(), parent);
                        var new_node = new DefaultMutableTreeNode(node_tree);
                        load(new_node);
                        parent = (DefaultMutableTreeNode) parent.getParent();
                        parent.insert(new_node, parent.getIndex(node) + 1);
                        treeModel.reload(parent);
                    }
                });
            });
            options_panel.add(duplicate_button);

            tree.addTreeSelectionListener((_) -> {
                var selected = tree.getSelectionPaths();
                boolean add_in = false, add_insert = false, rename = false, edit = false, delete = false, duplicate = false;

                if (selected != null && selected.length > 0) {
                    var node = (DefaultMutableTreeNode) selected[0].getLastPathComponent();
                    var o = node.getUserObject();

                    boolean all_same_type = true, contains_root = false, all_named = true;

                    for (var path : selected) {
                        var parent = (DefaultMutableTreeNode) path.getLastPathComponent();
                        var e = parent.getUserObject();
                        if (e.getClass() != o.getClass()) {
                            all_same_type = false;
                        } else if (o instanceof NamedTag named_tag && e instanceof NamedTag named_tag2) {
                            if (named_tag.getTag().getClass() != named_tag2.getTag().getClass()) {
                                all_same_type = false;
                            }
                        } else if (o instanceof ListTag<?> list_tag && e instanceof ListTag<?> list_tag2) {
                            if (list_tag.getTypeClass() != list_tag2.getTypeClass()) {
                                all_same_type = false;
                            }
                        }
                        if (!(e instanceof NamedTag)) {
                            all_named = false;
                        }
                        if (parent == root) {
                            contains_root = true;
                        }
                    }

                    if (o instanceof NamedTag named_tag) {
                        o = named_tag.getTag();
                    }
                    var tag = o;
                    
                    add_in = all_same_type && (tag instanceof CompoundTag || tag instanceof ListTag || tag instanceof IntArrayTag || tag instanceof LongArrayTag || tag instanceof ByteArrayTag);
                    
                    add_insert = all_same_type && !contains_root;
                    
                    rename = all_named;

                    edit = all_same_type && (tag instanceof ByteTag || tag instanceof ShortTag || tag instanceof IntTag || tag instanceof LongTag || tag instanceof FloatTag || tag instanceof DoubleTag || tag instanceof StringTag || tag instanceof Byte || tag instanceof Integer || tag instanceof Long);

                    delete = !contains_root;
                    duplicate = !contains_root;
                }

                add_in_button.setEnabled(add_in);
                add_before_button.setEnabled(add_insert);
                add_after_button.setEnabled(add_insert);
                rename_button.setEnabled(rename);
                edit_button.setEnabled(edit);
                delete_button.setEnabled(delete);
                duplicate_button.setEnabled(duplicate);
            });

            add_in_button.setEnabled(false);
            add_before_button.setEnabled(false);
            add_after_button.setEnabled(false);
            rename_button.setEnabled(false);
            edit_button.setEnabled(false);
            delete_button.setEnabled(false);
            duplicate_button.setEnabled(false);
        }
        //#endregion

        bottom_panel.add(options_panel);

        var save_panel = new JPanel();
        {
            var close_button = new JButton("Discard Changes and Close");
            close_button.addActionListener((e) -> {
                Thread.ofVirtual().start(() -> {
                    close();
                });
            });
            save_panel.add(close_button);


            save_panel.add(Box.createHorizontalStrut(100));

            var save_button = new JButton("Save");
            save_button.addActionListener((e) -> {
                Thread.ofVirtual().start(() -> {
                    try {
                        save();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        JOptionPane.showMessageDialog(frame, "Failed to save file: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            });
            save_panel.add(save_button);

            var save_and_close_button = new JButton("Save and Close");
            save_and_close_button.addActionListener((e) -> {
                Thread.ofVirtual().start(() -> {
                    try {
                        save();
                        close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        JOptionPane.showMessageDialog(frame, "Failed to save file: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            });
            save_panel.add(save_and_close_button);

        }
        bottom_panel.add(save_panel);

        frame.add(bottom_panel, BorderLayout.SOUTH);

        tree.getSelectionModel().clearSelection();

        frame.pack();

        frame.setVisible(true);
    }

    // public void load(NamedNBTNode node) {
    //     load(node, node.getNamedTag().getTag());
    // }
    // public void load(NBTNode<?> node) {
    //     load(node, node.getTag());
    // }
    public static void load(DefaultMutableTreeNode node) {
        var o = node.getUserObject();
        if (o instanceof NamedTag named_tag) {
            o = named_tag.getTag();
        }
        switch (o) {
            case CompoundTag compound_tag -> {
                for (var entry : compound_tag) {
                    var child = new DefaultMutableTreeNode(new NamedTag(entry.getKey(), entry.getValue().clone()));
                    load(child);
                    node.add(child);
                }
            }
            case ListTag<?> list_tag -> {
                for (var entry : list_tag) {
                    var child = new DefaultMutableTreeNode(entry.clone());
                    load(child);
                    node.add(child);
                }
            }
            case IntArrayTag int_array_tag -> {
                for (var entry : int_array_tag.getValue()) {
                    var child = new DefaultMutableTreeNode(entry);
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

    public static Object reconstructNbt(Object o, DefaultMutableTreeNode node) {
        return switch (o) {
            case NamedTag named_tag -> reconstructNbt(named_tag, node);
            case Tag<?> tag -> reconstructNbt(tag, node);
            default -> throw new IllegalArgumentException("Cannot reconstruct " + o.getClass().getSimpleName());
        };
    }
    public static NamedTag reconstructNbt(NamedTag tag, DefaultMutableTreeNode node) {
        return new NamedTag(tag.getName(), reconstructNbt(tag.getTag(), node));
    }
    public static Tag<?> reconstructNbt(Tag<?> tag, DefaultMutableTreeNode node) {
        return switch (tag) {
            case CompoundTag compound_tag -> {
                var new_tag = new CompoundTag();
                for (var i = 0; i < node.getChildCount(); ++i) {
                    var child = (DefaultMutableTreeNode) node.getChildAt(i);
                    var named_tag = (NamedTag) child.getUserObject();
                    new_tag.put(named_tag.getName(), reconstructNbt(named_tag.getTag(), child));
                }
                yield new_tag;
            }
            case ListTag<?> list_tag -> {
                @SuppressWarnings({"unchecked"})
                var new_tag = (ListTag<Tag<?>>)ListTag.createUnchecked(list_tag.getTypeClass());
                for (var i = 0; i < node.getChildCount(); ++i) {
                    var child = (DefaultMutableTreeNode) node.getChildAt(i);
                    new_tag.add(reconstructNbt((Tag<?>) child.getUserObject(), child));
                }
                yield new_tag;
            }
            case IntArrayTag int_array_tag -> {
                var arr = new int[node.getChildCount()];
                for (var i = 0; i < node.getChildCount(); ++i) {
                    var child = (DefaultMutableTreeNode) node.getChildAt(i);
                    arr[i] = (int) child.getUserObject();
                }
                int_array_tag.setValue(arr);
                yield int_array_tag;
            }
            case LongArrayTag long_array_tag -> {
                var arr = new long[node.getChildCount()];
                for (var i = 0; i < node.getChildCount(); ++i) {
                    var child = (DefaultMutableTreeNode) node.getChildAt(i);
                    arr[i] = (long) child.getUserObject();
                }
                long_array_tag.setValue(arr);
                yield long_array_tag;
            }
            case ByteArrayTag byte_array_tag -> {
                var arr = new byte[node.getChildCount()];
                for (var i = 0; i < node.getChildCount(); ++i) {
                    var child = (DefaultMutableTreeNode) node.getChildAt(i);
                    arr[i] = (byte) child.getUserObject();
                }
                byte_array_tag.setValue(arr);
                yield byte_array_tag;
            }
            default -> tag;
        };
    }

    public void save() throws IOException {
        var nbt = reconstructNbt(this.nbt, this.root);

        NBTUtil.write(nbt, file);

        main.reloadAllWorlds();
    }

    public void close() {
        frame.dispose();
        main.reloadAllWorlds();
    }

    enum TagType {
        compound, list, int_array, long_array, byte_array,
        byte_, short_, int_, long_, float_, double_, string_;

        public Class<?> getTypeClass() {
            return switch (this) {
                case compound -> CompoundTag.class;
                case list -> ListTag.class;
                case int_array -> IntArrayTag.class;
                case long_array -> LongArrayTag.class;
                case byte_array -> ByteArrayTag.class;
                case byte_ -> ByteTag.class;
                case short_ -> ShortTag.class;
                case int_ -> IntTag.class;
                case long_ -> LongTag.class;
                case float_ -> FloatTag.class;
                case double_ -> DoubleTag.class;
                case string_ -> StringTag.class;
            };
        }

        @Override
        public String toString() {
            return switch (this) {
                case compound -> "Compound";
                case list -> "List";
                case int_array -> "Int Array";
                case long_array -> "Long Array";
                case byte_array -> "Byte Array";
                case byte_ -> "Byte";
                case short_ -> "Short";
                case int_ -> "Int";
                case long_ -> "Long";
                case float_ -> "Float";
                case double_ -> "Double";
                case string_ -> "String";
            };
        }
    }

    private static RuntimeException unchecked(Throwable t) {
        return switch (t) {
            case RuntimeException e -> e;
            case Error e -> new RuntimeException(e);
            default -> new RuntimeException(t);
        };
    }

    private Object newTagOf(Tag<?> parent) {

        return switch (parent) {
            case CompoundTag compound_tag -> {
                var dialog = new JDialog(frame);
                var future = new CompletableFuture<Object>();
            
                var panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.setBorder(new EmptyBorder(5, 5, 5, 5));

                var name_field = new JTextField();
                panel.add(name_field);

                var container_type_box = new JComboBox<TagType>();
                container_type_box.setEnabled(false);

                var type_box = new JComboBox<TagType>(TagType.values());
                type_box.addActionListener((e) -> {
                    var type = (TagType) type_box.getSelectedItem();
                    if (type == TagType.list) {
                        container_type_box.setModel(new DefaultComboBoxModel<>(TagType.values()));
                        container_type_box.setEnabled(true);
                    } else {
                        container_type_box.setEnabled(false);
                        container_type_box.setModel(new DefaultComboBoxModel<>());
                    }
                });

                panel.add(type_box);
                panel.add(container_type_box);

                var ok_button = new JButton("OK");
                ok_button.addActionListener((e) -> {
                    var name = name_field.getText();
                    var type = (TagType) type_box.getSelectedItem();
                    Tag<?> new_tag = switch (type) {
                        case compound -> new CompoundTag();
                        case list -> {
                            var container_type = (TagType) container_type_box.getSelectedItem();
                            var contain_type = container_type.getTypeClass();
                            @SuppressWarnings({ "unchecked", "rawtypes" })
                            var tag = new ListTag(contain_type);
                            yield tag;
                        }
                        case int_array -> new IntArrayTag();
                        case long_array -> new LongArrayTag();
                        case byte_array -> new ByteArrayTag();
                        case byte_ -> new ByteTag();
                        case double_ -> new DoubleTag();
                        case float_ -> new FloatTag();
                        case int_ -> new IntTag();
                        case long_ -> new LongTag();
                        case short_ -> new ShortTag();
                        case string_ -> new StringTag();
                    };
                    future.complete(new NamedTag(name, new_tag));
                });
                panel.add(ok_button);

                dialog.setContentPane(panel);
                dialog.pack();
                dialog.setVisible(true);

                dialog.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentHidden(java.awt.event.ComponentEvent e) {
                        future.cancel(false);
                    }
                });

                var result = future.handle((e, t) -> {
                    dialog.dispose();
                    if (t != null)
                        throw unchecked(t);
                    return e;
                }).join();
                yield result;
            }

            case ListTag<?> list_tag -> {
                var type = list_tag.getTypeClass();
                Object result;
                if (type == ListTag.class) {
                    var dialog = new JDialog(frame);
                    var future = new CompletableFuture<Object>();

                    var panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

                    var container_type_box = new JComboBox<TagType>(TagType.values());
                    panel.add(container_type_box);

                    var ok_button = new JButton("OK");
                    ok_button.addActionListener((e) -> {
                        var container_type = (TagType) container_type_box.getSelectedItem();
                        var contain_type = container_type.getTypeClass();
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        var tag = new ListTag(contain_type);
                        future.complete(tag);
                    });
                    panel.add(ok_button);

                    dialog.setContentPane(panel);
                    dialog.pack();
                    dialog.setVisible(true);
                    dialog.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentHidden(java.awt.event.ComponentEvent e) {
                            future.cancel(false);
                        }
                    });

                    result = future.handle((e, t) -> {
                        dialog.dispose();
                        if (t != null)
                            throw unchecked(t);
                        return e;
                    }).join();
                } else if (type == EndTag.class) {
                    // you'll be able to add any tag to this list
                    var dialog = new JDialog(frame);
                    var future = new CompletableFuture<Object>();

                    var panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                    panel.setBorder(new EmptyBorder(5, 5, 5, 5));

                    var container_type_box = new JComboBox<TagType>();
                    container_type_box.setEnabled(false);

                    var type_box = new JComboBox<TagType>(TagType.values());
                    type_box.addActionListener((e) -> {
                        var selected_type = (TagType) type_box.getSelectedItem();
                        if (selected_type == TagType.list) {
                            container_type_box.setModel(new DefaultComboBoxModel<>(TagType.values()));
                            container_type_box.setEnabled(true);
                        } else {
                            container_type_box.setEnabled(false);
                            container_type_box.setModel(new DefaultComboBoxModel<>());
                        }
                    });

                    panel.add(type_box);
                    panel.add(container_type_box);

                    var ok_button = new JButton("OK");
                    ok_button.addActionListener((e) -> {
                        var selected_type = (TagType) type_box.getSelectedItem();
                        Tag<?> new_tag = switch (selected_type) {
                            case compound -> new CompoundTag();
                            case list -> {
                                var container_type = (TagType) container_type_box.getSelectedItem();
                                var contain_type = container_type.getTypeClass();
                                @SuppressWarnings({ "unchecked", "rawtypes" })
                                var tag = new ListTag(contain_type);
                                yield tag;
                            }
                            case int_array -> new IntArrayTag();
                            case long_array -> new LongArrayTag();
                            case byte_array -> new ByteArrayTag();
                            case byte_ -> new ByteTag();
                            case double_ -> new DoubleTag();
                            case float_ -> new FloatTag();
                            case int_ -> new IntTag();
                            case long_ -> new LongTag();
                            case short_ -> new ShortTag();
                            case string_ -> new StringTag();
                        };
                        future.complete(new_tag);
                    });
                    panel.add(ok_button);

                    dialog.setContentPane(panel);
                    dialog.pack();
                    dialog.setVisible(true);

                    dialog.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentHidden(java.awt.event.ComponentEvent e) {
                            future.cancel(false);
                        }
                    });

                    result = future.handle((e, t) -> {
                        dialog.dispose();
                        if (t != null)
                            throw unchecked(t);
                        return e;
                    }).join();
                } else {
                    try {
                        result = type.getDeclaredConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                        throw new RuntimeException(e);
                    }
                }
                yield result;
            }
            case IntArrayTag int_array_tag -> (Integer) 0;
            case LongArrayTag long_array_tag -> (Long) 0L;
            case ByteArrayTag byte_array_tag -> (Byte) (byte) 0;

            default -> null;
        };
    }

    private Object edit(Object o) {
        return switch (o) {
            case ByteTag e -> new ByteTag((byte)prompt("Enter new byte value", Byte::parseByte, e.valueToString()));
            case ShortTag e -> new ShortTag(prompt("Enter new short value", Short::parseShort, e.valueToString()));
            case IntTag e -> new IntTag(prompt("Enter new int value", Integer::parseInt, e.valueToString()));
            case LongTag e -> new LongTag(prompt("Enter new long value", Long::parseLong, e.valueToString()));
            case FloatTag e -> new FloatTag(prompt("Enter new float value", Float::parseFloat, e.valueToString()));
            case DoubleTag e -> new DoubleTag(prompt("Enter new double value", Double::parseDouble, e.valueToString()));
            case StringTag e -> new StringTag(prompt("Enter new string value (Java escape codes are supported)", StringEscapeUtils::unescapeJava, e.getValue()));
            case Byte _ -> prompt("Enter new byte value", Byte::parseByte, o.toString());
            case Integer _ -> prompt("Enter new int value", Integer::parseInt, o.toString());
            case Long _ -> prompt("Enter new long value", Long::parseLong, o.toString());
            default -> throw new IllegalArgumentException("Cannot edit " + o.getClass().getSimpleName());
        };
    }

    private <T> T prompt(String message, Function<String, ? extends T> converter, String def) {
        var dialog = new JDialog(frame);
        var future = new CompletableFuture<T>();

        var panel = new JPanel();
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        var message_panel = new JPanel();

        var message_label = new JLabel(message);

        message_panel.add(message_label);
        panel.add(message_panel);


        panel.add(Box.createVerticalStrut(5));

        var value_field = new JTextField(def);
        panel.add(value_field);

        panel.add(Box.createVerticalStrut(5));

        var ok_button = new JButton("Done");
        ok_button.addActionListener((e) -> {
            var value = converter.apply(value_field.getText());
            future.complete(value);
        });

        // only allow the dialog to be closed if the input is valid
        value_field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                check();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                check();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                check();
            }

            void check() {
                try {
                    ok_button.setEnabled(converter.apply(value_field.getText()) != null);
                } catch (Exception e) {
                    ok_button.setEnabled(false);
                }
            }
        });

        panel.add(ok_button);

        dialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(java.awt.event.ComponentEvent e) {
                future.cancel(false);
            }
        });

        dialog.setContentPane(panel);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setVisible(true);


    

        var result = future.handle((e, t) -> {
            dialog.dispose();
            if (t != null) 
                throw unchecked(t);
            return e;
        }).join();

        return result;
    }

    private static Object clone(Object o) {
        return switch (o) {
            case Number n -> n;
            case Tag<?> tag -> tag.clone();
            case NamedTag named_tag -> new NamedTag(named_tag.getName(), named_tag.getTag().clone());
            case Cloneable c -> {
                try {
                    yield c.getClass().getMethod("clone").invoke(c);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> throw new IllegalArgumentException("Cannot clone " + o.getClass().getSimpleName());
        };
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
