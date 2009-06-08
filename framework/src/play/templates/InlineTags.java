package play.templates;

public class InlineTags {
    
    public enum CALL {
        START, END
    }
    
    public static String _if(int index, CALL f) {
        StringBuffer s = new StringBuffer();
        switch(f) {
            case START:
                s.append("if(attrs"+index+"['arg']) {");
                break;
            case END:
                s.append("play.templates.TagContext.parent().data.put('_executeNextElse', false);");
                s.append("} else {");
                s.append("play.templates.TagContext.parent().data.put('_executeNextElse', true);");
                s.append("}");
                break;
        }
        return s.toString();
    }
    
    public static String _ifnot(int index, CALL f) {
        StringBuffer s = new StringBuffer();
        switch(f) {
            case START:
                s.append("if(!attrs"+index+"['arg']) {");
                break;
            case END:
                s.append("play.templates.TagContext.parent().data.put('_executeNextElse', false);");
                s.append("} else {");
                s.append("play.templates.TagContext.parent().data.put('_executeNextElse', true);");
                s.append("}");
                break;
        }
        return s.toString();
    }
    
    public static String _else(int index, CALL f) {
        StringBuffer s = new StringBuffer();
        switch(f) {
            case START:
                s.append("if(play.templates.TagContext.parent().data.get('_executeNextElse')) {");
                break;
            case END:
                s.append("};");
                s.append("play.templates.TagContext.parent().data.remove('_executeNextElse');");
                break;
        }
        return s.toString();
    }
    
    public static String _elseif(int index, CALL f) {
        StringBuffer s = new StringBuffer();
        switch(f) {
            case START:
                s.append("if(play.templates.TagContext.parent().data.get('_executeNextElse') && attrs"+index+"['arg']) {");
                break;
            case END:
                s.append("play.templates.TagContext.parent().data.put('_executeNextElse', false);");
                s.append("};");
                break;
        }
        return s.toString();
    }
   

}
