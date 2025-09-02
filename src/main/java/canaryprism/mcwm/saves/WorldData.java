package canaryprism.mcwm.saves;

import canaryprism.mcwm.Main;
import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.tag.CompoundTag;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

public record WorldData(Optional<Image> image, String worldName, String dirName, LocalDateTime lastPlayed, Gamemode gamemode, boolean cheats, boolean experimental, String version) {
    
    public static WorldData parse(Path path) throws ParsingException {
        try (var fs = (Files.isDirectory(path)) ? null : Main.createArchiveFileSystem(path)) {
            var world_path = (fs != null) ? findWorldPath(fs.getRootDirectories().iterator().next()) : path;
            var level_dat = world_path.resolve("level.dat");
            var icon = world_path.resolve("icon.png");
            try (var icon_stream = Files.exists(icon) ? Files.newInputStream(icon) : InputStream.nullInputStream();
                 var level_stream = Files.newInputStream(level_dat)) {
                return parse(icon_stream, level_stream, Optional.ofNullable(path.getFileName()).map(Path::toString).orElse(""));
            }
        } catch (IOException | URISyntaxException e) {
            throw new ParsingException("Failed to read world archive data", e, "Potentially problematic file");
        }
    }
    
    public static Path findWorldPath(Path path) throws ParsingException {
        
        try (var stream = Files.walk(path)) {
            var list = stream
                    .filter(Files::isDirectory)
                    .filter((e) -> Files.exists(e.resolve("level.dat")))
                    .toList();
            if (list.isEmpty())
                throw new ParsingException("No world (level.dat) file found in archive", "Archive potentially malformed or does not contain a world");
            
            if (list.size() > 1)
                throw new ParsingException("Multiple worlds (level.dat) files found in archive", "Archive potentially malformed or contains multiple worlds");
            
            return list.getFirst();
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
