/**
 *
 * Copyright 2010, Nicolas Leroux.
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * User: ${user}
 * Date: ${date}
 *
 */
package play.libs;

import play.Play;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Expression {

    static Pattern expression = Pattern.compile("^\\$\\{(.*)\\}$");

    public static Object evaluate(String value, String defaultValue) {
        Matcher matcher = expression.matcher(value);
        if (matcher.matches()) {
            return Play.configuration.getProperty(matcher.group(1), defaultValue);
        }
        return value;
    }
}
