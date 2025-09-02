package canaryprism.mcwm.instance;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.SystemUtils.*;

public interface InstanceFinder {
    
    default void find() {
        if (IS_OS_MAC)
            findMac();
        else if (IS_OS_WINDOWS)
            findWindows();
        else if (IS_OS_LINUX)
            findLinux();
    }

    /**
     * attempt to find the saves directory for Windows
     */
    void findWindows();

    /**
     * attempt to find the saves directory for Mac
     */
    void findMac();

    /**
     * I'd just like to interject for a moment. What you're refering to as Linux, is
     * in fact, GNU/Linux, or as I've recently taken to calling it, GNU plus Linux.
     * Linux is not an operating system unto itself, but rather another free
     * component of a fully functioning GNU system made useful by the GNU corelibs,
     * shell utilities and vital system components comprising a full OS as defined
     * by POSIX.
     * 
     * <p>
     * attempt to find the saves directory for Linux
     * 
     * @return your mom
     */
    void findLinux();


    /**
     * returns a list of all the saves paths found, if any
     * <p>
     * you should call findWindows, findMac, findLinux, or loadCache before calling this
     * 
     * @return list of SaveDirectory objects found by the finder, not null
     */
    List<SaveDirectory> getSavesPaths();


    /**
     * writes the cache to the passed path
     *
     * @param path the path to the cache file
     */
    default void writeCache(Path path) throws IOException {}

    /**
     * loads the saves directory from the cache
     * 
     * @param path the path to the cache file
     */
    default void loadCache(Path path) throws IOException {}


    /**
     * name of the launcher the finder is for
     * 
     * @return the name of the launcher
     */
    String getLauncherName();

    /**
     * gets the icon for the launcher the finder is for, 
     * or empty if there is no icon
     * 
     * @return optional icon for the launche
     */
    Optional<Image> getIcon();
}
