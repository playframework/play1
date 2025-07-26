package play.templates;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Tag Context (retrieve who call you)
 */
public class TagContext {
    
    private static final ThreadLocal<Deque<TagContext>> currentStack = new ThreadLocal<>();
    
    public final Map<String, Object> data = new HashMap<>();

    public final String tagName;

    public TagContext(String tagName) {
        this.tagName = tagName;
    }
    
    public static void init() {
        currentStack.set(new ArrayDeque<>());
        enterTag("ROOT");
    }
    
    public static void enterTag(String name) {
        currentStack.get().push(new TagContext(name));
    }
    
    public static void exitTag() {
        currentStack.get().pop();
    }
    
    public static TagContext current() {
        return currentStack.get().peek();
    }
    
    public static TagContext parent() {
        Iterator<TagContext> it = currentStack.get().iterator();
        if (it.hasNext()) {
            // skip
            it.next();

            if (it.hasNext()) {
                return it.next();
            }
        }

        return null;
    }
    
    public static boolean hasParentTag(String name) {
        for (TagContext current : currentStack.get()) {
            if (name.equals(current.tagName)) {
                return true;
            }
        }
        return false;
    }
    
    public static TagContext parent(String name) {
        Iterator<TagContext> it = currentStack.get().iterator();
        if (it.hasNext()) {
            // skip head
            it.next();

            while (it.hasNext()) {
                TagContext parent = it.next();
                if (name.equals(parent.tagName)) {
                    return parent;
                }
            }
        }
        return null;
    }

    public static TagContext parent(String... names) {
        Iterator<TagContext> it = currentStack.get().iterator();
        if (it.hasNext()) {
            // skip head
            it.next();

            while (it.hasNext()) {
                TagContext parent = it.next();
                for (String name : names) {
                    if (name.equals(parent.tagName)) {
                        return parent;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.tagName + this.data;
    }

}
