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

public class MultiMC implements SaveFinder {

    private static final Optional<Image> icon;
    static {
        Optional<Image> temp_icon;
        try (var is = MultiMC.class.getResourceAsStream("/mcwm/launcher/multimc.png")) {
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
        try (var is = MultiMC.class.getResourceAsStream("/mcwm/grass.png")) {
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
        // i'll just try to find it by searching in downloads documents and desktop
        // since multimc doesn't have an actual working directory not relative to its exe
        // and there isn't a standard place to put multimc in windows

        var home = System.getProperty("user.home");

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
                        public FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs) {
                            if (Files.isDirectory(dir) 
                                && Files.isRegularFile(dir.resolve("MultiMC.exe"))
                                && Files.isDirectory(dir.resolve("instances"))) {
    
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

        Path instances;
        try {
            instances = future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // the rest is the same as Mac (or at least i'm assuming)

        try {
            Files.list(instances)
                .filter(Files::isDirectory)
                .filter((e) -> Files.isDirectory(e.resolve(".minecraft")))
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
    public synchronized void findMac() {
        var home = System.getProperty("user.home");

        var multimc = Path.of(home, "Applications", "MultiMC.app");
        if (!Files.isDirectory(multimc)) { // give it 1 more try from root
            multimc = Path.of("/", "Applications", "MultiMC.app");
        }

        var instances = multimc.resolve("Data", "instances");
        if (Files.isDirectory(instances)) {
            try {
                Files.list(instances)
                    .filter(Files::isDirectory)
                    .filter((e) -> Files.isDirectory(e.resolve(".minecraft")))
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
    }

    @Override
    public synchronized void findLinux() {
        // TODO: implement linux
    }

    @Override
    public synchronized List<SaveDirectory> getSavesPaths() {
        return saves_path;
    }

    @Override
    public String getLauncherName() {
        return "MultiMC Launcher <WIP>";
    }

    @Override
    public Optional<Image> getIcon() {
        return icon;
    }

    
}
