package canaryprism.mcwm.swing.nbt.nodes.primitive;

import javax.swing.tree.DefaultMutableTreeNode;

public class IntNode extends DefaultMutableTreeNode {
    
    private volatile int value;

    public IntNode(int value) {
        this.allowsChildren = false;
        this.value = value;
        this.setUserObject(value);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
        this.setUserObject(value);
    }
}
