package canaryprism.mcwm.savedir.launcher;

import java.awt.Image;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;

import canaryprism.mcwm.savedir.SaveDirectory;
import canaryprism.mcwm.savedir.SaveFinder;


/**
 * the Prism launcher save finder
 * <p>
 * this class is responsible for finding the saves directory of the Prism launcher,
 * which is less annoying than MultiMC but still annoying because it has a portable version
 */
public class Prism implements SaveFinder {

    private static final Optional<Image> icon;
    static {
        Optional<Image> temp_icon;
        try (var is = MultiMC.class.getResourceAsStream("/mcwm/launcher/prism/icon.png")) {
            temp_icon = Optional.ofNullable(ImageIO.read(is));
        } catch (Exception e) {
            e.printStackTrace(); // swallowing exceptions is bad
            temp_icon = Optional.empty();
        }
        icon = temp_icon;
    }

    private static final Optional<Image> default_world_icon;
    static {
        Optional<Image> temp_icon;
        try (var is = MultiMC.class.getResourceAsStream("/mcwm/launcher/prism/default_instance_icon.png")) {
            temp_icon = Optional.ofNullable(ImageIO.read(is));
        } catch (Exception e) {
            e.printStackTrace(); // swallowing exceptions is bad
            temp_icon = Optional.empty();
        }
        default_world_icon = temp_icon;
    }

    private final List<SaveDirectory> saves_path = new ArrayList<>();

    @Override
    public synchronized void findWindows() {

        // once again thank you prism for actually putting your instances in a consistent location
        var appdata = System.getenv("APPDATA");
        var home = System.getProperty("user.home");

        var default_prism = Path.of(appdata, "PrismLauncher", "instances");

        var scoop_prism = Path.of(home, "scoop", "persist", "prismlauncher", "instances");

        Path instances;
        if (Files.isDirectory(default_prism)) {
            instances = default_prism;
        } else if (Files.isDirectory(scoop_prism)) { // apparently this is a thing
            instances = scoop_prism;
        } else {

            // but it isn't all sunshine and rainbows, is it, prism?
            // you just had to have a portable version that doesn't have a consistent location
            // it instead uses the MultiMC method of having a working directory relative to the exe
            // so i'll have to find it the hard way.. again.
            // you almost had it, prism, you almost had it.

    
    
            var targets = List.of(
                Path.of(home, "Downloads"), 
                Path.of(home, "Documents"), 
                Path.of(home, "Desktop"),
                Path.of(appdata) // who the heck would put prism in appdata
            );
    
            final var future = new CompletableFuture<Path>();
            var countdown = new CountDownLatch(targets.size());
    
            for (var target : targets) {
                Thread.ofVirtual().start(() -> {
                    try {
                        Files.walkFileTree(target, new SimpleFileVisitor<>() {
    
                            final int max_depth = 2;
                            int depth = 0;
    
                            @Override
                            public FileVisitResult preVisitDirectory(Path dir,
                                    java.nio.file.attribute.BasicFileAttributes attrs) {
                                if (Files.isDirectory(dir)
                                        && Files.isRegularFile(dir.resolve("prismlauncher.exe"))
                                        && Files.isDirectory(dir.resolve("instances"))) { // i'm assuming it looks like this, i haven't actually seen it yet
    
                                    future.complete(dir.resolve("instances"));
    
                                    return java.nio.file.FileVisitResult.TERMINATE;
                                }
                                if (future.isDone()) {
                                    return java.nio.file.FileVisitResult.TERMINATE;
                                }
    
                                if (depth >= max_depth) {
                                    return java.nio.file.FileVisitResult.SKIP_SUBTREE;
                                }
                                depth++;
                                return java.nio.file.FileVisitResult.CONTINUE;
                            }
    
                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                                depth--;
                                return java.nio.file.FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    countdown.countDown();
                });
            }
    
            Thread.ofVirtual().start(() -> {
                try {
                    countdown.await();
                    future.completeExceptionally(new RuntimeException("MultiMC not found"));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            try {
                instances = future.get();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }


        load(instances);
    }

    @Override
    public synchronized void findMac() {
        var home = System.getProperty("user.home");

        // thank you prism for using a constant location at least in mac
        // thanks for nothing multimc
        var prism = Path.of(home, "Library", "Application Support", "PrismLauncher");

        var instances = prism.resolve("instances");

        if (Files.isDirectory(instances)) {
            load(instances);
        }
    }

    @Override
    public synchronized void findLinux() {

        // and holy shit prism actually documented their working directory so i can just see that and implement Linux right now

        var home = System.getProperty("user.home");

        var default_prism = Path.of(home, ".local", "share", "PrismLauncher", "instances");
        var flatpak_prism = Path.of(home, ".var", "app", "org.prismlauncher.PrismLauncher", "data", "PrismLauncher", "instances");

        Path instances;
        if (Files.isDirectory(default_prism)) {
            instances = default_prism;
        } else if (Files.isDirectory(flatpak_prism)) { // apparently this is also a thing
            instances = flatpak_prism;
        } else {

            // again with the portable versions!
            // i almost don't wanna bother

            var targets = List.of(Path.of(home, "Downloads"), Path.of(home, "Documents"), Path.of(home, "Desktop"));

            final var future = new CompletableFuture<Path>();
            var countdown = new CountDownLatch(targets.size());

            for (var target : targets) {
                Thread.ofVirtual().start(() -> {
                    try {
                        Files.walkFileTree(target, new SimpleFileVisitor<>() {

                            final int max_depth = 2;
                            int depth = 0;

                            @Override
                            public FileVisitResult preVisitDirectory(Path dir,
                                    java.nio.file.attribute.BasicFileAttributes attrs) {
                                if (Files.isDirectory(dir)
                                        && Files.isRegularFile(dir.resolve("PrismLauncher"))
                                        && Files.isDirectory(dir.resolve("instances"))) { // i'm assuming it looks like this, i haven't actually seen it yet

                                    future.complete(dir.resolve("instances"));

                                    return java.nio.file.FileVisitResult.TERMINATE;
                                }
                                if (future.isDone()) {
                                    return java.nio.file.FileVisitResult.TERMINATE;
                                }

                                if (depth >= max_depth) {
                                    return java.nio.file.FileVisitResult.SKIP_SUBTREE;
                                }
                                depth++;
                                return java.nio.file.FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                                depth--;
                                return java.nio.file.FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    countdown.countDown();
                });
            }

            Thread.ofVirtual().start(() -> {
                try {
                    countdown.await();
                    future.completeExceptionally(new RuntimeException("MultiMC not found"));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            try {
                instances = future.get();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        load(instances);
    }

    private void load(Path instances) {
        try {
            Files.list(instances)
                .filter(Files::isDirectory)
                .filter((e) -> Files.isDirectory(e.resolve(".minecraft", "saves")))
                .map((e) -> {
                    try {
                        return new SaveDirectory(
                            Optional.of(ImageIO.read(e.resolve(".minecraft", "icon.png").toFile())),
                            e.getFileName().toString(),
                            e.resolve(".minecraft", "saves")
                        );
                    } catch (IOException ex) {
                        return new SaveDirectory(
                            default_world_icon,
                            e.getFileName().toString(),
                            e.resolve(".minecraft", "saves")
                        );
                    }
                })
                .forEach(saves_path::add);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized List<SaveDirectory> getSavesPaths() {
        return saves_path;
    }

    @Override
    public String getLauncherName() {
        return "Prism Launcher";
    }

    @Override
    public Optional<Image> getIcon() {
        return icon;
    }

}
