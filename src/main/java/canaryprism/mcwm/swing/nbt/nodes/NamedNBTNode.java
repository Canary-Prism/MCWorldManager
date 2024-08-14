package canaryprism.mcwm.swing.nbt.nodes;

import javax.swing.tree.DefaultMutableTreeNode;

import net.querz.nbt.io.NamedTag;

public class NamedNBTNode extends DefaultMutableTreeNode {
    private final NamedTag tag;

    public NamedNBTNode(NamedTag tag) {
        super(tag.getName() + ": " + NBTNode.getTagName(tag.getTag()));
        this.tag = tag;
    }

    public NamedTag getNamedTag() {
        return tag;
    }
}
