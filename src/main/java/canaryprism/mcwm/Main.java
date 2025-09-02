package canaryprism.mcwm;

import canaryprism.mcwm.instance.InstanceFinder;
import canaryprism.mcwm.instance.SaveDirectory;
import canaryprism.mcwm.swing.InstancePickerView;
import canaryprism.mcwm.swing.SaveView;
import canaryprism.mcwm.swing.file.LoadedFile;
import canaryprism.mcwm.swing.file.WorldFile;
import canaryprism.mcwm.swing.nbt.NBTView;
import canaryprism.mcwm.swing.savedir.SaveDirectoryView;
import canaryprism.mcwm.swing.savedir.SaveFinderView;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import dev.dirs.ProjectDirectories;
import org.apache.commons.io.file.PathUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The main class of the Minecraft World Manager
 */
public class Main {


    public static final String VERSION = "3.0.1";

    private static <T> T getArg(String[] args, String key, Function<String, T> parser, Supplier<T> default_value) {
        for (int i = args.length - 2; i >= 0; i--) {
            if (args[i].equals(key)) {
                return parser.apply(args[i + 1]);
            }
        }
        return default_value.get();
    }
    
    private static final ProjectDirectories DIRS = ProjectDirectories.from("", "canaryprism", "mcwm");
    

    public static void main(String[] args) throws IOException {
        
        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        
        final var icon = ImageIO.read(Objects.requireNonNull(Main.class.getResource("/mcwm/icon.png")));
        if (Taskbar.isTaskbarSupported())
            Taskbar.getTaskbar().setIconImage(icon);

        var working_directory = Path.of(DIRS.cacheDir);
        if (Files.notExists(working_directory)) {
            Files.createDirectories(working_directory);
        }

        {
            var laf = getArg(args, "--laf", (e) -> {
                if (e.equals("default")) {
                    return UIManager.getSystemLookAndFeelClassName();
                }
                return e;
            }, FlatMacDarkLaf.class::getName);
        
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
        
        var cache_file = working_directory.resolve("cache.zip");
        var cache_file_fs = FileSystems.newFileSystem(cache_file, Map.of("create", true));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                cache_file_fs.close();
            } catch (IOException e) {
                System.err.println("failed to write cache file");
                throw new RuntimeException(e);
            }
        }));
        var cache_path = cache_file_fs.getPath("/");
        
        try {
            new Main(cache_path).show();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Fatal Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static CompletableFuture<Path> pickLauncher(List<InstanceFinder> finders) {
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

        var launcher_list = new JList<InstanceFinder>();
        launcher_list.setListData(finders.toArray(InstanceFinder[]::new));

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

    public static Optional<Path> openDialog() {
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
    
    private final JFrame frame;
    private final JTabbedPane instances;

    /**
     * initialise the main class and the frame
     *
     */
    public Main(Path cache_path) {
        this.frame = new JFrame("Minecraft World Manager");

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        var title_panel = new JPanel();
        var title_label = new JLabel("""
                <html>
                    <h1>Minecraft World Manager</h1>
                </html>
                """);
        title_panel.add(title_label);

        frame.getContentPane().add(title_panel, BorderLayout.NORTH);
        
        this.instances = new JTabbedPane();
        instances.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        
        instances.addTab("+", null, new InstancePickerView(this, cache_path), "Create New Tab");
        
        frame.add(instances, BorderLayout.CENTER);

        var bottom_panel = new JPanel();
        bottom_panel.setLayout(new BoxLayout(bottom_panel, BoxLayout.Y_AXIS));

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


    private final JFileChooser export_fc = new JFileChooser();
    {
        export_fc.setDialogTitle("Export World");
        export_fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        export_fc.setFileFilter(new FileFilter() {
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

    public void export(WorldFile loaded_file) {
        Thread.ofPlatform().start(() -> {
            var world_path = loaded_file.path();

            export_fc.setSelectedFile(new File(last_export_directory + "/" + world_path.getFileName() + ".zip"));

            var result = export_fc.showSaveDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
                var output = export_fc.getSelectedFile();
                last_export_directory = output.getParent();
                if (!output.getName().endsWith(".zip")) {
                    output = new File(output.getParentFile(), output.getName() + ".zip");
                }

                var output_path = output.toPath();

                try (var fs = FileSystems.newFileSystem(output_path, Map.of("create", true))) {
                    var root = fs.getPath("/");
                    PathUtils.copyDirectory(world_path, root.resolve(world_path.getFileName().toString()));
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Failed to export: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                JOptionPane.showMessageDialog(null, "Export completed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    public void delete(LoadedFile loaded_file) {
        Thread.ofPlatform().start(() -> {
            var path = loaded_file.path();
            var name = path.getFileName().toString();
            if (loaded_file instanceof WorldFile world) {
                name = world.data().worldName();
            }
            var confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete: " + name, "Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    PathUtils.delete(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Failed to delete: " + name + " - " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        });
    }

    private static final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    public void copy(LoadedFile file) {
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

    public void add(SaveDirectory directory) {
        var index = instances.getTabCount() - 1;
        final var icon_height = 20;
        
        var icon = directory.icon()
                .map((e) -> e.getScaledInstance(
                        (int)(((double) e.getWidth(null)) / e.getHeight(null) * icon_height),
                        icon_height,
                        Image.SCALE_SMOOTH));
        var save_view = new SaveView(this, directory.path());
        instances.insertTab(
                directory.name(),
                icon.map(ImageIcon::new).orElse(null),
                save_view,
                directory.path().toString(),
                index);
        class TabRenderer extends JComponent {
            TabRenderer() {
                this.setLayout(new FlowLayout());
                
                JLabel label = new JLabel(directory.name());
                label.setIcon(icon.map(ImageIcon::new).orElse(null));
                this.add(label);
                
                var close_button = new JButton("╳");
                close_button.setToolTipText("Close Tab");
                close_button.setContentAreaFilled(false);
                close_button.addMouseListener(new MouseAdapter() {
                    private Color last_foreground;
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        last_foreground = close_button.getForeground();
                        close_button.setForeground(Color.RED);
                    }
                    
                    @Override
                    public void mouseExited(MouseEvent e) {
                        close_button.setForeground(last_foreground);
                    }
                });
                close_button.addActionListener((_) -> {
                    var index = instances.indexOfTabComponent(this);
                    if (instances.getComponentAt(index) instanceof AutoCloseable closeable) {
                        try {
                            closeable.close();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    instances.removeTabAt(index);
                });
                
                this.add(close_button);
            }
        }
        instances.setTabComponentAt(index, new TabRenderer());
        instances.setSelectedIndex(index);
    }
    
    public static Optional<Path> getSubpathWithName(Path path, String name) {
        try (var stream = Files.walk(path)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter((e) -> e.getFileName().toString().equals(name))
                    .findAny();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static FileSystem createArchiveFileSystem(Path path) throws URISyntaxException, IOException {
        return FileSystems.newFileSystem(
                new URI("vfs:file", "", URLEncoder.encode(path.toString(), StandardCharsets.UTF_8), null),
                Map.of());
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
}
