package play.templates;

import groovy.lang.Closure;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import play.Play;
import play.PlayPlugin;
import play.exceptions.TemplateCompilationException;
import play.templates.GroovyInlineTags.CALL;

/**
 * The template compiler
 */
public class GroovyTemplateCompiler extends TemplateCompiler {

    public static List<String> extensionsClassnames = new ArrayList<String>();

    @Override
    public BaseTemplate compile(BaseTemplate template) {
        try {
            extensionsClassnames.clear();
            for(PlayPlugin p : Play.plugins) {
                extensionsClassnames.addAll(p.addTemplateExtensions());
            }
            List<Class> extensionsClasses = Play.classloader.getAssignableClasses(JavaExtensions.class);
            for (Class extensionsClass : extensionsClasses) {
                extensionsClassnames.add(extensionsClass.getName());
            }
        } catch (Throwable e) {
            //
        }
        return super.compile(template);
    }

    @Override
    String source() {
        String source = template.source;

        // Static access
        List<String> names = new ArrayList<String>();
        Map<String, String> originalNames = new HashMap<String, String>();
        for (Class clazz : Play.classloader.getAllClasses()) {
            if (clazz.getName().endsWith("$")) {
                names.add(clazz.getName().substring(0, clazz.getName().length() - 1).replace("$", ".") + "$");
                originalNames.put(clazz.getName().substring(0, clazz.getName().length() - 1).replace("$", ".") + "$", clazz.getName());
            } else {
                names.add(clazz.getName().replace("$", "."));
                originalNames.put(clazz.getName().replace("$", "."), clazz.getName());
            }
        }
        Collections.sort(names, new Comparator<String>() {

            public int compare(String o1, String o2) {
                return o2.length() - o1.length();
            }
        });
        for (String cName : names) { // dynamic class binding
            source = source.replaceAll("new " + Pattern.quote(cName) + "(\\([^)]*\\))", "_('" + originalNames.get(cName) + "').newInstance$1");
            source = source.replaceAll("([a-zA-Z0-9.-_$]+)\\s+instanceof\\s+" + Pattern.quote(cName), "_('" + originalNames.get(cName).replace("$", "\\$") + "').isAssignableFrom($1.class)");
            source = source.replaceAll("([^.])" + Pattern.quote(cName) + ".class", "$1_('" + originalNames.get(cName) + "')");
            source = source.replaceAll("([^'\".])" + Pattern.quote(cName) + "([.][^'\"])", "$1_('" + originalNames.get(cName).replace("$", "\\$") + "')$2");
        }

        return source;
    }

    @Override
    void head() {
        print("class ");
        String className = "Template_" + ((template.name.hashCode() + "").replace("-", "M"));
        print(className);
        println(" extends play.templates.GroovyTemplate.ExecutableTemplate {");
        println("public Object run() { use(play.templates.JavaExtensions) {");
        for (String n : extensionsClassnames) {
            println("use(_('" + n + "')) {");
        }
    }

    @Override
    @SuppressWarnings("unused")
    void end() {
        for (String n : extensionsClassnames) {
            println(" } ");
        }
        println("} }");
        println("}");
    }

