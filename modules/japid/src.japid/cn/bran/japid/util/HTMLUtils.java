package cn.bran.japid.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/*
 * Copyright 2002-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Represents a set of character entity references defined by the
 * HTML 4.0 standard.
 *
 * <p>A complete description of the HTML 4.0 character set can be found
 * at http://www.w3.org/TR/html4/charset.html.
 *
 * @author Juergen Hoeller
 * @author Martin Kersten
 * @since 1.2.1
 */
public class HTMLUtils {

    /*
     * Shared instance of pre-parsed HTML character entity references.
     */
    private static final HtmlCharacterEntityReferences characterEntityReferences = new HtmlCharacterEntityReferences();

    /**
     * Turn special characters into HTML character references.
     * Handles complete character set defined in HTML 4.01 recommendation.
     * <p>Escapes all special characters to their corresponding
     * entity reference (e.g. <code>&lt;</code>).
     * <p>Reference:
     * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
     * http://www.w3.org/TR/html4/sgml/entities.html
     * </a>
     * @param input the (unescaped) input string
     * @return the escaped string
     */
    public static String htmlEscape(String input) {
        if (input == null) {
            return null;
        }
        StringBuffer escaped = new StringBuffer(input.length() * 2);
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            String reference = characterEntityReferences.convertToReference(character);
            if (reference != null) {
                escaped.append(reference);
            } else {
                escaped.append(character);
            }
        }
        return escaped.toString();
    }

    public static class HtmlCharacterEntityReferences {

        static final char REFERENCE_START = '&';
        static final String DECIMAL_REFERENCE_START = "&#";
        static final String HEX_REFERENCE_START = "&#x";
        static final char REFERENCE_END = ';';
        static final char CHAR_NULL = (char) -1;
        private static final String PROPERTIES_FILE = "htmlentities.properties";
        private final String[] characterToEntityReferenceMap = new String[3000];
        private final Map<String, Character> entityReferenceToCharacterMap = new HashMap<String, Character>(252);

        /**
         * Returns a new set of character entity references reflecting the HTML 4.0 character set.
         */
        public HtmlCharacterEntityReferences() {
            Properties entityReferences = new Properties();

            // Load reference definition file.
            InputStream is = HtmlCharacterEntityReferences.class.getResourceAsStream(PROPERTIES_FILE);
            if (is == null) {
                throw new IllegalStateException(
                        "Cannot find reference definition file [htmlentities.properties] as class path resource");
            }
            try {
                try {
                    entityReferences.load(is);
                } finally {
                    is.close();
                }
            } catch (IOException ex) {
                throw new IllegalStateException(
                        "Failed to parse reference definition file [HtmlCharacterEntityReferences.properties]: " + ex.getMessage());
            }

            // Parse reference definition properties.
            Enumeration<?> keys = entityReferences.propertyNames();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                int referredChar = Integer.parseInt(key);
                int index = (referredChar < 1000 ? referredChar : referredChar - 7000);
                String reference = entityReferences.getProperty(key);
                this.characterToEntityReferenceMap[index] = REFERENCE_START + reference + REFERENCE_END;
                this.entityReferenceToCharacterMap.put(reference, new Character((char) referredChar));
            }
        }

        /**
         * Return the number of supported entity references.
         */
        public int getSupportedReferenceCount() {
            return this.entityReferenceToCharacterMap.size();
        }

        /**
         * Return true if the given character is mapped to a supported entity reference.
         */
        public boolean isMappedToReference(char character) {
            return (convertToReference(character) != null);
        }

        /**
         * Return the reference mapped to the given character or <code>null</code>.
         */
        public String convertToReference(char character) {
            if (character < 1000 || (character >= 8000 && character < 10000)) {
                int index = (character < 1000 ? character : character - 7000);
                String entityReference = this.characterToEntityReferenceMap[index];
                if (entityReference != null) {
                    return entityReference;
                }
            }
            return null;
        }

        /**
         * Return the char mapped to the given entityReference or -1.
         */
        public char convertToCharacter(String entityReference) {
            Character referredCharacter = this.entityReferenceToCharacterMap.get(entityReference);
            if (referredCharacter != null) {
                return referredCharacter.charValue();
            }
            return CHAR_NULL;
        } 
    }
}
