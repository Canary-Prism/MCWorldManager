package canaryprism.mcwm;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.json.JSONException;
import org.json.JSONObject;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;

import canaryprism.mcwm.savedir.SaveDirectory;
import canaryprism.mcwm.savedir.SaveFinder;
import canaryprism.mcwm.savedir.launcher.Curseforge;
import canaryprism.mcwm.savedir.launcher.Modrinth;
import canaryprism.mcwm.savedir.launcher.MultiMC;
import canaryprism.mcwm.savedir.launcher.Prism;
import canaryprism.mcwm.savedir.launcher.Vanilla;
import canaryprism.mcwm.saves.ParsingException;
import canaryprism.mcwm.saves.WorldData;
import canaryprism.mcwm.swing.WorldListEntry;
import canaryprism.mcwm.swing.file.LoadedFile;
import canaryprism.mcwm.swing.file.UnknownFile;
import canaryprism.mcwm.swing.file.WorldFile;
import canaryprism.mcwm.swing.nbt.NBTView;
import canaryprism.mcwm.swing.savedir.SaveDirectoryView;
import canaryprism.mcwm.swing.savedir.SaveFinderView;
import dev.dirs.ProjectDirectories;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * The main class of the Minecraft World Manager
 */
public class Main {


    public static final String VERSION = "2.5.0";

    private static <T> T getArg(String[] args, String key, Function<String, T> parser, Supplier<T> default_value) {
        for (int i = args.length - 2; i >= 0; i--) {
            if (args[i].equals(key)) {
                return parser.apply(args[i + 1]);
            }
        }
        return default_value.get();
    }
    
    private static final ProjectDirectories dirs = ProjectDirectories.from("", "canaryprism", "mcwm");
    

