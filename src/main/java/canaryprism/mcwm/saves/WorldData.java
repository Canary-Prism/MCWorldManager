package canaryprism.mcwm.saves;

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import canaryprism.mcwm.Main;
import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.tag.CompoundTag;

public record WorldData(Optional<Image> image, String worldName, String dirName, LocalDateTime lastPlayed, Gamemode gamemode, boolean cheats, boolean experimental, String version) {
    
    public static WorldData parse(File file) throws ParsingException {
        try {
            if (file.isDirectory()) {
                var level_dat = new File(file, "level.dat");
                var icon = new File(file, "icon.png");
                if (icon.exists()) {
                    return parse(new FileInputStream(icon), new FileInputStream(level_dat), file.getName());
                } else {
                    return parse(InputStream.nullInputStream(), new FileInputStream(level_dat), file.getName());
                }
            } else {
                byte[] imgbuffer = {}, levelbuffer = null;
                String dir_name = null;
                boolean found_level = false;
                boolean found_icon = false;
                var images = new HashMap<String, byte[]>();
                try (
                    var fis = new BufferedInputStream(new FileInputStream(file));
                    var i = Main.createArchiveInputStream(fis);
                ) {
                    ArchiveEntry entry = null;
                    while ((entry = i.getNextEntry()) != null) {
                        if (!i.canReadEntryData(entry)) {
                            // log something?
                            continue;
                        }

                        var split = entry.getName().split("/");
                        var name = split[split.length - 1];
                        
                        // i want to determine the directory of the world by where the level.dat file is
                        // so it works even if the world is in a subdirectory
                        if (name.equals("level.dat")) {
                            if (found_level) {
                                throw new ParsingException("Multiple worlds (level.dat) files found in archive");
                            }
                            found_level = true;
                            levelbuffer = i.readAllBytes();
                            try {
                                dir_name = entry.getName().substring(0, entry.getName().length() - name.length() - 1);
                            } catch (StringIndexOutOfBoundsException e) {
                                dir_name = ""; // this means the level.dat file is in the root of the archive
                            }
                        } else if (name.equals("icon.png")) {
                            if (!found_icon) {
                                images.put(entry.getName(), i.readAllBytes());
                                if (found_level) {
                                    if (images.containsKey(dir_name + "/icon.png")) {
                                        imgbuffer = images.get(dir_name + "/icon.png");
                                        found_icon = true;
                                        // images.clear();
                                        images = null;
                                    }
                                }
                            }
                        }
                    }

                    if (found_level) {
                        // try just one more time to get the icon
                        if (!found_icon) {
                            if (images.containsKey(dir_name + "/icon.png")) {
                                imgbuffer = images.get(dir_name + "/icon.png");
                                found_icon = true;
                                // images.clear();
                                images = null;
                            }
                        }

                        return parse(new ByteArrayInputStream(imgbuffer), new ByteArrayInputStream(levelbuffer), dir_name);
                    } else {
                        throw new ParsingException("No world (level.dat) file found in archive");
                    }
                }
            } 
        } catch (FileNotFoundException | ArchiveException e) {
            throw new ParsingException("Failed to parse world data", e);
        } catch (IOException e) {
            throw new ParsingException("Failed to read world archive data", e);
        }
    }

    public static WorldData parse(InputStream image_input_stream, InputStream level_dat_input_stream, String dir_name) throws ParsingException {
        try {
            Optional<Image> image;
            try {
                image = Optional.of(ImageIO.read(image_input_stream));
            } catch (Exception e) {
                image = Optional.empty();
            }
            var level_dat = new NBTDeserializer().fromStream(level_dat_input_stream);

            var data = ((CompoundTag)level_dat.getTag()).getCompoundTag("Data");

            var worldName = data.getString("LevelName");
            var lastPlayed = LocalDateTime.ofInstant(Instant.ofEpochMilli(data.getLong("LastPlayed")), ZoneId.systemDefault());
            
            var gamemode = Gamemode.fromIndex(data.getInt("GameType"));
            if (data.getBoolean("hardcore")) {
                gamemode = Gamemode.hardcore;
            }
            var cheats = data.getBoolean("allowCommands");

            var experimental = data.containsKey("enabled_features");

            // i'm not sure if this is the correct way to get the version name
            var version = data.getCompoundTag("Version").getString("Name");

            return new WorldData(image, worldName, dir_name, lastPlayed, gamemode, cheats, experimental, version);
        } catch (Exception e) {
            throw new ParsingException("Malformed level.dat file", e);
        }
    }
}
