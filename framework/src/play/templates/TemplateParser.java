package play.templates;

/**
 * Template parser
 */
public class TemplateParser {

    private String pageSource;
    private int nestedBracesCounter; // counts nested braces in current expression/tag

    public TemplateParser(String pageSource) {
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
    private int end, begin, end2, begin2, len;
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

    public String checkNext() {
        if (end2 < pageSource.length()) {
            return pageSource.charAt(end2) + "";
        }
        return "";
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
                        nestedBracesCounter = 0;
                        return found(Token.EXPR, 2);
                    }
                    if (c == '#' && c1 == '{' && c2 == '/') {
                        return found(Token.END_TAG, 3);
                    }
                    if (c == '#' && c1 == '{') {
                        nestedBracesCounter = 0;
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
                    if (c == '}' && nestedBracesCounter == 0) {
                        return found(Token.PLAIN, 1);
                    }
                    if (c == '/' && c1 == '}') {
                        return found(Token.END_TAG, 1);
                    }
                    if (c == '{') nestedBracesCounter++;
                    if (c == '}') nestedBracesCounter--;
                    break;
                case END_TAG:
                    if (c == '}') {
                        return found(Token.PLAIN, 1);
                    }
                    break;
                case EXPR:
                    if (c == '}' && nestedBracesCounter == 0) {
                        return found(Token.PLAIN, 1);
                    }
                    if (c == '{') nestedBracesCounter++;
                    if (c == '}') nestedBracesCounter--;
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