package canaryprism.mcwm.savedir.launcher;

import java.awt.Image;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;

import canaryprism.mcwm.savedir.SaveDirectory;
import canaryprism.mcwm.savedir.SaveFinder;


/**
 * the Modrinth launcher save finder
 * <p>
 * this class is responsible for finding the saves directory of the Modrinth launcher,
 * which is like vanilla and only has one save directory per platform, hooray
 */
public class Modrinth implements SaveFinder {
    private static final Optional<Image> icon;
    static {
        Optional<Image> temp_icon;
        try (var is = Modrinth.class.getResourceAsStream("/mcwm/launcher/modrinth/icon.png")) {
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
        try (var is = Modrinth.class.getResourceAsStream("/mcwm/launcher/modrinth/default_instance_icon.png")) {
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

        var appdata = System.getenv("APPDATA");

        var path = Path.of(appdata, "ModrintApp");
        if (!Files.isDirectory(path)) {
            path = Path.of(appdata, "com.modrinth.theseus");
        }

        load(path);
    }

    @Override
    public synchronized void findMac() {

        var home = System.getProperty("user.home");

        var path = Path.of(home, "Library", "Application Support", "ModrinthApp");

        if (!Files.isDirectory(path)) {
            path = Path.of(home, "Library", "Application Support", "com.modrinth.theseus");
        }

        load(path);
    }

    @Override
    public synchronized void findLinux() {

        // what the hell is xdg_config_home
        var xdg_config_home = System.getenv("XDG_CONFIG_HOME");
        var home = System.getProperty("user.home");

        if (xdg_config_home == null) { // if it's not set
            xdg_config_home = Path.of(home, ".config").toString();
        }

        var path = Path.of(xdg_config_home, "com.modrinth.theseus");

        if (!Files.isDirectory(path)) {
            // this seems to be the directory that my machine uses
            // at this point the path modrinth says they use on the website has been accurate exactly 0 times
            // i don't even think $XDG_CONFIG_HOME is a thing
            path = Path.of(home, ".local", "share", "ModrinthApp");
        }

        if (!Files.isDirectory(path)) { // last ditch effort

            // apparently fedora does this???
            path = Path.of(home, ".var", "app", "com.modrinth.ModrinthApp", "config", "com.modrinth.theseus");
        }

        // okay linux is definitely the most annoying to find

        load(path);
    }

    private void load(Path modrinth) {

        // this is by far my favourite launcher just because of how easy it is to find the saves

        var instances = modrinth.resolve("profiles");
        if (Files.isDirectory(instances)) {
            try {
                Files.list(instances)
                    .filter((e) -> Files.isDirectory(e.resolve("saves")))
                    .map((e) -> {
                        try {
                            return new SaveDirectory(
                                Optional.of(ImageIO.read(
                                    Path.of(
                                        new JSONObject(Files.readString(e.resolve("profile.json")))
                                            .getJSONObject("metadata")
                                            .getString("icon") // this is a bit convoluted
                                    ).toFile()
                                )),
                                e.getFileName().toString(),
                                e.resolve("saves")
                            );
                        } catch (IOException | JSONException ex) {
                            return new SaveDirectory(
                                default_world_icon,
                                e.getFileName().toString(),
                                e.resolve("saves")
                            );
                        }
                    })
                    .forEach(saves_path::add);
                
                cache_path = modrinth;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private volatile Path cache_path;

    @Override
    public Optional<Path> toCache() {
        return Optional.ofNullable(cache_path);
    }

    @Override
    public void loadCache(Path path) {
        if (Files.isDirectory(path))
            load(path);
    }


    @Override
    public synchronized List<SaveDirectory> getSavesPaths() {
        return saves_path;
    }

    @Override
    public String getLauncherName() {
        return "Modrinth App";
    }

    @Override
    public Optional<Image> getIcon() {
        return icon;
    }

}
