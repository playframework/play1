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

    protected StringBuilder compiledSource = new StringBuilder();
    protected BaseTemplate template;
    protected TemplateParser parser;
    protected boolean doNextScan = true;
    protected TemplateParser.Token state;
    protected Stack<Tag> tagsStack = new Stack<>();
    protected int tagIndex;
    protected boolean skipLineBreak;
    protected int currentLine = 1;

    protected static class Tag {
        String name;
        int startLine;
        boolean hasBody;
    }

    protected void generate(BaseTemplate template) {
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

    protected abstract String source();

    protected abstract void head();

    protected abstract void end();

    protected abstract void plain();

    protected abstract void script();

    protected abstract void expr();

    protected abstract void message();

    protected abstract void action(boolean absolute);

    protected abstract void startTag();

    protected abstract void endTag();

    protected void markLine(int line) {
        compiledSource.append("// line ").append(line);
        template.linesMatrix.put(currentLine, line);
    }

    protected void println() {
        compiledSource.append("\n");
        currentLine++;
    }

    protected void print(String text) {
        compiledSource.append(text);
    }

    protected void println(String text) {
        compiledSource.append(text);
        println();
    }
}
