package play.libs;

import java.util.List;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * XPath for parsing
 */
public class XPath {

    /**
     * Select all nodes that are selected by this XPath expression. If multiple nodes match,
     * multiple nodes will be returned. Nodes will be returned in document-order,
     * @param path
     * @param node
     * @return
     */
    @SuppressWarnings("unchecked")
    public static List<Node> selectNodes(String path, Object node) {
        try {
            return new DOMXPath(path).selectNodes(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     * @param path
     * @param node
     * @return
     */
    public static Node selectNode(String path, Object node) {
        try {
            List<Node> nodes = selectNodes(path, node);
            if (nodes.size() == 0) {
                return null;
            }
            return nodes.get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the text of a node, or the value of an attribute
     * @param path the XPath to execute
     * @param node the node, node-set or Context object for evaluation. This value can be null.
     */
    public static String selectText(String path, Object node) {
        try {
            Node rnode = (Node) new DOMXPath(path).selectSingleNode(node);
            if (rnode == null) {
                return null;
            }
            if (!(rnode instanceof Text)) {
                rnode = rnode.getFirstChild();
            }
            if (!(rnode instanceof Text)) {
                return null;
            }
            return ((Text) rnode).getData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
