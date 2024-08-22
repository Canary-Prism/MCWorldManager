package canaryprism.mcwm.swing.nbt;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class NBTTreeTransferHandler extends TransferHandler {
    DataFlavor nodesFlavor;
    DataFlavor[] flavors = new DataFlavor[1];
    DefaultMutableTreeNode[] nodesToRemove;
    DefaultMutableTreeNode[] transferring_nodes;

    public NBTTreeTransferHandler() {
        try {
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
                    ";class=\"" +
                    javax.swing.tree.DefaultMutableTreeNode[].class.getName() +
                    "\"";
            nodesFlavor = new DataFlavor(mimeType);
            flavors[0] = nodesFlavor;
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFound: " + e.getMessage());
        }
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        support.setShowDropLocation(true);
        if (!support.isDataFlavorSupported(nodesFlavor)) {
            return false;
        }
        // Do not allow a drop on the drag source selections.
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        JTree tree = (JTree) support.getComponent();
        tree.getRowForPath(dl.getPath());
        int[] selRows = tree.getSelectionRows();
        // for (int i = 0; i < selRows.length; i++) {
        //     if (selRows[i] == dropRow) {
        //         return false;
        //     }
        // }
        
        // // Do not allow MOVE-action drops if a non-leaf node is
        // // selected unless all of its children are also selected.
        // int action = support.getDropAction();
        // if (action == MOVE) {
        //     return haveCompleteNode(tree);
        // }

        // // Do not allow a non-leaf node to be copied to a level
        // // which is less than its source level.
        // TreePath dest = dl.getPath();
        // DefaultMutableTreeNode target = (DefaultMutableTreeNode) dest.getLastPathComponent();
        // TreePath path = tree.getPathForRow(selRows[0]);
        // DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        // if (firstNode.getChildCount() > 0 &&
        //         target.getLevel() < firstNode.getLevel()) {
        //     return false;
        // }


        // // require that the parent node doesn't change
        // TreePath dest = dl.getPath();
        // DefaultMutableTreeNode target = (DefaultMutableTreeNode) dest.getLastPathComponent();
        // TreePath path = tree.getPathForRow(selRows[0]);
        // DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        // if (firstNode.getParent() != target.getParent()) {
        //     return false;
        // }

        // require only one transferring node
        if (selRows.length != 1) {
            return false;
        }

        // require that the parent node can hold the child node

        TreePath path = dl.getPath();
        if (path == null) {
            return false;
        }
        
        DefaultMutableTreeNode newParent = (DefaultMutableTreeNode) path.getLastPathComponent();

        var destination_tag = NBTView.getTag(newParent.getUserObject());
        var tag = ((DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent()).getUserObject();

        if (destination_tag == null || tag == null) {
            return false;
        }
        // don't let you drop a node onto itself or its children it will vanish
        if (tree.getSelectionPath().isDescendant(path)) {
            return false;
        }
        return NBTView.canHold(destination_tag, tag);
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null) {
            // Make up a node array of copies for transfer and
            // another for/of the nodes that will be removed in
            // exportDone after a successful drop.
            List<DefaultMutableTreeNode> copies = new ArrayList<DefaultMutableTreeNode>();
            List<DefaultMutableTreeNode> toRemove = new ArrayList<DefaultMutableTreeNode>();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
            DefaultMutableTreeNode copy = copy(node);
            copies.add(copy);
            toRemove.add(node);
            for (int i = 1; i < paths.length; i++) {
                DefaultMutableTreeNode next = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
                // Do not allow higher level nodes to be added to list.
                if (next.getLevel() < node.getLevel()) {
                    break;
                } else if (next.getLevel() > node.getLevel()) { // child node
                    copy.add(copy(next));
                    // node already contains child
                } else { // sibling
                    copies.add(copy(next));
                    toRemove.add(next);
                }
            }
            DefaultMutableTreeNode[] nodes = copies.toArray(new DefaultMutableTreeNode[copies.size()]);
            this.transferring_nodes = copies.toArray(new DefaultMutableTreeNode[copies.size()]);
            nodesToRemove = toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
            return new NodesTransferable(nodes);
        }
        return null;
    }

    /** Defensive copy used in createTransferable. */
    private DefaultMutableTreeNode copy(TreeNode node) {
        if (node instanceof DefaultMutableTreeNode n) {
            var node_tree = NBTView.reconstructNbt(n.getUserObject(), n);
            var new_node = new DefaultMutableTreeNode(node_tree);
            NBTView.load(new_node);
            return new_node;
        } else {
            return new DefaultMutableTreeNode(node);
        }
    }

    protected void exportDone(JComponent source, Transferable data, int action) {
        if ((action & MOVE) == MOVE) {
            JTree tree = (JTree) source;
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            // Remove nodes saved in nodesToRemove in createTransferable.
            for (int i = 0; i < nodesToRemove.length; i++) {
                model.removeNodeFromParent(nodesToRemove[i]);
            }

            // set the transferred nodes to be selected
            for (int i = 0; i < transferring_nodes.length; i++) {
                tree.addSelectionPath(new TreePath(transferring_nodes[i].getPath()));
            }
        }
    }

    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        // Extract transfer data.
        DefaultMutableTreeNode[] nodes = null;
        try {
            Transferable t = support.getTransferable();
            nodes = (DefaultMutableTreeNode[]) t.getTransferData(nodesFlavor);
        } catch (UnsupportedFlavorException ufe) {
            System.out.println("UnsupportedFlavor: " + ufe.getMessage());
        } catch (java.io.IOException ioe) {
            System.out.println("I/O error: " + ioe.getMessage());
        }
        // Get drop location info.
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        int childIndex = dl.getChildIndex();
        TreePath dest = dl.getPath();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
        JTree tree = (JTree) support.getComponent();
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        // Configure for drop mode.
        int index = childIndex; // DropMode.INSERT
        if (childIndex == -1) { // DropMode.ON
            index = parent.getChildCount();
        }
        // Add data to model.
        for (int i = 0; i < nodes.length; i++) {
            model.insertNodeInto(nodes[i], parent, index++);
        }
        return true;
    }

    public String toString() {
        return getClass().getName();
    }

    public class NodesTransferable implements Transferable {
        DefaultMutableTreeNode[] nodes;

        public NodesTransferable(DefaultMutableTreeNode[] nodes) {
            this.nodes = nodes;
        }

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor))
                throw new UnsupportedFlavorException(flavor);
            return nodes;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return nodesFlavor.equals(flavor);
        }
    }
}