    public static void main(String[] args) throws IOException {
        
        final var icon = ImageIO.read(Objects.requireNonNull(Main.class.getResource("/mcwm/icon.png")));
        if (Taskbar.isTaskbarSupported())
            Taskbar.getTaskbar().setIconImage(icon);

        final var save_finders = List.of(
            new Vanilla(),
            new Prism(),
            new MultiMC(),
            new Modrinth(),
            new Curseforge()
        );

        var working_directory = Path.of(dirs.cacheDir);
        if (Files.notExists(working_directory)) {
            Files.createDirectories(working_directory);
        }

        {
            var laf = getArg(args, "--laf", (e) -> {
                if (e.equals("default")) {
                    return UIManager.getSystemLookAndFeelClassName();
                }
                return e;
            }, () -> FlatMacDarkLaf.class.getName());
        
            try {
                UIManager.setLookAndFeel(laf);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // this is for nerds who just want to see the NBT data
        try {
            var nbt = getArg(args, "--nbt-view", 
                ((Function<String, Path>)Path::of).andThen(Path::toFile), // i am very funny
                () -> { throw new NoSuchElementException(); }
            );

            new NBTView(nbt, null);

            return;
        } catch (NoSuchElementException e) { // ignore
        }

        var cache_file = working_directory.resolve("cache.json");

        JSONObject save = null;
        if (Files.exists(cache_file)) {
            try {
                save = new JSONObject(Files.readString(cache_file));
                for (var finder : save_finders) {
                    try {
                        var path = save.getString(finder.getClass().getName());
                        finder.loadCache(Path.of(path));
                    } catch (Exception e) {
                        System.err.print("Failed to load cache for " + finder.getClass().getName() + ": ");
                        e.printStackTrace();
                    }
                }
            } catch (JSONException | IOException e) {
                System.err.print("Failed to load cache: ");
                e.printStackTrace();
            }
        }
        

        // here, we assign the name of the OS, according to Java, to a variable...
        // only try to find the saves if loading from cache failed
        String OS = (System.getProperty("os.name")).toUpperCase();
        if (OS.contains("WIN")) {
            save_finders.stream().filter(e -> e.getSavesPaths().isEmpty()).forEach(SaveFinder::findWindows);
        } else if (OS.contains("MAC")) {
            save_finders.stream().filter(e -> e.getSavesPaths().isEmpty()).forEach(SaveFinder::findMac);
        } else { // we'll just assume Linux or something else
            save_finders.stream().filter(e -> e.getSavesPaths().isEmpty()).forEach(SaveFinder::findLinux);
        }

        { // save the cache
            var new_save = Optional.ofNullable(save).orElseGet(JSONObject::new);

            for (var finder : save_finders) {
                finder.toCache().map(Path::toString).ifPresent((e) -> {
                    new_save.put(finder.getClass().getName(), e);
                });
            }

            Files.writeString(cache_file, new_save.toString());
        }

        var has_save = save_finders.stream().filter(e -> !e.getSavesPaths().isEmpty()).toList();

        Path saves_path;
        if (has_save.size() == 0) {
            saves_path = openDialog().orElseThrow();
        } else {
            saves_path = pickLauncher(has_save)
                .exceptionally((_) -> {
                    System.exit(-1); // i guess
                    return null;
                })
                .join();
        }


        try {
            new Main(saves_path).show();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Fatal Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static CompletableFuture<Path> pickLauncher(List<SaveFinder> finders) {
        var dialog = new JDialog((JDialog) null, "Select Minecraft Launcher");

        var future = new CompletableFuture<Path>();

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialog.dispose();
                future.cancel(true);
            }
        });

        var launcher_list = new JList<SaveFinder>();
        launcher_list.setListData(finders.toArray(SaveFinder[]::new));

        launcher_list.setCellRenderer((list, value, index, selected, cellHasFocus) -> {
            var entry = new SaveFinderView(value);
            entry.setPreferredSize(new Dimension(400, 40));
            var panel = new JPanel(new BorderLayout());
            panel.add(entry, BorderLayout.CENTER);

            panel.setBorder(new LineBorder(Color.gray, 2));

            return panel;
        });

        launcher_list.setFocusable(false);
        launcher_list.setSelectedValue(null, false);

        launcher_list.addListSelectionListener((e) -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            var selected = launcher_list.getSelectedValue();
            if (selected != null) {
                var instances = selected.getSavesPaths();
                if (instances.size() == 1) {
                    dialog.dispose();
                    future.complete(instances.get(0).path());
                } else {
                    pickInstance(instances, dialog)
                    .thenAccept((path) -> {
                        dialog.dispose();
                        future.complete(path);
                    })
                    .exceptionally((_) -> null);
                }
            }
        });

        var scroll_pane = new JScrollPane(launcher_list);

        {
            var pref_size = launcher_list.getPreferredSize();
            pref_size.height = Math.min(pref_size.height, 500);
            scroll_pane.getViewport().setPreferredSize(pref_size);
        }

        dialog.add(scroll_pane);


        var custom_button = new JButton("Custom Folder");
        custom_button.addActionListener((e) -> {
            openDialog().ifPresent((path) -> {
                dialog.dispose();
                future.complete(path);
            });
        });

        dialog.add(custom_button, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setResizable(false);
        dialog.setVisible(true);

        return future;
    }

    private static CompletableFuture<Path> pickInstance(List<SaveDirectory> instances, JDialog owner) {
        var dialog = new JDialog(owner, "Select Instance");

        var future = new CompletableFuture<Path>();

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialog.dispose();
                future.cancel(true);
            }
        });

        var instance_list = new JList<SaveDirectory>();
        instance_list.setListData(instances.toArray(SaveDirectory[]::new));

        instance_list.setCellRenderer((list, value, index, selected, cellHasFocus) -> {
            var entry = new SaveDirectoryView(value);
            entry.setPreferredSize(new Dimension(300, 30));
            var panel = new JPanel(new BorderLayout());
            panel.add(entry, BorderLayout.CENTER);

            panel.setBorder(new LineBorder(Color.gray, 2));

            return panel;
        });

        instance_list.setFocusable(false);
        instance_list.setSelectedValue(null, false);

        instance_list.addListSelectionListener((e) -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            var selected = instance_list.getSelectedValue();
            if (selected != null) {
                dialog.dispose();
                future.complete(selected.path());
            }
        });

        var scroll_pane = new JScrollPane(instance_list);

        {
            var pref_size = instance_list.getPreferredSize();
            pref_size.height = Math.min(pref_size.height, 500);
            scroll_pane.getViewport().setPreferredSize(pref_size);
        }

        dialog.add(scroll_pane);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setVisible(true);

        return future;
    }

    private static Optional<Path> openDialog() {
        var fc = new JFileChooser();
        fc.setDialogTitle("Select Minecraft Saves Folder");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setFileHidingEnabled(false);

        File saves = null;

        while (saves == null || !saves.exists()) {
            var input = fc.showOpenDialog(null);
            if (input != JFileChooser.APPROVE_OPTION) {
                return Optional.empty();
            }
            saves = fc.getSelectedFile();
            if (saves.exists() && saves.isDirectory()) {
                if (saves.getName().contains("minecraft") && new File(saves, "saves").exists()) {
                    saves = new File(saves, "saves");
                }
                break;
            }
        }
        return Optional.of(saves.toPath());
    }

    /**
     * just a simple method to turn a throwable into a string with stacktrace and such
     * @param throwable the throwable to convert
     * @return the stack trace string
     */
    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    private final Path folder;
    private final List<LoadedFile> worlds = new ArrayList<>();
    private final JFrame frame;


    /**
     * initialise the main class and the frame
     * 
     * @param folder the Path to load the worlds from, must be a directory that exists
     */
    public Main(Path folder) {
        if (!Files.exists(folder)) {
            throw new IllegalArgumentException("Minecraft saves folder does not exist: " + folder);
        }
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("Not a folder: " + folder);
        }
        this.folder = folder;
        this.frame = new JFrame("Minecraft World Manager");

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.list = new JList<LoadedFile>();

        reloadAllWorlds();
        var auto_refresh_checkbox = new JCheckBox("Auto Refresh");
        auto_refresh_checkbox.setSelected(true);
        Thread.ofVirtual().start(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
    
                folder.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
    
    
                WatchKey key;
                while ((key = watchService.take()) != null) {
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
        

        var title_panel = new JPanel();
        var title_label = new JLabel("""
                <html>
                    <h1>Minecraft World Manager</h1>
                </html>
                """);
        title_panel.add(title_label);

        frame.getContentPane().add(title_panel, BorderLayout.NORTH);


        var world_list_pane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);


        
        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        


        final var selected_color = Optional.ofNullable(UIManager.getColor("List.selectionBorderColor")).orElse(new Color(0xabcdef));
        // final var selected_color = new Color(0xabcdef);

        list.setCellRenderer((list, value, index, selected, cellHasFocus) -> {
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
                    List<File> dropped_files = (List<File>) evt.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    Thread.ofPlatform().start(() -> {
                        var list = dropped_files.stream().parallel().filter((e) -> {
                            if (e.getName().endsWith(".dat") || e.getName().endsWith(".dat_old")) {
                                try {
                                    new NBTView(e, Main.this);
                                } catch (IOException e1) {
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

        frame.getContentPane().add(world_list_pane, BorderLayout.CENTER);

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
            open_button.addActionListener((e) -> {
                open(folder);
            });
            open_button.setToolTipText("Open the Minecraft saves folder");
        } else {
            open_button.setEnabled(false);
            open_button.setToolTipText("Open the Minecraft saves folder (not supported on this computer)");
        }


        var refresh_button = new JButton("Refresh");
        refresh_button.addActionListener((e) -> {
            reloadAllWorlds();
        });
        refresh_button.setToolTipText("Refresh the list of worlds");

        auto_refresh_checkbox.setToolTipText("Automatically refresh the list of worlds when a change is detected in the folder");

        action_panel.add(import_button);
        action_panel.add(open_button);
        action_panel.add(refresh_button);
        action_panel.add(auto_refresh_checkbox);

        bottom_panel.add(action_panel);

        var about_panel = new JPanel(new BorderLayout());
        about_panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        var version_label = new JLabel("Version: " + VERSION);

        about_panel.add(version_label, BorderLayout.WEST);

        var about_button = new JButton("About");
        about_button.addActionListener((e) -> {
            Thread.ofVirtual().start(() -> {
                JOptionPane.showMessageDialog(frame, """
                        <html>
                            <h1>Minecraft World Manager</h1>
                            <p>made for the non nerds</p>
                            by Mia
                            <p>github.com/Canary-Prism/MCWorldManager</p>
                        </html>
                        """, "About", JOptionPane.INFORMATION_MESSAGE);
            });
        });

        about_panel.add(about_button, BorderLayout.EAST);

        bottom_panel.add(about_panel);

        frame.getContentPane().add(bottom_panel, BorderLayout.SOUTH);

    }

    // private final JPanel world_options_panel, archive_options_panel, unknown_options_panel;

    private void initialiseOptions() {
        //#region init World Options
        {
            var export_button = new JButton("Share");
            export_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                if (selected instanceof WorldFile world) {
                    export(world);
                }
            });
            export_button.setToolTipText("Export the world to a zip file to share it with others");

            var copy_button = new JButton("Copy");
            copy_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                copy(selected);
            });
            copy_button.setToolTipText("Copy the world folder to clipboard");

            var delete_button = new JButton("Delete");
            delete_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                delete(selected);
            });
            delete_button.setToolTipText("Delete the world forever");

            var edit_nbt_button = new JButton("Edit NBT Data");
            edit_nbt_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                if (selected instanceof WorldFile world) {
                    Thread.ofVirtual().start(() -> {
                        try {
                            new NBTView(world.path().resolve("level.dat").toFile(), this);
                        } catch (IOException e1) {
                            JOptionPane.showMessageDialog(frame, "Failed to open NBT Editor: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

            option_buttons_panel.add(panel, SelectedType.world.name());
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
                copy(selected);
            });
            copy_button.setToolTipText("Copy the archive to clipboard");

            var delete_button = new JButton("Delete");
            delete_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                delete(selected);
            });
            delete_button.setToolTipText("Delete the world forever");

            var panel = new JPanel();

            panel.add(expand_button);
            panel.add(copy_button);
            panel.add(delete_button);

            option_buttons_panel.add(panel, SelectedType.archive.name());
        }
        //#endregion
        //#region init Unknown Options
        {
            var delete_button = new JButton("Delete");
            delete_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                delete(selected);
            });
            delete_button.setToolTipText("Delete the file forever");

            var error_button = new JButton("Error Details");
            error_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                if (selected instanceof UnknownFile unknown) {
                    Thread.ofVirtual().start(() -> {
                        var message = getStackTraceAsString(unknown.exception());
                        JOptionPane.showMessageDialog(frame, message, "Error Details", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
            error_button.setToolTipText("View the error details");

            var copy_button = new JButton("Copy");
            copy_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                copy(selected);
            });
            copy_button.setToolTipText("Copy the file to clipboard");


            var panel = new JPanel();

            panel.add(delete_button);
            panel.add(error_button);
            panel.add(copy_button);

            option_buttons_panel.add(panel, SelectedType.unknown.name());
        }
        //#endregion
        //#region init Unknown with Level Options
        {
            var delete_button = new JButton("Delete");
            delete_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                delete(selected);
            });
            delete_button.setToolTipText("Delete the file forever");

            var error_button = new JButton("Error Details");
            error_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                if (selected instanceof UnknownFile unknown) {
                    Thread.ofVirtual().start(() -> {
                        var message = getStackTraceAsString(unknown.exception());
                        JOptionPane.showMessageDialog(frame, message, "Error Details", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
            error_button.setToolTipText("View the error details");

            var copy_button = new JButton("Copy");
            copy_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                copy(selected);
            });
            copy_button.setToolTipText("Copy the file to clipboard");

            var edit_nbt_button = new JButton("Edit NBT Data");
            edit_nbt_button.addActionListener((e) -> {
                var selected = list.getSelectedValue();
                if (selected instanceof UnknownFile file) {
                    Thread.ofVirtual().start(() -> {
                        try {
                            new NBTView(file.path().resolve("level.dat").toFile(), this);
                        } catch (IOException e1) {
                            JOptionPane.showMessageDialog(frame, "Failed to open NBT Editor: " + e1.getMessage(),
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

            option_buttons_panel.add(panel, SelectedType.unknown_with_level.name());
        }

        //#region init None Options
        {
            var panel = new JPanel();
            panel.add(new JLabel("<Select an item to see options>"));
            option_buttons_panel.add(panel, SelectedType.none.name());
        }
    }

    private final JList<LoadedFile> list;
    private final JPanel option_buttons_panel;
    private final CardLayout card;

    enum SelectedType {
        none, world, archive, unknown, unknown_with_level
    }

    private void updateOptionsPanel() {
        switch (list.getSelectedValue()) {
            case WorldFile e -> {
                if (Files.isDirectory(e.path())) {
                    card.show(option_buttons_panel, SelectedType.world.name());
                } else {
                    card.show(option_buttons_panel, SelectedType.archive.name());
                }
            }
            case UnknownFile e -> {
                if (Files.isDirectory(e.path()) && Files.exists(e.path().resolve("level.dat"))) {
                    card.show(option_buttons_panel, SelectedType.unknown_with_level.name());
                } else {
                    card.show(option_buttons_panel, SelectedType.unknown.name());
                }
            }
            case null -> {
                card.show(option_buttons_panel, SelectedType.none.name());
            }
        }
    }



    public void reloadAllWorlds() {
        try {

            synchronized (worlds) {
                worlds.clear();
    
                var ex = Executors.newVirtualThreadPerTaskExecutor();
    
                List<CompletableFuture<Void>> futures; 
                try (var stream = Files.list(folder)) {
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
    
                list.setListData(worlds.stream().sorted((a, b) -> {
                    if (a instanceof WorldFile world_a && b instanceof WorldFile world_b) {
                        return -world_a.data().lastPlayed().compareTo(world_b.data().lastPlayed());
                    } else if (a instanceof WorldFile) {
                        return -1;
                    } else if (b instanceof WorldFile) {
                        return 1;
                    } else {
                        try {
                            return (int)(Files.getLastModifiedTime(b.path()).compareTo(Files.getLastModifiedTime(a.path())));
                        } catch (IOException e) {
                            return 0; // eh, whatever
                        }
                    }
                }).toArray(LoadedFile[]::new));
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Failed to load worlds: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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
            var result = import_fc.showOpenDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
                var inputs = List.of(import_fc.getSelectedFiles());
                importWorlds(inputs);
            }
        });
    }

    private void importWorlds(List<File> files) {

        for (var file : files) {
            if (!file.exists()) {
                JOptionPane.showMessageDialog(frame, "File does not exist: " + file, "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        var worlds = files.stream().map(File::toPath).flatMap((path) -> {
            try {
                return Stream.of(new WorldFile(path, WorldData.parse(path)));
            } catch (ParsingException e) {
                JOptionPane.showMessageDialog(frame,
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

        var confirm = JOptionPane.showConfirmDialog(frame, panel,
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
                output = folder.resolve(input.getFileName());
            } else {
                output = folder.resolve(data.dirName());
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
                    var new_name = JOptionPane.showInputDialog(frame,
                            panel,
                            "New Name", JOptionPane.QUESTION_MESSAGE);
                    if (new_name == null) {
                        return;
                    }
                    output = folder.resolve(new_name);
                }

                panel = null;
            }
            outputs.put(world, output);
        }

        var ve = Executors.newVirtualThreadPerTaskExecutor();

        var futures = Stream.of(worlds).map((world) -> CompletableFuture.runAsync(() -> {
            if (!(world instanceof WorldFile(var input, var data))) 
                throw new IllegalArgumentException("what");

            var name = data.worldName();
            var output = outputs.get(world);

            if (Files.isDirectory(input)) {
                // we just copy the directory

                try {
                    Files.copy(input, output);
                    var source = input;
                    var target = output;
                    Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                                throws IOException {
                            Path targetDir = target.resolve(source.relativize(dir));
                            if (!Files.exists(targetDir)) {
                                Files.createDirectories(targetDir);
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.copy(file, target.resolve(source.relativize(file)),
                                    StandardCopyOption.REPLACE_EXISTING);
                            return FileVisitResult.CONTINUE;
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Failed to import world: " + name + " - " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {

                var root = data.dirName().isEmpty();

                try (
                        var fis = new BufferedInputStream(Files.newInputStream(input));
                        var ais = createArchiveInputStream(fis)) {
                    Files.createDirectories(output);

                    ArchiveEntry entry;
                    while ((entry = ais.getNextEntry()) != null) {
                        var entry_name = entry.getName();

                        if (!root) {
                            entry_name = entry_name.substring(data.dirName().length() + 1);
                        }

                        Path entry_path = output.resolve(entry_name);

                        if (entry.isDirectory()) {
                            Files.createDirectories(entry_path);
                        } else {
                            Files.createDirectories(entry_path.getParent());
                            try (OutputStream os = Files.newOutputStream(entry_path)) {
                                ais.transferTo(os);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Failed to import world: " + name + " - " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    
                    throw new RuntimeException(e);
                }
            }
        }, ve)).toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).exceptionally((_) -> null).join();


        JOptionPane.showMessageDialog(null, futures.stream().filter(e -> !e.isCompletedExceptionally()).count() + " of " + futures.size() + " worlds imported successfully", "Import Worlds",
                JOptionPane.INFORMATION_MESSAGE);

        reloadAllWorlds();
    }

    private final JFileChooser export_fc = new JFileChooser();
    {
        export_fc.setDialogTitle("Export World");
        export_fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        export_fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".zip");
            }

            @Override
            public String getDescription() {
                return "Zip Files";
            }
        });
    }

    private volatile String last_export_directory = System.getProperty("user.home");

    private void export(WorldFile loaded_file) {
        Thread.ofPlatform().start(() -> {
            var file = loaded_file.path().toFile();

            export_fc.setSelectedFile(new File(last_export_directory + "/" + file.getName() + ".zip"));

            var result = export_fc.showSaveDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
                var output = export_fc.getSelectedFile();
                last_export_directory = output.getParent();
                if (!output.getName().endsWith(".zip")) {
                    output = new File(output.getParentFile(), output.getName() + ".zip");
                }

                try (
                    var fos = new FileOutputStream(output);
                    var zos = new ZipOutputStream(fos)
                ) {
                    if (Files.isDirectory(file.toPath())) {
                        zipDirectory(file, file.getName(), zos);
                    } else {
                        zos.putNextEntry(new ZipEntry(file.getName()));
                        try (var fis = new FileInputStream(file)) {
                            fis.transferTo(zos);
                        }
                    }
                    zos.closeEntry();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Failed to export: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                JOptionPane.showMessageDialog(null, "Export completed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private void zipDirectory(File folder, String parent_folder, ZipOutputStream zos) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                zipDirectory(file, parent_folder + "/" + file.getName(), zos);
            } else {
                try (var fis = new FileInputStream(file)) {
                    var zipEntry = new ZipEntry(parent_folder + "/" + file.getName());
                    zos.putNextEntry(zipEntry);
                    fis.transferTo(zos);
                    zos.closeEntry();
                }
            }
        }
    }

    private void expand(WorldFile loaded_file) {
        Thread.ofPlatform().start(() -> {
            var path = loaded_file.path();
            var name = path.getFileName().toString();

            var confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to expand: " + name, "Expand", JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            var keep = JOptionPane.showConfirmDialog(frame, "Do you want to keep the archive after expanding?", "Keep Archive", JOptionPane.YES_NO_OPTION);

            Path output_path;
            while (true) {
                var output = JOptionPane.showInputDialog(frame, "Enter the output folder name", "Output Directory", JOptionPane.QUESTION_MESSAGE);
                if (output == null) {
                    return;
                }
                output_path = folder.resolve(output);
                if (Files.exists(output_path)) {
                    JOptionPane.showConfirmDialog(frame, "Already exists, please pick another name", "Output Exists", JOptionPane.ERROR_MESSAGE);
                } else {
                    break;
                }
            }

            try (
                var fis = new BufferedInputStream(Files.newInputStream(path));
                var ais = createArchiveInputStream(fis)
            ) {
                Files.createDirectories(output_path);

                ArchiveEntry entry;
                while ((entry = ais.getNextEntry()) != null) {
                    Path entryPath = output_path.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        try (var os = Files.newOutputStream(entryPath)) {
                            ais.transferTo(os);
                        }
                    }
                }

                JOptionPane.showMessageDialog(null, "Expansion completed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to expand: " + name + " - " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (keep == JOptionPane.NO_OPTION) {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Failed to delete archive: " + name + " - " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            reloadAllWorlds();
        
        });
    }

    private void delete(LoadedFile loaded_file) {
        Thread.ofPlatform().start(() -> {
            var path = loaded_file.path();
            var name = path.getFileName().toString();
            if (loaded_file instanceof WorldFile world) {
                name = world.data().worldName();
            }
            var confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete: " + name, "Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    if (Files.isDirectory(path)) {
                        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }
    
                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } else {
                        Files.delete(path);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Failed to delete: " + name + " - " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                reloadAllWorlds();
            }
        });
    }

    private static final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    private void copy(LoadedFile file) {
        Thread.ofVirtual().start(() -> {
            try {
                clipboard.setContents(new Transferable() {
                    final File f = file.path().toFile();
                    @Override
                    public DataFlavor[] getTransferDataFlavors() {
                        return new DataFlavor[] { DataFlavor.javaFileListFlavor };
                    }
        
                    @Override
                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return flavor.equals(DataFlavor.javaFileListFlavor);
                    }
        
                    @Override
                    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                        if (!isDataFlavorSupported(flavor)) {
                            throw new UnsupportedFlavorException(flavor);
                        }
                        return List.of(f);
                    }
                }, new ClipboardOwner() {
                    @Override
                    public void lostOwnership(Clipboard clipboard, Transferable contents) {
                        System.err.println("Lost ownership of clipboard");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to copy to clipboard: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void open(Path path) {
        Thread.ofVirtual().start(() -> {
            try {
                Desktop.getDesktop().open(path.toFile());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to open: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public void show() {
        frame.pack();
        frame.setVisible(true);
    }

    public void hide() {
        frame.setVisible(false);
    }

    public void close() {
        frame.dispose();
    }


    public static ArchiveInputStream<? extends ArchiveEntry> createArchiveInputStream(InputStream is) throws FileNotFoundException, ArchiveException {
        var type = ArchiveStreamFactory.detect(is);

        if (ArchiveStreamFactory.ZIP.equals(type)) { // zip files are special for some reason... unless other ones are too and I just don't know
            return new ZipArchiveInputStream(is, StandardCharsets.UTF_8.name(), true, true);
        }
        return new ArchiveStreamFactory().createArchiveInputStream(type, is);
    }
}
