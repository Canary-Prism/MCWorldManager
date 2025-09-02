import canaryprism.mcwm.instance.InstanceFinder;
import canaryprism.mcwm.instance.launcher.*;

module canaryprism.mcwm {
    requires canaryprism.querz.nbt;
    requires com.formdev.flatlaf;
    requires dev.dirs;
    requires java.datatransfer;
    requires java.desktop;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;
    requires org.json;
    requires org.apache.commons.configuration2;
    
    uses InstanceFinder;
    
    provides InstanceFinder with
            Curseforge,
            Modrinth,
            MultiMC,
            Prism,
            Vanilla;
}