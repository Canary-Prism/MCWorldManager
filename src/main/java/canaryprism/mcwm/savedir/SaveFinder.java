package canaryprism.mcwm.savedir;

import java.awt.Image;
import java.util.List;
import java.util.Optional;

public interface SaveFinder {
    void findWindows();

    void findMac();

    /**
     * I'd just like to interject for a moment. What you're refering to as Linux, is
     * in fact, GNU/Linux, or as I've recently taken to calling it, GNU plus Linux.
     * Linux is not an operating system unto itself, but rather another free
     * component of a fully functioning GNU system made useful by the GNU corelibs,
     * shell utilities and vital system components comprising a full OS as defined
     * by POSIX.
     * 
     * @return your mom
     */
    void findLinux();

    List<SaveDirectory> getSavesPaths();


    String getLauncherName();

    Optional<Image> getIcon();
}
