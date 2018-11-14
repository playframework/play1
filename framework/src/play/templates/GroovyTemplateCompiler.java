package play.templates;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import groovy.lang.Closure;
import play.Logger;
import play.Play;
import play.exceptions.TemplateCompilationException;
import play.templates.GroovyInlineTags.CALL;

public class GroovyTemplateCompiler extends TemplateCompiler {

    protected List<String> extensionsClassnames = new ArrayList<>();

    // [#714] The groovy-compiler complaints if a line is more than 65535 unicode units long..
    // Have to split it if it is really that big
    protected static final int maxPlainTextLength = 60000;

    @Override
    public BaseTemplate compile(BaseTemplate template) {
        try {
            extensionsClassnames.clear();
            extensionsClassnames.addAll(Play.pluginCollection.addTemplateExtensions());
            List<Class> extensionsClasses = Play.classloader.getAssignableClasses(JavaExtensions.class);
            for (Class extensionsClass : extensionsClasses) {
                extensionsClassnames.add(extensionsClass.getName());
            }
        } catch (Throwable e) {
            Logger.error(e, "Failed to compile template %s", template.getName());
        }
        return super.compile(template);
    }

    @Override
    protected String source() {
        String source = template.source;

        // If a plugin has something to change in the template before the compilation
        source = Play.pluginCollection.overrideTemplateSource(template, source);

        if (Boolean.parseBoolean(Play.configuration.getProperty("groovy.template.check.scala.comptatibility", "false"))) {
            source = this.checkScalaCompatibility(source);
        }

        return source;
    }

    @Deprecated
    protected String checkScalaComptability(String source) {
        return checkScalaCompatibility(source);
    }

