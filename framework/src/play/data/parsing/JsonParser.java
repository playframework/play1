/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package play.data.parsing;

import fr.zenexity.json.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import fr.zenexity.json.JSONTokenizer.Token;
import static fr.zenexity.json.JSONTokenizer.Token.*;


/**
 *
 * @author lwe
 */
public class JsonParser extends DataParser {

    private JSONTokenizer jt;
    
    public Map<String, String[]> parse(InputStream body) {
        try {
            return parse( new InputStreamReader(body, "utf-8"));
        } catch( UnsupportedEncodingException ex ) {
            throw new RuntimeException(ex);
        }
    }

    public Map<String, String[]> parse( Reader reader ) {
        Map<String, String[]> result = new HashMap<String, String[]>();
        jt = new JSONTokenizer( reader );
        internalParse("", result );
        return result;
    }
    
    //// 

    private void internalParse( String name, Map<String, String[]> result ) {
        String nextname;
        Token tok = jt.nextToken();
        // primitive field found
        if( tok  == STRING || tok == NULL || tok == NUMBER || tok == BOOLEAN ) {
            putMapEntry( result, name, tok.value );
        }
        
        else if( tok  == START_ARRAY ) {
        
            int idx=0;
            for(;;) {
                nextname = name+"["+idx+"]";
                internalParse( nextname, result);
                tok = jt.nextToken();
                idx ++;
                if( tok == END_ARRAY )
                    break;
                if( tok != COMMA )
                    throw JsonParseException.grammarError( jt, COMMA );
            }   
        }
                
        else if( tok  == START_MAP ) {
            // hack for first level map
            if( ! name.equals(""))
                name += ".";
            
            for(;;) {
                
                tok = jt.expect( STRING );
                String key = tok.value;
                jt.expect(SEMICOLON);
                nextname = name+key;
                internalParse( nextname, result);
                tok = jt.nextToken();
                if( tok == END_MAP )
                    break;
                if( tok != COMMA )
                    throw JsonParseException.grammarError( jt, COMMA );

            }
        } 
        
        else {
           throw JsonParseException.parseError( "unexpected bytes found "+tok, jt );
        }
    }
    
}
