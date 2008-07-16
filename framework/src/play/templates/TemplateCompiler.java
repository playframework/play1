package play.templates;

import groovy.lang.Closure;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Stack;
import play.Logger;
import play.vfs.VirtualFile;
import play.exceptions.TemplateCompilationException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;

public class TemplateCompiler {

    public static Template compile(VirtualFile file) {
        try {
            String source = file.contentAsString();
            String name = file.relativePath();
            Template template = new Template(name, source);
            long start = System.currentTimeMillis();
            new Compiler().hop(template);
            Logger.trace("%sms to parse template %s", System.currentTimeMillis()-start, file.relativePath());
            return template;
        } catch (PlayException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public static class Compiler {

        StringBuilder groovySource = new StringBuilder();
        Template template;
        Parser parser;
        boolean doNextScan = true;
        Parser.Token state;
        Stack<Tag> tagsStack = new Stack<Tag>();
        int tagIndex;
        boolean skipLineBreak;
        
        static class Tag {

            String name;
            int startLine;
        }

        void hop(Template template) {
            this.template = template;
            this.parser = new Parser(template.source);

            // Class header
            print("class ");
            String className = "Template_" + template.name.replaceAll("/", "_").replaceAll("\\.", "_").replaceAll("-", "_");
            print(className);
            println(" extends play.templates.Template.ExecutableTemplate {");
            println("public Object run() { use(play.templates.JavaExtensions) {");

            // Parse
            loop:
            for (;;) {

                if (doNextScan) {
                    state = parser.nextToken();
                } else {
                    doNextScan = true;
                }

                switch (state) {
                    case EOF:
                        break loop;
                    case PLAIN:
                        plain();
                        break;
                    case SCRIPT:
                        script();
                        break;
                    case EXPR:
                        expr();
                        break;
                    case MESSAGE:
                        message();
                        break;
                    case ACTION:
                        action(false);
                        break;
                    case ABS_ACTION:
                        action(true);
                        break;
                    case COMMENT:
                        skipLineBreak = true;
                        break;
                    case START_TAG:
                        startTag();
                        break;
                    case END_TAG:
                        endTag();
                        break;
                }
            }

            println("} }");
            println("}");
            
            if(!tagsStack.empty()) {
                Tag tag = tagsStack.peek();
                throw new TemplateCompilationException(template, tag.startLine, "#{" + tag.name + "} is not closed.");
            }

            // Done !            
            template.groovySource = groovySource.toString();
        }

        void plain() {
            String text = parser.getToken().replace("\\", "\\\\").replaceAll("\"", "\\\\\"").replace("$", "\\$");
            if(skipLineBreak && text.startsWith("\n")) {
                text = text.substring(1);
            }
            skipLineBreak = false;
            if (text.indexOf("\n") > -1) {
                String[] lines = text.split("\n", 10000);
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if(line.length() > 0 && (int)line.charAt(line.length()-1) == 13)  {
                        line = line.substring(0, line.length()-1);
                    }
                    if (i == lines.length - 1 && !text.endsWith("\n")) {
                        print("\tout.print(\"");
                    } else if(i == lines.length - 1 && line.equals("")) {
                        continue;
                    } else {
                        print("\tout.println(\"");
                    }
                    print(line);
                    print("\")");
                    markLine(parser.getLine() + i);
                    println();
                }
            } else {
                print("\tout.print(\"");
                print(text);
                print("\")");
                markLine(parser.getLine());
                println();
            }
        }

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

        void expr() {
            String expr = parser.getToken().trim();
            print("\tval=");
            print(expr);
            print(";out.print(val!=null?val:'')");
            markLine(parser.getLine());
            println();
        }

        void message() {
            String expr = parser.getToken().trim();
            print(";out.print(messages.get(" + expr + "))");
            markLine(parser.getLine());
            println();
        }

        void action(boolean absolute) {
            String action = parser.getToken().trim();
            if (!action.endsWith(")")) {
                action = action + "()";
            }
            if(absolute) {
                print("\tout.print(request.getBase() + actionBridge." + action + ");");
            } else {
                print("\tout.print(actionBridge." + action + ");");
            }
            markLine(parser.getLine());
            println();
        }

        void startTag() {
            tagIndex++;
            String tagText = parser.getToken().trim();
            String tagName = "";
            String tagArgs = "";
            if (tagText.indexOf(" ") > 0) {
                tagName = tagText.substring(0, tagText.indexOf(" "));
                tagArgs = tagText.substring(tagText.indexOf(" ") + 1).trim();
                if (!tagArgs.matches("^[a-zA-Z0-9]+:.*$")) {
                    tagArgs = "arg:" + tagArgs;
                }
            } else {
                tagName = tagText;
                tagArgs = ":";
            }
            Tag tag = new Tag();
            tag.name = tagName;
            tag.startLine = parser.getLine();
            tagsStack.push(tag);
            print("attrs" + tagIndex + " = [" + tagArgs + "];");
            if (!tag.name.equals("doBody")) {
                print("body" + tagIndex + " = {");
                markLine(parser.getLine());
                println();
            }
            skipLineBreak = true;
            
        }

        void endTag() {
            String tagName = parser.getToken().trim();
            if (tagsStack.isEmpty()) {
                throw new TemplateCompilationException(template, currentLine, "#{/" + tagName + "} is not opened.");
            }
            Tag tag = (Tag) tagsStack.pop();
            String lastInStack = tag.name;
            if (tagName.equals("")) {
                tagName = lastInStack;
            }
            if (!lastInStack.equals(tagName)) {
                throw new TemplateCompilationException(template, tag.startLine, "#{" + tag.name + "} is not closed.");
            }
            if (tag.name.equals("doBody")) {
                print("if(_body) {");
                print("if(attrs" + tagIndex + "['vars']) {");
                print("attrs" + tagIndex + "['vars'].each() {");
                print("_body.setProperty(it.key, it.value);");
                print("}};");
                print("_body.call() };");
                markLine(tag.startLine);
                template.doBodyLines.add(currentLine);
                println();
            } else {
                print("}");
                markLine(currentLine);
                println();
                // Use fastTag if exists
                try {
                    FastTags.class.getDeclaredMethod("_" + tag.name, Map.class, Closure.class, PrintWriter.class, Template.ExecutableTemplate.class, int.class);
                    print("play.templates.FastTags._" + tag.name + "(attrs" + tagIndex + ",body" + tagIndex + ", out, this, " + tag.startLine + ")");
                } catch(NoSuchMethodException e) {
                    print("invokeTag(" + tag.startLine + ",'" + tagName + "',attrs" + tagIndex + ",body" + tagIndex + ")");
                }
                markLine(tag.startLine);
                println();
            }
            tagIndex--;
            skipLineBreak = true;
        }  
        
        // Writer
        int currentLine = 1;

        void markLine(int line) {
            groovySource.append("// ligne " + line);
            template.linesMatrix.put(currentLine, line);
        }

        //
        void println() {
            groovySource.append("\n");
            currentLine++;
        }

        void print(String text) {
            groovySource.append(text);
        }

        void println(String text) {
            groovySource.append(text);
            println();
        }
    }

    public static class Parser {

        private String pageSource;

        public Parser(String pageSource) {
            this.pageSource = pageSource;
            this.len = pageSource.length();
        }

        //
        public enum Token {

            EOF, //
            PLAIN, //
            SCRIPT, // %{...}% or {%...%}
            EXPR, // ${...}
            START_TAG, // #{...}
            END_TAG, // #{/...}
            MESSAGE, // &{...}
            ACTION, // @{...}
            ABS_ACTION, // @@{...}
            COMMENT, // *{...}*
        }
        private int end,  begin,  end2,  begin2,  len;
        private Token state = Token.PLAIN;

        private Token found(Token newState, int skip) {
            begin2 = begin;
            end2 = --end;
            begin = end += skip;
            Token lastState = state;
            state = newState;
            return lastState;
        }

        public Integer getLine() {
            String token = pageSource.substring(0, begin2);
            if (token.indexOf("\n") == -1) {
                return 1;
            } else {
                return token.split("\n").length;
            }
        }

        public String getToken() {
            return pageSource.substring(begin2, end2);
        }

        public Token nextToken() {
            for (;;) {

                int left = len - end;
                if (left == 0) {
                    end++;
                    return found(Token.EOF, 0);
                }

                char c = pageSource.charAt(end++);
                char c1 = left > 1 ? pageSource.charAt(end) : 0;
                char c2 = left > 2 ? pageSource.charAt(end + 1) : 0;

                switch (state) {
                    case PLAIN:
                        if (c == '%' && c1 == '{') {
                            return found(Token.SCRIPT, 2);
                        }
                        if (c == '{' && c1 == '%') {
                            return found(Token.SCRIPT, 2);
                        }
                        if (c == '$' && c1 == '{') {
                            return found(Token.EXPR, 2);
                        }
                        if (c == '#' && c1 == '{' && c2 == '/') {
                            return found(Token.END_TAG, 3);
                        }
                        if (c == '#' && c1 == '{') {
                            return found(Token.START_TAG, 2);
                        }
                        if (c == '&' && c1 == '{') {
                            return found(Token.MESSAGE, 2);
                        }
                        if (c == '@' && c1 == '@' && c2 == '{') {
                            return found(Token.ABS_ACTION, 3);
                        }
                        if (c == '@' && c1 == '{') {
                            return found(Token.ACTION, 2);
                        }
                        if (c == '*' && c1 == '{') {
                            return found(Token.COMMENT, 2);
                        }
                        break;
                    case SCRIPT:
                        if (c == '}' && c1 == '%') {
                            return found(Token.PLAIN, 2);
                        }
                        if (c == '%' && c1 == '}') {
                            return found(Token.PLAIN, 2);
                        }
                        break;
                    case COMMENT:
                        if (c == '}' && c1 == '*') {
                            return found(Token.PLAIN, 2);
                        }
                        break;
                    case START_TAG:
                        if (c == '}') {
                            return found(Token.PLAIN, 1);
                        }
                        if (c == '/' && c1 == '}') {
                            return found(Token.END_TAG, 1);
                        }
                        break;
                    case END_TAG:
                        if (c == '}') {
                            return found(Token.PLAIN, 1);
                        }
                        break;
                    case EXPR:
                        if (c == '}') {
                            return found(Token.PLAIN, 1);
                        }
                        break;
                    case ACTION:
                        if (c == '}') {
                            return found(Token.PLAIN, 1);
                        }
                        break;
                    case ABS_ACTION:
                        if (c == '}') {
                            return found(Token.PLAIN, 1);
                        }
                        break;
                    case MESSAGE:
                        if (c == '}') {
                            return found(Token.PLAIN, 1);
                        }
                        break;
                }
            }
        }

        void reset() {
            end = begin = end2 = begin2 = 0;
            state = Token.PLAIN;
        }
    }
}