    /**
     * Makes the code scala compatible (for the scala module).
     * 
     * @param source
     *            The string representation of the code
     * @return The scala compatible source
     */
    protected String checkScalaCompatibility(String source) {
        // Static access
        List<String> names = new ArrayList<>();
        Map<String, String> originalNames = new HashMap<>();
        for (Class clazz : Play.classloader.getAllClasses()) {
            if (clazz.getName().endsWith("$")) {
                String name = clazz.getName().substring(0, clazz.getName().length() - 1).replace('$', '.') + '$';
                names.add(name);
                originalNames.put(name, clazz.getName());
            } else {
                String name = clazz.getName().replace('$', '.');
                names.add(name);
                originalNames.put(name, clazz.getName());
            }
        }
        Collections.sort(names, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o2.length() - o1.length();
            }
        });

        // We're about to do many many String.replaceAll() so we do some
        // checking first
        // to try to reduce the number of needed replaceAll-calls.
        // Morten: I have tried to create a single regexp that can be used
        // instead of all the replaceAll,
        // but I failed to do so.. Such a single regexp would be much faster
        // since
        // we then we only would have to have one pass.

        if (!names.isEmpty()) {

            if (names.size() <= 1 || source.contains("new ")) {
                for (String cName : names) { // dynamic class binding
                    source = source.replaceAll("new " + Pattern.quote(cName) + "(\\([^)]*\\))",
                            "__loadClass('" + originalNames.get(cName).replace("$", "\\$") + "').newInstance$1");
                }
            }

            if (names.size() <= 1 || source.contains("instanceof")) {
                for (String cName : names) { // dynamic class binding
                    source = source.replaceAll("([a-zA-Z0-9.-_$]+)\\s+instanceof\\s+" + Pattern.quote(cName),
                            "__loadClass('" + originalNames.get(cName).replace("$", "\\$") + "').isAssignableFrom($1.class)");

                }
            }

            if (names.size() <= 1 || source.contains(".class")) {
                for (String cName : names) { // dynamic class binding
                    source = source.replaceAll("([^.])" + Pattern.quote(cName) + ".class",
                            "$1_('" + originalNames.get(cName).replace("$", "\\$") + "')");

                }
            }

            // With the current arg0 in replaceAll, it is not possible to do a
            // quick indexOf-check for this one,
            // so we have to run all the replaceAll-calls
            for (String cName : names) { // dynamic class binding
                source = source.replaceAll("([^'\".])" + Pattern.quote(cName) + "([.][^'\"])",
                        "$1_('" + originalNames.get(cName).replace("$", "\\$") + "')$2");

            }

        }
        return source;
    }

    @Override
    protected void head() {
        print("class ");
        // This generated classname is parsed when creating cleanStackTrace.
        // The part after "Template_" is used as key when
        // looking up the file on disk this template-class is generated from.
        // cleanStackTrace is looking in TemplateLoader.templates

        String uniqueNumberForTemplateFile = TemplateLoader.getUniqueNumberForTemplateFile(template.name);

        String className = "Template_" + uniqueNumberForTemplateFile;
        print(className);
        println(" extends play.templates.GroovyTemplate.ExecutableTemplate {");
        println("public Object run() { use(play.templates.JavaExtensions) {");
        for (String n : extensionsClassnames) {
            println("use(__loadClass('" + n + "')) {");
        }
    }

    @Override
    @SuppressWarnings("unused")
    protected void end() {
        for (String n : extensionsClassnames) {
            println(" } ");
        }
        println("} }");
        println("}");
    }

    /**
     * Interesting performance observation: Calling print(); from java (in ExecutableTemplate) called from groovy is
     * MUCH slower than java returning string to groovy which then prints with out.print();
     */

    @Override
    protected void plain() {
        String text = parser.getToken().replace("\\", "\\\\").replaceAll("\"", "\\\\\"").replace("$", "\\$");
        if (skipLineBreak && text.startsWith("\n")) {
            text = text.substring(1);
        }
        skipLineBreak = false;
        text = text.replaceAll("\r\n", "\n").replaceAll("\n", "\\\\n");
        // we don't have to print line numbers here since this cannot fail - it is only text printing

        // [#714] The groovy-compiler complaints if a line is more than 65535 unicode units long..
        // Have to split it if it is really that big
        if (text.length() < maxPlainTextLength) {
            // text is "short" - just print it
            println("out.print(\"" + text + "\");");
        } else {
            // text is long - must split it
            int offset = 0;
            do {
                int endPos = offset + maxPlainTextLength;
                if (endPos > text.length()) {
                    endPos = text.length();
                } else {
                    // #869 If the last char (at endPos-1) is \, we're dealing with escaped char - must include the next
                    // one also..
                    if (text.charAt(endPos - 1) == '\\') {
                        // use one more char so the escaping is not broken. Don't have to check length, since
                        // all '\' is used in escaping, ref replaceAll above..
                        endPos++;
                    }
                }
                println("out.print(\"" + text.substring(offset, endPos) + "\");");
                offset += (endPos - offset);
            } while (offset < text.length());
        }
    }

    @Override
    protected void script() {
        String text = parser.getToken();
        if (text.contains("\n")) {
            String[] lines = parser.getToken().split("\n");
            for (int i = 0; i < lines.length; i++) {
                print(lines[i]);
                markLine(parser.getLine() + i);
                println();
            }
        } else {
            print(text);
            markLine(parser.getLine());
            println();
        }
        skipLineBreak = true;
    }

    @Override
    protected void expr() {
        String expr = parser.getToken().trim();
        print(";out.print(__safeFaster(" + expr + "))");
        markLine(parser.getLine());
        println();
    }

    @Override
    protected void message() {
        String expr = parser.getToken().trim();
        print(";out.print(__getMessage(" + expr + "))");
        markLine(parser.getLine());
        println();
    }

    @Override
    protected void action(boolean absolute) {
        String action = parser.getToken().trim();
        if (action.trim().matches("^'.*'$")) {
            if (absolute) {
                print("\tout.print(__reverseWithCheck_absolute_true(" + action + "));");
            } else {
                print("\tout.print(__reverseWithCheck_absolute_false(" + action + "));");
            }
        } else {
            if (!action.endsWith(")")) {
                action = action + "()";
            }
            if (absolute) {
                print("\tout.print(actionBridge._abs()." + action + ");");
            } else {
                print("\tout.print(actionBridge." + action + ");");
            }
        }
        markLine(parser.getLine());
        println();
    }

    @Override
    protected void startTag() {
        tagIndex++;
        String tagText = parser.getToken().trim().replaceAll("\r", "").replaceAll("\n", " ");
        String tagName;
        String tagArgs;
        boolean hasBody = !parser.checkNext().endsWith("/");
        if (tagText.indexOf(" ") > 0) {
            tagName = tagText.substring(0, tagText.indexOf(" "));
            tagArgs = tagText.substring(tagText.indexOf(" ") + 1).trim();
            if (!tagArgs.matches("^[_a-zA-Z0-9]+\\s*:.*$")) {
                tagArgs = "arg:" + tagArgs;
            }
            // We only have to try to replace the following if we find at least one
            // @ in tagArgs..
            if (tagArgs.indexOf('@') >= 0) {
                tagArgs = tagArgs.replaceAll("[:]\\s*[@]{2}", ":actionBridge._abs().");
                tagArgs = tagArgs.replaceAll("(\\s)[@]{2}", "$1actionBridge._abs().");
                tagArgs = tagArgs.replaceAll("[:]\\s*[@]{1}", ":actionBridge.");
                tagArgs = tagArgs.replaceAll("(\\s)[@]{1}", "$1actionBridge.");
            }
        } else {
            tagName = tagText;
            tagArgs = ":";
        }
        Tag tag = new Tag();
        tag.name = tagName;
        tag.startLine = parser.getLine();
        tag.hasBody = hasBody;
        tagsStack.push(tag);
        if (tagArgs.trim().equals("_:_")) {
            print("attrs" + tagIndex + " = _attrs;");
        } else {
            print("attrs" + tagIndex + " = [" + tagArgs + "];");
        }
        // Use inlineTag if exists
        try {
            Method m = GroovyInlineTags.class.getDeclaredMethod("_" + tag.name, int.class, CALL.class);
            print("play.templates.TagContext.enterTag('" + tag.name + "');");
            print((String) m.invoke(null, tagIndex, CALL.START));
            tag.hasBody = false;
            markLine(parser.getLine());
            println();
            skipLineBreak = true;
            return;
        } catch (NoSuchMethodException e) {
            // We find a tag that is not defined in GroovyInlineTags, lets see if it is defined somewhere else
        } catch (Exception e) {
            Logger.debug(e, "Failed to start tag %s in template %s", tag.name, template.getName());
        }
        if (!tag.name.equals("doBody") && hasBody) {
            print("body" + tagIndex + " = {");
            markLine(parser.getLine());
            println();
        } else {
            print("body" + tagIndex + " = null;");
            markLine(parser.getLine());
            println();
        }
        skipLineBreak = true;
    }

    @Override
    protected void endTag() {
        String tagName = parser.getToken().trim();
        if (tagsStack.isEmpty()) {
            throw new TemplateCompilationException(template, parser.getLine(), "#{/" + tagName + "} is not opened.");
        }
        Tag tag = tagsStack.pop();
        String lastInStack = tag.name;
        if (tagName.equals("")) {
            tagName = lastInStack;
        }
        if (!lastInStack.equals(tagName)) {
            throw new TemplateCompilationException(template, tag.startLine, "#{" + tag.name + "} is not closed.");
        }
        if (tag.name.equals("doBody")) {
            print("if(_body || attrs" + tagIndex + "['body']) {");
            print("def toExecute = attrs" + tagIndex + "['body'] ?: _body; toUnset = []; if(attrs" + tagIndex + "['vars']) {");
            print("attrs" + tagIndex + "['vars'].each() {");
            print("if(toExecute.getProperty(it.key) == null) {toUnset.add(it.key);}; toExecute.setProperty(it.key, it.value);");
            print("}};");
            print("if(attrs" + tagIndex + "['as']) { setProperty(attrs" + tagIndex
                    + "['as'], toExecute.toString()); } else { out.print(toExecute.toString()); }; toUnset.each() {toExecute.setProperty(it, null)} };");
            markLine(tag.startLine);
            template.doBodyLines.add(currentLine);
            println();
        } else {
            if (tag.hasBody) {
                print("};"); // close body closure
            }
            println();
            // Use inlineTag if exists
            try {
                Method m = GroovyInlineTags.class.getDeclaredMethod("_" + tag.name, int.class, CALL.class);
                println((String) m.invoke(null, tagIndex, CALL.END));
                print("play.templates.TagContext.exitTag();");
            } catch (Exception e) {
                // Use fastTag if exists
                List<Class> fastClasses = new ArrayList<>();
                fastClasses.addAll(Play.classloader.getAssignableClasses(FastTags.class));
                fastClasses.add(FastTags.class);

                Method m = null;
                String tName = tag.name;
                String tSpace = "";
                if (tName.indexOf(".") > 0) {
                    tSpace = tName.substring(0, tName.lastIndexOf("."));
                    tName = tName.substring(tName.lastIndexOf(".") + 1);
                }
                for (Class<?> c : fastClasses) {
                    if (!c.isAnnotationPresent(FastTags.Namespace.class) && tSpace.length() > 0) {
                        continue;
                    }
                    if (c.isAnnotationPresent(FastTags.Namespace.class)
                            && !c.getAnnotation(FastTags.Namespace.class).value().equals(tSpace)) {
                        continue;
                    }
                    try {
                        m = c.getDeclaredMethod("_" + tName, Map.class, Closure.class, PrintWriter.class,
                                GroovyTemplate.ExecutableTemplate.class, int.class);
                        break;
                    } catch (NoSuchMethodException ex) {
                        // continue looking for this method in other *FastTags implementations
                    }
                }
                if (m != null) {
                    print("play.templates.TagContext.enterTag('" + tag.name + "');");
                    print("__loadClass('" + m.getDeclaringClass().getName() + "')._" + tName + "(attrs" + tagIndex + ",body" + tagIndex
                            + ", out, this, " + tag.startLine + ");");
                    print("play.templates.TagContext.exitTag();");
                } else {
                    print("invokeTag(" + tag.startLine + ",'" + tagName + "',attrs" + tagIndex + ",body" + tagIndex + ");");
                }
            }
            markLine(tag.startLine);
            println();
        }
        tagIndex--;
        skipLineBreak = true;
    }
}
