package play.data.binding;

import static org.junit.Assert.*; 

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class BinderTest {

    @Test
    public void testSimpleObject() {
        Map<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("document.id", p("42"));
        parameters.put("document.title", p("Titre du document"));
        parameters.put("document.body", p("Un document qui parle de..."));
        parameters.put("document.d", p("2.35"));

        Document doc = (Document) Binder.bind("document", Document.class, Document.class.getGenericSuperclass(), parameters);
        assertEquals(doc.id, 42);
        assertEquals(doc.getTitle(), "Titre du document");
        assertEquals(doc.body, "Un document qui parle de...");
        assertEquals(doc.d, 2.35f, 0);
    }

    @Test
    public void testComposite() {
        Map<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("folder.name", p("Un répertoire"));
        parameters.put("folder.id", p("24"));
        parameters.put("folder.document.id", p("42"));
        parameters.put("folder.document.title", p("Titre du document"));
        parameters.put("folder.document.body", p("Un document qui parle de..."));
        parameters.put("folder.document.d", p("2.35"));

        Folder folder = (Folder) Binder.bind("folder", Folder.class, Folder.class.getGenericSuperclass(), parameters);
        assertEquals(folder.document.id, 42);
        assertEquals(folder.document.getTitle(), "Titre du document");
        assertEquals(folder.document.body, "Un document qui parle de...");
        assertEquals(folder.document.d, 2.35f, 0);
    }

    public String[] p(String... params) {
        String[] result = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            result[i] = params[i];
        }
        return result;
    }
}
