package canaryprism.mcwm.swing;

import canaryprism.mcwm.Main;
import canaryprism.mcwm.instance.InstanceFinder;
import canaryprism.mcwm.instance.SaveDirectory;
import canaryprism.mcwm.swing.instance.SaveDirectoryView;
import canaryprism.mcwm.swing.instance.SaveFinderView;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;

public class InstancePickerView extends JComponent {
    
    public static final int LAUNGER_ENTRY_WIDTH = 500, LAUNCHER_ENTRY_HEIGHT = 40;
    public static final int INSTANCE_ENTRY_WIDTH = 300, INSTANCE_ENTRY_HEIGHT = 30;
    public static final int INSTANCE_PICKER_MIN_HEIGHT = 500;
    
    private final Main main;
    private final ServiceLoader<InstanceFinder> finders = ServiceLoader.load(InstanceFinder.class);
    private final JList<InstanceFinder> launcher_list = new JList<>();
    
    private final Path cache_path;
    
    public InstancePickerView(Main main, Path cache_path) {
        this.main = main;
        this.cache_path = cache_path;
        this.setLayout(new BorderLayout());
        
        var label = new JLabel("""
                <html><h2>Select a Launcher</h2></html>
                """);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(label, BorderLayout.NORTH);
        
        launcher_list.setCellRenderer((_, value, _, _, _) -> {
            var entry = new SaveFinderView(value);
            entry.setPreferredSize(new Dimension(LAUNGER_ENTRY_WIDTH, LAUNCHER_ENTRY_HEIGHT));
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
                    addSaveDirectory(instances.getFirst());
                } else {
                    pickInstance(instances)
                            .thenAccept(this::addSaveDirectory)
                            .exceptionally((_) -> null);
                }
            }
            launcher_list.setSelectedValue(null, false);
        });
        
        reloadFinders(true);
        
        var scroll_pane = new JScrollPane(launcher_list);
        
        this.add(scroll_pane, BorderLayout.CENTER);
        
        var bottom_panel = createBottomButtonPanel();
        
        this.add(bottom_panel, BorderLayout.SOUTH);
    }
    
    private JPanel createBottomButtonPanel() {
        var bottom_panel = new JPanel();
        var reload_button = new JButton("Reload");
        reload_button.addActionListener((_) -> reloadFinders(false));
        var custom_button = new JButton("Custom Folder");
        custom_button.addActionListener((_) ->
                Main.openDialog().ifPresent((path) ->
                        addSaveDirectory(new SaveDirectory(Optional.empty(), "Custom", path))));
        bottom_panel.add(reload_button);
        bottom_panel.add(custom_button);
        return bottom_panel;
    }
    
    private void reloadFinders(boolean use_cache) {
        finders.reload();
        var finder_list = finders.stream().map(ServiceLoader.Provider::get).toList();
        finder_list.stream()
                .filter((e) -> {
                    if (!use_cache)
                        return true;
                    try {
                        var cache = cache_path.resolve(e.getClass().getName());
                        if (Files.exists(cache))
                            e.loadCache(cache);
                    } catch (IOException _) {}
                    return e.getSavesPaths().isEmpty();
                })
                .forEach((e) -> {
                    e.find();
                    
                    try {
                        e.writeCache(cache_path.resolve(e.getClass().getName()));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
        launcher_list.setListData(finder_list.toArray(InstanceFinder[]::new));
    }
    
    private void addSaveDirectory(SaveDirectory directory) {
        main.add(directory);
    }
    
    
    private static CompletableFuture<SaveDirectory> pickInstance(SequencedCollection<SaveDirectory> instances) {
        @SuppressWarnings("OverlyStrongTypeCast")
        var dialog = new JDialog(((JDialog) null), "Select Instance");
        
        var future = new CompletableFuture<SaveDirectory>();
        
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
        
        instance_list.setCellRenderer((_, value, _, _, _) -> {
            var entry = new SaveDirectoryView(value);
            entry.setPreferredSize(new Dimension(INSTANCE_ENTRY_WIDTH, INSTANCE_ENTRY_HEIGHT));
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
                future.complete(selected);
            }
        });
        
        var scroll_pane = new JScrollPane(instance_list);
        
        {
            var pref_size = instance_list.getPreferredSize();
            pref_size.height = Math.min(pref_size.height, INSTANCE_PICKER_MIN_HEIGHT);
            scroll_pane.getViewport().setPreferredSize(pref_size);
        }
        
        dialog.add(scroll_pane);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setVisible(true);
        
        return future;
    }
}
