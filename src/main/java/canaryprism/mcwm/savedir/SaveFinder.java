package canaryprism.mcwm.savedir;

import java.awt.Image;
import java.util.List;
import java.util.Optional;

public interface SaveFinder {

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
     * you should call findWindows, findMac, or findLinux before calling this
     * 
     * @return list of SaveDirectory objects found by the finder, not null
     */
    List<SaveDirectory> getSavesPaths();


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
