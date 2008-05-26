package play.templates;

import java.io.File;
import java.io.FileInputStream;
import play.Play;
import play.libs.Files;

public class TemplateCompiler {
    
    public static Template compile(File file) {
        try {
            String source = Files.readContentAsString(new FileInputStream(file));
            String name = Play.relativize(file);
            Template template = new Template(name, source);
            new Compiler().hop(template);
            return template;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    } 
    
    public static class Compiler {
        
        StringBuilder groovySource = new StringBuilder();
        Template template;     
        Parser parser;
        boolean doNextScan = true;
        Parser.Token state;
        
        void hop(Template template) {        
            this.template = template;
            this.parser = new Parser(template.source);
            
            // Class header
            print("class ");
            String className = "Template_" + template.name.replaceAll("/", "_").replaceAll("\\.", "_");
            print(className);
            println(" extends play.templates.Template {");
            println("public Object run() { use(play.templates.Extensions) {");
            
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
        }

        void action() {
        }

        void startTag() {
        }

        void endTag() {
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
        private int openAc = 0;

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
                            return  found(Token.SCRIPT, 2);
                        }
                        if (c == '$' && c1 == '{') {
                            return  found(Token.EXPR, 2);
                        } 
                        if (c == '#' && c1 == '{') {
                            return  found(Token.START_TAG, 2);
                        }
                        if (c == '#' && c1 == '{' && c2 == '/') {
                            return  found(Token.END_TAG, 3);
                        }
                        if (c == '&' && c1 == '{') {
                            return  found(Token.MESSAGE, 2);
                        }
                        if (c == '@' && c1 == '{') {
                            return  found(Token.ACTION, 2);
                        }
                        break;
                    case SCRIPT:
                        if (c == '{') {
                            openAc++;
                        }
                        if (c == '}') {
                            if(openAc == 0) {
                                return found(Token.PLAIN, 1);
                            }
                            openAc--;                            
                        }
                        break;
                    case START_TAG:
                        if (c == '{') {
                            openAc++;
                        }
                        if (c == '}') {
                            if(openAc == 0) {
                                return found(Token.PLAIN, 1);
                            }
                            openAc--;                            
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
                        if (c == '{') {
                            openAc++;
                        }
                        if (c == '}') {
                            if(openAc == 0) {
                                return found(Token.PLAIN, 1);
                            }
                            openAc--; 
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