    @Override
    void plain() {
        String text = parser.getToken().replace("\\", "\\\\").replaceAll("\"", "\\\\\"").replace("$", "\\$");
        if (skipLineBreak && text.startsWith("\n")) {
            text = text.substring(1);
        }
        skipLineBreak = false;
        if (text.indexOf("\n") > -1) {
            String[] lines = text.split("\n", 10000);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.length() > 0 && line.charAt(line.length() - 1) == 13) {
                    line = line.substring(0, line.length() - 1);
                }

                if (i == lines.length - 1 && !text.endsWith("\n")) {
                    print("\tout.print(\"");
                } else if (i == lines.length - 1 && line.equals("")) {
                    continue;
                } else {
                    print("\tout.println(\"");
                }
                print(line);
                print("\");");

                markLine(parser.getLine() + i);
                println();
            }
        } else {
            print("\tout.print(\"");
            print(text);
            print("\");");
            markLine(parser.getLine());
            println();
        }
    }

    @Override
    void script() {
        String text = parser.getToken();
        if (text.indexOf("\n") > -1) {
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
    void expr() {
        String expr = parser.getToken().trim();
        print("\t__val=");
        print(expr);
        print(";out.print(__val!=null?__safe(__val):'')");
        markLine(parser.getLine());
        println();
    }

    @Override
    void message() {
        String expr = parser.getToken().trim();
        print(";out.print(messages.get(" + expr + "))");
        markLine(parser.getLine());
        println();
    }

    @Override
    void action(boolean absolute) {
        String action = parser.getToken().trim();
        if (action.trim().matches("^'.*'$")) {
            if (absolute) {
                print("\tout.print(play.mvc.Router.reverseWithCheck(" + action + ", play.Play.getVirtualFile(" + action + "), true));");
            } else {
                print("\tout.print(play.mvc.Router.reverseWithCheck(" + action + ", play.Play.getVirtualFile(" + action + "), false));");
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
    void startTag() {
        tagIndex++;
        String tagText = parser.getToken().trim().replaceAll("\n", " ");
        String tagName = "";
        String tagArgs = "";
        boolean hasBody = !parser.checkNext().endsWith("/");
        if (tagText.indexOf(" ") > 0) {
            tagName = tagText.substring(0, tagText.indexOf(" "));
            tagArgs = tagText.substring(tagText.indexOf(" ") + 1).trim();
            if (!tagArgs.matches("^[_a-zA-Z0-9]+\\s*:.*$")) {
                tagArgs = "arg:" + tagArgs;
            }
            tagArgs = tagArgs.replaceAll("[:]\\s*[@]{2}", ":actionBridge._abs().");
            tagArgs = tagArgs.replaceAll("(\\s)[@]{2}", "$1actionBridge._abs().");
            tagArgs = tagArgs.replaceAll("[:]\\s*[@]{1}", ":actionBridge.");
            tagArgs = tagArgs.replaceAll("(\\s)[@]{1}", "$1actionBridge.");
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
            print((String) m.invoke(null, new Object[]{tagIndex, CALL.START}));
            tag.hasBody = false;
            markLine(parser.getLine());
            println();
            skipLineBreak = true;
            return;
        } catch (Exception e) {
            // do nothing here
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
    void endTag() {
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
            print("if(attrs" + tagIndex + "['as']) { setProperty(attrs" + tagIndex + "['as'], toExecute.toString()); } else { out.print(toExecute.toString()); }; toUnset.each() {toExecute.setProperty(it, null)} };");
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
                println((String) m.invoke(null, new Object[]{tagIndex, CALL.END}));
                print("play.templates.TagContext.exitTag();");
            } catch (Exception e) {
                // Use fastTag if exists
                List<Class> fastClasses = new ArrayList<Class>();
                try {
                    fastClasses = Play.classloader.getAssignableClasses(FastTags.class);
                } catch (Exception xe) {
                    //
                }
                fastClasses.add(0, FastTags.class);
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
                    if (c.isAnnotationPresent(FastTags.Namespace.class) && !c.getAnnotation(FastTags.Namespace.class).value().equals(tSpace)) {
                        continue;
                    }
                    try {
                        m = c.getDeclaredMethod("_" + tName, Map.class, Closure.class, PrintWriter.class, GroovyTemplate.ExecutableTemplate.class, int.class);
                    } catch (NoSuchMethodException ex) {
                        continue;
                    }
                }
                if (m != null) {
                    print("play.templates.TagContext.enterTag('" + tag.name + "');");
                    print("_('" + m.getDeclaringClass().getName() + "')._" + tName + "(attrs" + tagIndex + ",body" + tagIndex + ", out, this, " + tag.startLine + ");");
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


