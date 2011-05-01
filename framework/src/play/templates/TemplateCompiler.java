package play.templates;

import java.util.Stack;
import play.Logger;
import play.vfs.VirtualFile;
import play.exceptions.TemplateCompilationException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;

public abstract class TemplateCompiler {

    public BaseTemplate compile(BaseTemplate template) {
        try {
            long start = System.currentTimeMillis();
            generate(template);
            if (Logger.isTraceEnabled()) {
                Logger.trace("%sms to parse template %s", System.currentTimeMillis() - start, template.name);
            }
            return template;
        } catch (PlayException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public BaseTemplate compile(VirtualFile file) {
        return compile(new GroovyTemplate(file.relativePath(), file.contentAsString()));
    }

    StringBuilder compiledSource = new StringBuilder();
    BaseTemplate template;
    TemplateParser parser;
    boolean doNextScan = true;
    TemplateParser.Token state;
    Stack<Tag> tagsStack = new Stack<Tag>();
    int tagIndex;
    boolean skipLineBreak;
    int currentLine = 1;

    static class Tag {
        String name;
        int startLine;
        boolean hasBody;
    }

    void generate(BaseTemplate template) {
        this.template = template;
        String source = source();
        this.parser = new TemplateParser(source);

        // Class header
        head();

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

        // Class end
        end();

        // Check tags imbrication
        if (!tagsStack.empty()) {
            Tag tag = tagsStack.peek();
            throw new TemplateCompilationException(template, tag.startLine, "#{" + tag.name + "} is not closed.");
        }

        // Done !
        template.compiledSource = compiledSource.toString();

        if (Logger.isTraceEnabled()) {
            Logger.trace("%s is compiled to %s", template.name, template.compiledSource);
        }

    }

    abstract String source();

    abstract void head();

    abstract void end();

    abstract void plain();

    abstract void script();

    abstract void expr();

    abstract void message();

    abstract void action(boolean absolute);

    abstract void startTag();

    abstract void endTag();

    void markLine(int line) {
        compiledSource.append("// line ").append(line);
        template.linesMatrix.put(currentLine, line);
    }

    void println() {
        compiledSource.append("\n");
        currentLine++;
    }

    void print(String text) {
        compiledSource.append(text);
    }

    void println(String text) {
        compiledSource.append(text);
        println();
    }
}
