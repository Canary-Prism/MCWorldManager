package canaryprism.mcwm.swing;

import canaryprism.mcwm.Main;
import canaryprism.mcwm.saves.ParsingException;
import canaryprism.mcwm.saves.WorldData;
import canaryprism.mcwm.swing.file.LoadedFile;
import canaryprism.mcwm.swing.file.UnknownFile;
import canaryprism.mcwm.swing.file.WorldFile;
import canaryprism.mcwm.swing.nbt.NBTView;
import org.apache.commons.io.file.PathUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static canaryprism.mcwm.Main.*;
import static java.nio.file.StandardWatchEventKinds.*;

public class SaveView extends JComponent implements Closeable {
    
    private final Main main;
    private final Path save_path;
    private final JList<LoadedFile> list;
    private final List<LoadedFile> worlds = new ArrayList<>();
    
    private WatchService watch_service;
    
    public SaveView(Main main, Path save_path) {
        
        this.main = main;
        this.save_path = save_path;
        
        this.setLayout(new BorderLayout());
        
        if (!Files.exists(save_path)) {
            throw new IllegalArgumentException("Minecraft saves folder does not exist: " + save_path);
        }
        if (!Files.isDirectory(save_path)) {
            throw new IllegalArgumentException("Not a folder: " + save_path);
        }
        
        this.list = new JList<>();
        
        reloadAllWorlds();
        var auto_refresh_checkbox = new JCheckBox("Auto Refresh");
        auto_refresh_checkbox.setSelected(true);
        Thread.ofVirtual().start(() -> {
            try {
                watch_service = FileSystems.getDefault().newWatchService();
                save_path.register(watch_service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                
                WatchKey key;
                while ((key = watch_service.take()) != null) {
                    for (var _ : key.pollEvents()) {
                        if (auto_refresh_checkbox.isSelected()) {
                            reloadAllWorlds();
                        }
                    }
                    key.reset();
                }
                
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        
        var world_list_pane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        final var selected_color = Optional.ofNullable(UIManager.getColor("List.selectionBorderColor")).orElse(new Color(0xabcdef));
        // final var selected_color = new Color(0xabcdef);
        
        list.setCellRenderer((_, value, _, selected, _) -> {
            var entry = WorldListEntry.of(value);
            entry.setPreferredSize(new Dimension(500, 50));
            var panel = new JPanel(new BorderLayout());
            panel.add(entry, BorderLayout.CENTER);
            if (selected) {
                panel.setBorder(new LineBorder(selected_color, 2));
            } else {
                panel.setBorder(new EmptyBorder(2, 2, 2, 2));
            }
            if (value instanceof WorldFile world && !Files.isDirectory(world.path())) {
                panel.setToolTipText("Warning: This world is compressed and won't be loaded by Minecraft");
            }
            return panel;
        });
        list.setFocusable(false);
        
        // list.setPreferredSize(new Dimension(500, 500));
        
        list.addListSelectionListener((e) -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            updateOptionsPanel();
        });
        
        world_list_pane.setViewportView(list);
        world_list_pane.setMinimumSize(new Dimension(500, 80));
        
        world_list_pane.setDropTarget(new DropTarget() {
            @SuppressWarnings("unchecked")
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    Collection<File> dropped_files = (Collection<File>) evt.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    Thread.ofPlatform().start(() -> {
                        var list = dropped_files.stream().parallel().filter((e) -> {
                            if (e.getName().endsWith(".dat") || e.getName().endsWith(".dat_old")) {
                                try {
                                    new NBTView(e, SaveView.this);
                                } catch (IOException _) {
                                }
                                return false;
                            }
                            return true;
                        }).toList();
                        if (list.isEmpty()) {
                            return;
                        }
                        importWorlds(list);
                    });
                    evt.dropComplete(true);
                } catch (Exception ex) {
                }
            }
        });
        
        this.add(world_list_pane, BorderLayout.CENTER);
        
        var bottom_panel = new JPanel();
        bottom_panel.setLayout(new BoxLayout(bottom_panel, BoxLayout.Y_AXIS));
        
        card = new CardLayout();
        option_buttons_panel = new JPanel(card);
        option_buttons_panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // world_options_panel = new JPanel();
        // archive_options_panel = new JPanel();
        // unknown_options_panel = new JPanel();
        
        initialiseOptions();
        
        updateOptionsPanel();
        
        bottom_panel.add(option_buttons_panel);
        
        var action_panel = new JPanel();
        action_panel.setLayout(new BoxLayout(action_panel, BoxLayout.X_AXIS));
        
        var import_button = new JButton("Import World");
        import_button.addActionListener((e) -> {
            importButton();
        });
        import_button.setToolTipText("Import a Minecraft world");
        
        var open_button = new JButton("Open Folder");
        if (Desktop.isDesktopSupported()) {
            open_button.addActionListener((_) -> open(save_path));
            open_button.setToolTipText("Open the Minecraft saves folder");
        } else {
            open_button.setEnabled(false);
            open_button.setToolTipText("Open the Minecraft saves folder (not supported on this computer)");
        }
        
        
        var refresh_button = new JButton("Refresh");
        refresh_button.addActionListener((_) -> {
            reloadAllWorlds();
        });
        refresh_button.setToolTipText("Refresh the list of worlds");
        
        auto_refresh_checkbox.setToolTipText("Automatically refresh the list of worlds when a change is detected in the folder");
        
        action_panel.add(import_button);
        action_panel.add(open_button);
        action_panel.add(refresh_button);
        action_panel.add(auto_refresh_checkbox);
        
        bottom_panel.add(action_panel);
        
        this.add(bottom_panel, BorderLayout.SOUTH);
    }
    
    
    private void initialiseOptions() {
        //#region init World Options
        {
            var export_button = new JButton("Share");
            export_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                if (selected instanceof WorldFile world) {
                    main.export(world);
                }
            });
            export_button.setToolTipText("Export the world to a zip file to share it with others");
            
            var copy_button = new JButton("Copy");
            copy_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                main.copy(selected);
            });
            copy_button.setToolTipText("Copy the world folder to clipboard");
            
            var delete_button = new JButton("Delete");
            delete_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                main.delete(selected);
            });
            delete_button.setToolTipText("Delete the world forever");
            
            var edit_nbt_button = new JButton("Edit NBT Data");
            edit_nbt_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                if (selected instanceof WorldFile world) {
                    Thread.ofVirtual().start(() -> {
                        try {
                            new NBTView(world.path().resolve("level.dat").toFile(), SaveView.this);
                        } catch (IOException e1) {
                            JOptionPane.showMessageDialog(this
                                    , "Failed to open NBT Editor: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            });
            edit_nbt_button.setToolTipText("Advanced: Edit the NBT data of the world");
            
            var panel = new JPanel();
            
            panel.add(export_button);
            panel.add(copy_button);
            panel.add(delete_button);
            
            panel.add(Box.createHorizontalStrut(50));
            panel.add(edit_nbt_button);
            
            option_buttons_panel.add(panel, SelectedType.WORLD.name());
        }
        //#endregion
        //#region init Archive Options
        {
            var expand_button = new JButton("Expand");
            expand_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                if (selected instanceof WorldFile world) {
                    expand(world);
                }
            });
            expand_button.setToolTipText("Expand the archive so Minecraft can load it");
            
            var copy_button = new JButton("Copy");
            copy_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                main.copy(selected);
            });
            copy_button.setToolTipText("Copy the archive to clipboard");
            
            var delete_button = new JButton("Delete");
            delete_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                main.delete(selected);
            });
            delete_button.setToolTipText("Delete the world forever");
            
            var panel = new JPanel();
            
            panel.add(expand_button);
            panel.add(copy_button);
            panel.add(delete_button);
            
            option_buttons_panel.add(panel, SelectedType.ARCHIVE.name());
        }
        //#endregion
        //#region init Unknown Options
        {
            var delete_button = new JButton("Delete");
            delete_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                main.delete(selected);
            });
            delete_button.setToolTipText("Delete the file forever");
            
            var error_button = new JButton("Error Details");
            error_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                if (selected instanceof UnknownFile unknown) {
                    Thread.ofVirtual().start(() -> {
                        var message = getStackTraceAsString(unknown.exception());
                        JOptionPane.showMessageDialog(this
                                , message, "Error Details", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
            error_button.setToolTipText("View the error details");
            
            var copy_button = new JButton("Copy");
            copy_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                main.copy(selected);
            });
            copy_button.setToolTipText("Copy the file to clipboard");
            
            
            var panel = new JPanel();
            
            panel.add(delete_button);
            panel.add(error_button);
            panel.add(copy_button);
            
            option_buttons_panel.add(panel, SelectedType.UNKNOWN.name());
        }
        //#endregion
        //#region init Unknown with Level Options
        {
            var delete_button = new JButton("Delete");
            delete_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                main.delete(selected);
            });
            delete_button.setToolTipText("Delete the file forever");
            
            var error_button = new JButton("Error Details");
            error_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                if (selected instanceof UnknownFile unknown) {
                    Thread.ofVirtual().start(() -> {
                        var message = getStackTraceAsString(unknown.exception());
                        JOptionPane.showMessageDialog(this
                                , message, "Error Details", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
            error_button.setToolTipText("View the error details");
            
            var copy_button = new JButton("Copy");
            copy_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                main.copy(selected);
            });
            copy_button.setToolTipText("Copy the file to clipboard");
            
            var edit_nbt_button = new JButton("Edit NBT Data");
            edit_nbt_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                if (selected instanceof UnknownFile file) {
                    Thread.ofVirtual().start(() -> {
                        try {
                            new NBTView(file.path().resolve("level.dat").toFile(), SaveView.this);
                        } catch (IOException e1) {
                            JOptionPane.showMessageDialog(this
                                    , "Failed to open NBT Editor: " + e1.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            });
            edit_nbt_button.setToolTipText("Advanced: Edit the NBT data of the found level.dat file");
            
            var panel = new JPanel();
            
            panel.add(delete_button);
            panel.add(error_button);
            panel.add(copy_button);
            
            panel.add(Box.createHorizontalStrut(50));
            panel.add(edit_nbt_button);
            
            option_buttons_panel.add(panel, SelectedType.UNKNOWN_WITH_LEVEL.name());
        }
        
        //#region init None Options
        {
            var panel = new JPanel();
            panel.add(new JLabel("<Select an item to see options>"));
            option_buttons_panel.add(panel, SelectedType.NONE.name());
        }
    }
    
    private final JPanel option_buttons_panel;
    private final CardLayout card;
    

    
    enum SelectedType {
        NONE, WORLD, ARCHIVE, UNKNOWN, UNKNOWN_WITH_LEVEL;
    }
    private void updateOptionsPanel() {
        switch (list.getSelectedValue()) {
            case WorldFile e -> {
                if (Files.isDirectory(e.path())) {
                    card.show(option_buttons_panel, SelectedType.WORLD.name());
                } else {
                    card.show(option_buttons_panel, SelectedType.ARCHIVE.name());
                }
            }
            case UnknownFile e -> {
                if (Files.isDirectory(e.path()) && Files.exists(e.path().resolve("level.dat"))) {
                    card.show(option_buttons_panel, SelectedType.UNKNOWN_WITH_LEVEL.name());
                } else {
                    card.show(option_buttons_panel, SelectedType.UNKNOWN.name());
                }
            }
            case null -> {
                card.show(option_buttons_panel, SelectedType.NONE.name());
            }
        }
    }
    
    
    
    public void reloadAllWorlds() {
        Thread.ofVirtual().start(() -> {
            
            try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {
                synchronized (worlds) {
                    worlds.clear();
                    
                    
                    List<CompletableFuture<Void>> futures;
                    try (var stream = Files.list(save_path)) {
                        futures = stream.map((path) -> CompletableFuture.runAsync(() -> {
                            try {
                                var file = new WorldFile(path, WorldData.parse(path));
                                synchronized (ex) {
                                    worlds.add(file);
                                }
                            } catch (ParsingException e) {
                                var file = new UnknownFile(path, e);
                                synchronized (ex) {
                                    worlds.add(file);
                                }
                            }
                        }, ex)).toList();
                    }
                    
                    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
                    
                    SwingUtilities.invokeLater(() -> {
                        list.setListData(worlds.stream().sorted((a, b) -> {
                            if (a instanceof WorldFile world_a && b instanceof WorldFile world_b) {
                                return -world_a.data().lastPlayed().compareTo(world_b.data().lastPlayed());
                            } else if (a instanceof WorldFile) {
                                return -1;
                            } else if (b instanceof WorldFile) {
                                return 1;
                            } else {
                                try {
                                    return Files.getLastModifiedTime(b.path()).compareTo(Files.getLastModifiedTime(a.path()));
                                } catch (IOException e) {
                                    return 0; // eh, whatever
                                }
                            }
                        }).toArray(LoadedFile[]::new));
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this
                        , "Failed to load worlds: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private final JFileChooser import_fc = new JFileChooser();
    
    {
        import_fc.setDialogTitle("Import World");
        import_fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        import_fc.setMultiSelectionEnabled(true);
        import_fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory()
                        || f.getName().endsWith(".zip")
                        || f.getName().endsWith(".7z")
                        || f.getName().endsWith(".tar")
                        || f.getName().endsWith(".gz");
                
            }
            
            @Override
            public String getDescription() {
                return "Folders or Archive Files";
            }
        });
    }
    private void importButton() {
        Thread.ofPlatform().start(() -> {
            var result = import_fc.showOpenDialog(this
            );
            
            if (result == JFileChooser.APPROVE_OPTION) {
                var inputs = List.of(import_fc.getSelectedFiles());
                importWorlds(inputs);
            }
        });
    }
    
    private void importWorlds(Collection<File> files) {
        
        for (var file : files) {
            if (!file.exists()) {
                JOptionPane.showMessageDialog(this
                        , "File does not exist: " + file, "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        var worlds = files.stream().map(File::toPath).flatMap((path) -> {
            try {
                return Stream.of(new WorldFile(path, WorldData.parse(path)));
            } catch (ParsingException e) {
                JOptionPane.showMessageDialog(this
                        ,
                        path.getFileName() + " is not a Minecraft world: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                return Stream.empty();
            }
        }).toArray(WorldFile[]::new);
        
        var panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        {
            var label_panel = new JPanel();
            label_panel.add(new JLabel("""
                    <html>
                        <h2>Do you want to import these worlds?</h2>
                    </html>
                    """));
            
            panel.add(label_panel);
            
            panel.add(Box.createVerticalStrut(5));
        }
        
        for (var world : worlds) {
            var display_panel = new JPanel(new BorderLayout());
            display_panel.setBorder(new EmptyBorder(2, 2 ,2, 2));
            var world_entry = WorldListEntry.of(world);
            world_entry.setPreferredSize(new Dimension(500, 50));
            display_panel.add(world_entry, BorderLayout.CENTER);
            
            panel.add(display_panel);
        }
        
        var confirm = JOptionPane.showConfirmDialog(this
                , panel,
                "Import World", JOptionPane.YES_NO_OPTION);
        
        // deallocate the panel for good measure
        panel = null;
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        var outputs = new HashMap<WorldFile, Path>();
        for (var world : worlds) {
            var data = world.data();
            var input = world.path();
            Path output;
            if (Files.isDirectory(input)) {
                output = save_path.resolve(input.getFileName());
            } else {
                output = save_path.resolve(data.dirName());
            }
            if (Files.exists(output) || outputs.containsValue(output)) {
                panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                
                var label_panel = new JPanel();
                
                label_panel.add(new JLabel("""
                        <html>
                            <h2>World folder with the same name already exists, please pick another folder name</h2>
                        </html>
                        """));
                
                panel.add(label_panel);
                
                panel.add(Box.createVerticalStrut(5));
                
                var display_panel = new JPanel(new BorderLayout());
                
                display_panel.setBorder(new EmptyBorder(2, 2, 2, 2));
                var world_entry = WorldListEntry.of(world);
                world_entry.setPreferredSize(new Dimension(500, 50));
                display_panel.add(world_entry, BorderLayout.CENTER);
                
                panel.add(display_panel);
                
                while (Files.exists(output) || outputs.containsValue(output)) {
                    var new_name = JOptionPane.showInputDialog(
                            this,
                            panel,
                            "New Name", JOptionPane.QUESTION_MESSAGE);
                    if (new_name == null) {
                        return;
                    }
                    output = save_path.resolve(new_name);
                }
                
                panel = null;
            }
            outputs.put(world, output);
        }
        
        try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = Stream.of(worlds).map((world) -> CompletableFuture.runAsync(() -> {
                if (!(world instanceof WorldFile(var input, var data)))
                    throw new IllegalArgumentException("what");
                
                var name = data.worldName();
                var output = outputs.get(world);
                
                if (Files.isDirectory(input)) {
                    // we just copy the directory
                    
                    try {
                        PathUtils.copyDirectory(input, output);
                        
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(this
                                , "Failed to import world: " + name + " - " + e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    
                    var is_root = data.dirName().isEmpty();
                    try (var fs = createArchiveFileSystem(input)) {
                        var root = fs.getRootDirectories().iterator().next();
                        
                        var world_path = (is_root) ? root : getSubpathWithName(root, data.dirName()).orElseThrow();
                        
                        PathUtils.copyDirectory(world_path, output);
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(this
                                , "Failed to import world: " + name + " - " + e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        
                        throw new RuntimeException(e);
                    }
                }
            }, ex)).toList();
            
            
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).exceptionally((_) -> null).join();
            
            
            JOptionPane.showMessageDialog(null, futures.stream().filter(e -> !e.isCompletedExceptionally()).count() + " of " + futures.size() + " worlds imported successfully", "Import Worlds",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        
        reloadAllWorlds();
    }
    
    private void open(Path path) {
        Thread.ofVirtual().start(() -> {
            try {
                Desktop.getDesktop().open(path.toFile());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to open: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    
    private void expand(WorldFile loaded_file) {
        Thread.ofPlatform().start(() -> {
            var path = loaded_file.path();
            var name = path.getFileName().toString();
            
            var confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to expand: " + name, "Expand", JOptionPane.YES_NO_OPTION);
            
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            
            var keep = JOptionPane.showConfirmDialog(this, "Do you want to keep the archive after expanding?", "Keep Archive", JOptionPane.YES_NO_OPTION);
            
            Path output_path;
            while (true) {
                var output = JOptionPane.showInputDialog(this
                        , "Enter the output folder name", "Output Directory", JOptionPane.QUESTION_MESSAGE);
                if (output == null) {
                    return;
                }
                output_path = path.resolve(output);
                if (Files.exists(output_path)) {
                    JOptionPane.showMessageDialog(this
                            , "Already exists, please pick another name", "Output Exists", JOptionPane.ERROR_MESSAGE);
                } else {
                    break;
                }
            }
            
            var is_root = loaded_file.data().dirName().isEmpty();
            try (var fs = createArchiveFileSystem(path)) {
                var root = fs.getRootDirectories().iterator().next();
                
                var world_path = (is_root) ? root : getSubpathWithName(root, loaded_file.data().dirName()).orElseThrow();
                
                PathUtils.copyDirectory(world_path, output_path);
                JOptionPane.showMessageDialog(null, "Expansion completed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to expand: " + name + " - " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (keep == JOptionPane.NO_OPTION) {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Failed to delete archive: " + name + " - " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            
            reloadAllWorlds();
            
        });
    }
    
    @Override
    public void close() throws IOException {
        watch_service.close();
    }
}
