package canaryprism.mcwm.saves;

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    
    public static WorldData parse(Path file) throws ParsingException {
        try {
            if (Files.isDirectory(file)) {
                var level_dat = file.resolve("level.dat");
                var icon = file.resolve("icon.png");
                try {
                    if (Files.exists(icon)) {
                        try (
                            var icon_stream = Files.newInputStream(icon);
                            var level_dat_stream = Files.newInputStream(level_dat)
                        ) {
                            return parse(icon_stream, level_dat_stream, file.getFileName().toString());
                        }
                    } else {
                        try (var level_dat_stream = Files.newInputStream(level_dat)) {
                            return parse(InputStream.nullInputStream(), level_dat_stream, file.getFileName().toString());
                        }
                    }
                } catch (IOException e) {
                    throw new ParsingException("No world (level.dat) file found in folder", e, "Not a Minecraft world");
                }
            } else {
                byte[] imgbuffer = {}, levelbuffer = null;
                String dir_name = null;
                boolean found_level = false;
                boolean found_icon = false;
                var images = new HashMap<String, byte[]>();
                try (
                    var fis = new BufferedInputStream(Files.newInputStream(file));
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
                                throw new ParsingException("Multiple worlds (level.dat) files found in archive", "Archive potentially malformed or contains multiple worlds");
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

                        try (
                            var imgstream = new ByteArrayInputStream(imgbuffer);
                            var levelstream = new ByteArrayInputStream(levelbuffer);
                        ) {
                            return parse(imgstream, levelstream, dir_name);
                        }
                    } else {
                        throw new ParsingException("No world (level.dat) file found in archive", "Archive potentially malformed or does not contain a world");
                    }
                }
            } 
        } catch (FileNotFoundException | ArchiveException e) {
            throw new ParsingException("Failed to parse world data", e, "Not a Minecraft world");
        } catch (IOException e) {
            throw new ParsingException("Failed to read world archive data", e, "Potentially problematic file");
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

            var worldName = data.getStringTag("LevelName").getValue();
            var lastPlayed = LocalDateTime.ofInstant(Instant.ofEpochMilli(data.getLongTag("LastPlayed").asLong()), ZoneId.systemDefault());
            
            var gamemode = Gamemode.fromIndex(data.getIntTag("GameType").asInt());
            if (data.getByteTag("hardcore").asBoolean()) {
                gamemode = Gamemode.hardcore;
            }
            var cheats = data.getByteTag("allowCommands").asBoolean();

            var experimental = data.containsKey("enabled_features");

            String version;
            try { // because older versions of Minecraft don't have this tag
                version = data.getCompoundTag("Version").getStringTag("Name").getValue(); // throw a goddamn exception it's not that fucking hard
            } catch (Exception e) {
                throw new ParsingException("Failed to parse version data", e, "Potentially outdated or corrupted Minecraft world");
            }

            return new WorldData(image, worldName, dir_name, lastPlayed, gamemode, cheats, experimental, version);
        } catch (Exception e) {
            if (e instanceof ParsingException n) {
                throw n;
            }
            throw new ParsingException("Malformed level.dat file", e, "Potentially corrupted Minecraft world");
        }
    }
}
