package canaryprism.mcwm;

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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import com.formdev.flatlaf.themes.FlatMacDarkLaf;

import canaryprism.mcwm.saves.ParsingException;
import canaryprism.mcwm.saves.WorldData;
import canaryprism.mcwm.swing.WorldListEntry;
import canaryprism.mcwm.swing.file.LoadedFile;
import canaryprism.mcwm.swing.file.UnknownFile;
import canaryprism.mcwm.swing.file.WorldFile;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.BufferedInputStream;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Hello world!
 *
 */
public class Main {


    public static final String VERSION = "1.1.0";

    public static void main(String[] args) throws IOException {
        var saves_directory = "";
        // here, we assign the name of the OS, according to Java, to a variable...
        String OS = (System.getProperty("os.name")).toUpperCase();
        if (OS.contains("WIN")) {
            saves_directory = System.getenv("AppData") + "/.minecraft";
        } else if (OS.contains("MAC")) {
            saves_directory = System.getProperty("user.home") + "/Library/Application Support/minecraft";
        } else { // we'll just assume Linux or something else
            saves_directory = System.getProperty("user.home") + "/.minecraft";
        }
        saves_directory += "/saves";

        FlatMacDarkLaf.setup();
        
        try {
            new Main(new File(saves_directory)).show();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Fatal Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    private final File folder;
    private final List<LoadedFile> worlds = new ArrayList<>();
    private final JFrame frame;

    public Main(File folder) {
        if (!folder.exists()) {
            throw new IllegalArgumentException("Minecraft saves folder does not exist: " + folder);
        }
        if (!folder.isDirectory()) {
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
    
                Path path = folder.toPath();
                path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
    
    
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


        var world_list_panel = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);


        
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
            if (value instanceof WorldFile world && !world.file().isDirectory()) {
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

        world_list_panel.setViewportView(list);
        world_list_panel.setMinimumSize(new Dimension(500, 80));

        frame.getContentPane().add(world_list_panel, BorderLayout.CENTER);

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
            importWorld();
        });
        import_button.setToolTipText("Import a Minecraft world");

        var refresh_button = new JButton("Refresh");
        refresh_button.addActionListener((e) -> {
            reloadAllWorlds();
        });
        refresh_button.setToolTipText("Refresh the list of worlds");

        auto_refresh_checkbox.setToolTipText("Automatically refresh the list of worlds when a change is detected in the folder");

        action_panel.add(import_button);
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

            var panel = new JPanel();

            panel.add(export_button);
            panel.add(copy_button);
            panel.add(delete_button);

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
        none, world, archive, unknown
    }

    private void updateOptionsPanel() {
        switch (list.getSelectedValue()) {
            case WorldFile e -> {
                if (e.file().isDirectory()) {
                    card.show(option_buttons_panel, SelectedType.world.name());
                } else {
                    card.show(option_buttons_panel, SelectedType.archive.name());
                }
            }
            case UnknownFile e -> {
                card.show(option_buttons_panel, SelectedType.unknown.name());
            }
            case null -> {
                card.show(option_buttons_panel, SelectedType.none.name());
            }
        }
    }



    private void reloadAllWorlds() {
        worlds.clear();
        for (var file : folder.listFiles()) {
            try {
                var data = WorldData.parse(file);
                worlds.add(new WorldFile(file, data));
            } catch (ParsingException e) {
                worlds.add(new UnknownFile(file, e));
            }
        }
        list.setListData(worlds.toArray(new LoadedFile[0]));
    }

    private final JFileChooser import_fc = new JFileChooser();
    {
        import_fc.setDialogTitle("Import World");
        import_fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
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

    private void importWorld() {
        Thread.ofPlatform().start(() -> {

            var result = import_fc.showOpenDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
                var input = import_fc.getSelectedFile();
                if (!input.exists()) {
                    JOptionPane.showMessageDialog(frame, "File does not exist: " + input, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    var data = WorldData.parse(input);

                    var panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                    panel.setBorder(new EmptyBorder(10, 10, 10, 10));

                    var label_panel = new JPanel();
                    label_panel.add(new JLabel("""
                        <html>
                            <h2>Do you want to import this world?</h2>
                        </html>
                        """));

                    panel.add(label_panel);

                    panel.add(Box.createVerticalStrut(5));

                    var display_panel = new JPanel(new BorderLayout());
                    var world_entry = WorldListEntry.of(new WorldFile(input, data));
                    world_entry.setPreferredSize(new Dimension(500, 50));
                    display_panel.add(world_entry, BorderLayout.CENTER);

                    panel.add(display_panel);

                    var confirm = JOptionPane.showConfirmDialog(frame, panel,
                            "Import World", JOptionPane.YES_NO_OPTION);
                    
                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }
                    
                    var name = data.worldName();
                    if (input.isDirectory()) {
                        // we just copy the directory
                        var output = new File(folder, input.getName());

                        while (output.exists()) {
                            var new_name = JOptionPane.showInputDialog(frame, "World folder with the same name already exists, please pick another folder name", "New Name", JOptionPane.QUESTION_MESSAGE);
                            if (new_name == null) {
                                return;
                            }
                            output = new File(folder, new_name);
                        }
                    
                        try {
                            Files.copy(input.toPath(), output.toPath());
                            JOptionPane.showMessageDialog(null, "Import completed successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                        } catch (IOException e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(frame, "Failed to import world: " + name + " - " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } else {
                        var output = new File(folder, data.dirName());
    
                        while (output.exists()) {
                            var new_name = JOptionPane.showInputDialog(frame,
                                    "World folder with the same name already exists, please pick another folder name",
                                    "New Name", JOptionPane.QUESTION_MESSAGE);
                            if (new_name == null) {
                                return;
                            }
                            output = new File(folder, new_name);
                        }

                        var root = data.dirName().isEmpty();
    
                        try (
                                var fis = new BufferedInputStream(new FileInputStream(input));
                                var ais = createArchiveInputStream(fis)) {
                            Files.createDirectories(output.toPath());
    
                            ArchiveEntry entry;
                            while ((entry = ais.getNextEntry()) != null) {
                                var entry_name = entry.getName();

                                if (!root) {
                                    entry_name = entry_name.substring(data.dirName().length() + 1);
                                }
                                
                                Path entry_path = output.toPath().resolve(entry_name);

                                if (entry.isDirectory()) {
                                    Files.createDirectories(entry_path);
                                } else {
                                    Files.createDirectories(entry_path.getParent());
                                    try (OutputStream os = Files.newOutputStream(entry_path)) {
                                        ais.transferTo(os);
                                    }
                                }
                            }
    
                            JOptionPane.showMessageDialog(null, "Import completed successfully", "Success",
                                    JOptionPane.INFORMATION_MESSAGE);
    

                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(frame, "Failed to import world: " + name + " - " + e.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    reloadAllWorlds();
                } catch (ParsingException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, input.getName() + " is not a Minecraft world: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        });
    }

    private final JFileChooser export_fc = new JFileChooser();
    {
        export_fc.setDialogTitle("Export World");
        export_fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

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

    private void export(WorldFile loaded_file) {
        Thread.ofPlatform().start(() -> {
            var file = loaded_file.file();

            var result = export_fc.showSaveDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
                var output = export_fc.getSelectedFile();
                if (!output.getName().endsWith(".zip")) {
                    output = new File(output.getParentFile(), output.getName() + ".zip");
                }

                try (
                    var fos = new FileOutputStream(output);
                    var zos = new ZipOutputStream(fos)
                ) {
                    if (file.isDirectory()) {
                        zipDirectory(file, "", zos);
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

    private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                zipDirectory(file, parentFolder + "/" + file.getName(), zos);
            } else {
                try (var fis = new FileInputStream(file)) {
                    var zipEntry = new ZipEntry(parentFolder + "/" + file.getName());
                    zos.putNextEntry(zipEntry);
                    fis.transferTo(zos);
                    zos.closeEntry();
                }
            }
        }
    }

    private void expand(WorldFile loaded_file) {
        Thread.ofPlatform().start(() -> {
            var file = loaded_file.file();
            var name = file.getName();
            var confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to expand: " + name, "Expand", JOptionPane.YES_NO_OPTION);

            var keep = JOptionPane.showConfirmDialog(frame, "Do you want to keep the archive after expanding?", "Keep Archive", JOptionPane.YES_NO_OPTION);

            Path output_path;
            while (true) {
                var output = JOptionPane.showInputDialog(frame, "Enter the output folder name", "Output Directory", JOptionPane.QUESTION_MESSAGE);
                if (output == null) {
                    return;
                }
                output_path = new File(folder, output).toPath();
                if (Files.exists(output_path)) {
                    JOptionPane.showConfirmDialog(frame, "Already exists, please pick another name", "Output Exists", JOptionPane.ERROR_MESSAGE);
                } else {
                    break;
                }
            }

            if (confirm == JOptionPane.YES_OPTION) {
                try (
                    var fis = new BufferedInputStream(new FileInputStream(file));
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
                    if (!file.delete()) {
                        JOptionPane.showMessageDialog(frame, "Failed to delete: " + name, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

                reloadAllWorlds();
            }
        });
    }

    private void delete(LoadedFile loaded_file) {
        Thread.ofPlatform().start(() -> {
            var file = loaded_file.file();
            var name = file.getName();
            if (loaded_file instanceof WorldFile world) {
                name = world.data().worldName();
            }
            var confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete: " + name, "Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (!file.delete()) {
                    JOptionPane.showMessageDialog(frame, "Failed to delete: " + name, "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    reloadAllWorlds();
                }
            }
        });
    }

    private static final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    private void copy(LoadedFile file) {
        try {
            clipboard.setContents(new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[] { DataFlavor.javaFileListFlavor };
                }
    
                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return flavor.equals(DataFlavor.javaFileListFlavor);
                }
    
                @Override
                public Object getTransferData(DataFlavor flavor) {
                    return List.of(file.file());
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Failed to copy to clipboard: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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


    static ArchiveInputStream<? extends ArchiveEntry> createArchiveInputStream(InputStream is) throws FileNotFoundException, ArchiveException {
        var type = ArchiveStreamFactory.detect(is);

        if (ArchiveStreamFactory.ZIP.equals(type)) {
            return new ZipArchiveInputStream(is, StandardCharsets.UTF_8.name(), true, true);
        }
        return new ArchiveStreamFactory().createArchiveInputStream(type, is);
    }
}
