package play.data.binding;

import play.utils.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParamNode {
    private final String name;
    private final Map<String, ParamNode> _children = new HashMap<>(8);
    private String[] values = null;
    private String originalKey;

    // splits a string on one-ore-more instances of .[]
    // this works so that all the following strings (param naming syntax)
    // is resolved into the same structural hierarchy:
    // a.b.c=12
    // a[b].c=12
    // a[b][c]=12
    // a.b[c]=12
    private static final String keyPartDelimiterRegexpString = "[\\.\\[\\]]+";

    public ParamNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String[] getValues() {
        return values;
    }

    public String getFirstValue(Class<?> type) {
        if (values == null) {
            return null;
        }

        if (values.length>1 && String.class.equals(type)) {
            // special handling for string - when multiple values, concatenate them with comma..
            return Utils.join(values, ", ");
        } else {
            return values[0];
        }
    }

    public void addChild( ParamNode child) {
        _children.put(child.name, child);
    }

    public ParamNode getChild(String name) {
        return getChild( name, false);
    }

    public ParamNode getChild(String name, boolean returnEmptyChildIfNotFound) {
        ParamNode child = getChild( name.split(keyPartDelimiterRegexpString));
        if (child == null && returnEmptyChildIfNotFound) {
            child = new ParamNode(name);
        }
        return child;
    }

    public static class RemovedNode {
        public final ParamNode removedFrom;
        public final ParamNode removedNode;

        public RemovedNode(ParamNode removedFrom, ParamNode removedNode) {
            this.removedFrom = removedFrom;
            this.removedNode = removedNode;
        }
    }

    /**
     * Removes a child from this node, but stores what is removed to list.
     * The we can later call which will add it back again.
     * This is a "hack" related to #1195 which makes it possible to reuse the RootParamsNode-structure
     * if you want to perform the bind-operation multiple times.
     *
     * @param name the name of the child-node in this paramNode which should be removed.
     * @param removedNodesList a list where info about what is removed where is stored.
     * @return true if anything was removed.
     */
    public boolean removeChild(String name, List<RemovedNode> removedNodesList) {
        ParamNode removedNode = _children.remove(name);
        if ( removedNode != null) {
            removedNodesList.add( new RemovedNode(this, removedNode));
            return true;
        } else {
            return false;
        }
    }

    public static void restoreRemovedChildren( List<RemovedNode> removedNodesList ) {
        for ( RemovedNode rn : removedNodesList) {
            rn.removedFrom._children.put( rn.removedNode.name, rn.removedNode);
        }
    }

    private ParamNode getChild(String[] nestedNames) {
        ParamNode currentChildNode = this;
        for (int i=0; i<nestedNames.length; i++) {
            currentChildNode = currentChildNode._children.get(nestedNames[i]);
            if (currentChildNode == null) {
                return null;
            }
        }
        return currentChildNode;
    }

    public Collection<ParamNode> getAllChildren() {
        return _children.values();
    }

    public Set<String> getAllChildrenKeys() {
        return _children.keySet();
    }

    public void setValue(String[] value, String originalKey) {
        this.values = value;
        this.originalKey = originalKey;
    }

    public String getOriginalKey() {
        if (originalKey==null) {
            return name;
        }
        return originalKey;
    }

    public static RootParamNode convert(Map<String, String[]> params) {
        RootParamNode root = new RootParamNode( params);

        for (Map.Entry<String, String[]> e : params.entrySet()) {
            String key = e.getKey();
            String[] values = e.getValue();
            if (values != null && values.length == 0) {
                values = null;
            }

            ParamNode currentParent = root;

            for ( String name : key.split(keyPartDelimiterRegexpString)) {
                ParamNode paramNode = currentParent.getChild( name );
                if (paramNode ==null) {
                    // first time we see this node - create it and add it to parent
                    paramNode = new ParamNode(name);
                    currentParent.addChild(paramNode);
                }
                currentParent = paramNode;
            }

            // currentParent is now the last node where we should place the values
            currentParent.setValue( values, key);

        }
        return root;
    }
}