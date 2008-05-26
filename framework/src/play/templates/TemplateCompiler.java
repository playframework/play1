package play.templates;

import java.util.Stack;
import play.Play.VirtualFile;

public class TemplateCompiler {

    public static Template compile(VirtualFile file) {
        try {
            String source = file.contentAsString();
            String name = file.relativePath();
            Template template = new Template(name, source);
            new Compiler().hop(template);
            return template;
        } catch (Exception e) {
            throw new RuntimeException(e);
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

        static class Tag {

            String name;
            int startLine;
        }

        void hop(Template template) {
            this.template = template;
            this.parser = new Parser(template.source);

            // Class header
            print("class ");
            String className = "Template_" + template.name.replaceAll("/", "_").replaceAll("\\.", "_");
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
                        action();
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

            // Done !            
            template.groovySource = groovySource.toString();
        }

        void plain() {
            String text = parser.getToken().replaceAll("\"", "\\\\\"").replace("$", "\\$");
            if (text.indexOf("\n") > -1) {
                String[] lines = text.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    if (i == lines.length - 1 && !text.endsWith("\n")) {
                        print("\tout.print(\"");
                    } else {
                        print("\tout.println(\"");
                    }
                    print(lines[i]);
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
            print("\tkey=");
            print(expr);
            print(";out.print('Message -> '+key)");
            markLine(parser.getLine());
            println();
        }

        void action() {
            String action = parser.getToken().trim();
            if (!action.endsWith(")")) {
                action = action + "()";
            }
            print("\tout.print(actionBridge." + action + ");");
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
            tag.startLine = currentLine;
            tagsStack.push(tag);
            print("attrs" + tagIndex + " = [" + tagArgs + "];");
            if (!tag.name.equals("doBody")) {
                print("body" + tagIndex + " = {");
                markLine(parser.getLine());
                println();
            }
        }

        void endTag() {
            String tagName = parser.getToken().trim();
            if (tagsStack.isEmpty()) {
                throw new RuntimeException("Erreur d'ouverture/fermeture des tags.");
            }
            Tag tag = (Tag) tagsStack.pop();
            String lastInStack = tag.name;
            if (tagName.equals("")) {
                tagName = lastInStack;
            }
            if (!lastInStack.equals(tagName)) {
                throw new RuntimeException("Erreur d'ouverture/fermeture des tags.");
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
                print("invokeTag(" + tag.startLine + ",'" + tagName + "',attrs" + tagIndex + ",body" + tagIndex + ")");
                markLine(tag.startLine);
                println();
            }
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
            SCRIPT, // %{...}
            EXPR, // ${...}
            START_TAG, // #{...}
            END_TAG, // #{/...}
            MESSAGE, // &{...}
            ACTION, // @{...}
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
                        if (c == '@' && c1 == '{') {
                            return found(Token.ACTION, 2);
                        }
                        break;
                    case SCRIPT:
                        if (c == '}' && c1 == '%') {
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
