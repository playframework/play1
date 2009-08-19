package play.data.parsing;

import fr.zenexity.json.JSONException;
import fr.zenexity.json.JSONTokenizer;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import fr.zenexity.json.JSONTokenizer.Token;
import static fr.zenexity.json.JSONTokenizer.Token.*;

/**
 * JSON Parser
 */
public class JsonParser extends DataParser {

    private JSONTokenizer jt;

    public Map<String, String[]> parse(InputStream body) {
        try {
            return parse(new InputStreamReader(body, "utf-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Map<String, String[]> parse(Reader reader) {
        Map<String, String[]> result = new HashMap<String, String[]>();
        jt = new JSONTokenizer(reader);
        internalParse("", result, jt.nextToken());
        return result;
    }

    private void internalParse(String name, Map<String, String[]> result, Token tok) {
        String nextname = null;
        
        // primitives type
        if (tok == STRING || tok == NULL || tok == NUMBER || tok == BOOLEAN) {
            putMapEntry(result, name, tok.value);
            
        // arrays
        } else if (tok == START_ARRAY) {
            tok = jt.nextToken();
            // empty arrays need a special treatment - stored as "arrayname[]": null in the result map
            if (tok == END_ARRAY) {
                putMapEntry(result, name + "[]", null);
                return;
            }
            int idx = 0;
            for (;;) {
                nextname = name + "[" + idx + "]";
                internalParse(nextname, result, tok);
                tok = jt.nextToken();
                idx++;
                if (tok == END_ARRAY) {
                    break;
                }
                if (tok != COMMA) {
                    throw JSONException.grammarError(jt, COMMA);
                }
                tok = jt.nextToken();
            }
        // maps / objects
        } else if (tok == START_MAP) {
            tok = jt.nextToken();
            // empty map need a special treatment
            if (tok == END_MAP) {
                putMapEntry(result, name, null);
                return; // right ?
            }
            // hack for first level map
            if (!"".equals(name)) {
                name += ".";
            }
            for (;;) {
                if (tok != STRING) {
                    throw JSONException.grammarError(jt, STRING);
                }
                String key = tok.value;
                jt.expect(SEMICOLON);
                nextname = name + key;
                internalParse(nextname, result, jt.nextToken());
                tok = jt.nextToken();
                if (tok == END_MAP) {
                    break;
                }
                if (tok != COMMA) {
                    throw JSONException.grammarError(jt, COMMA);
                }
                // next map key
                tok = jt.nextToken();
            }
        } else {
            throw JSONException.parseError("unexpected bytes found " + tok, jt);
        }
    }
}
