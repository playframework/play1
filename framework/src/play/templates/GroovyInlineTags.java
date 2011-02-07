package play.templates;

public class GroovyInlineTags {
    
    public enum CALL {
        START, END
    }
    
    public static String _if(int index, CALL f) {
        StringBuilder s = new StringBuilder();
        switch(f) {
            case START:
                s.append("if(attrs").append(index).append("['arg']) {");
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
        StringBuilder s = new StringBuilder();
        switch(f) {
            case START:
                s.append("if(!attrs").append(index).append("['arg']) {");
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
        StringBuilder s = new StringBuilder();
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
        StringBuilder s = new StringBuilder();
        switch(f) {
            case START:
                s.append("if(play.templates.TagContext.parent().data.get('_executeNextElse') && attrs").append(index).append("['arg']) {");
                break;
            case END:
                s.append("play.templates.TagContext.parent().data.put('_executeNextElse', false);");
                s.append("};");
                break;
        }
        return s.toString();
    }
   
    public static String _list(int index, CALL f) {
        StringBuilder s = new StringBuilder();
        switch(f) {
            case START:
                s.append("if(!attrs").append(index).append("['as']) {attrs").append(index).append("['as'] = '';};");
                s.append("if(!attrs").append(index).append("['items']) {attrs").append(index).append("['items'] = attrs").append(index).append("['arg'];};");
                s.append("if(attrs").append(index).append("['items']) { play.templates.TagContext.parent().data.put('_executeNextElse', false);");
                s.append("_iter").append(index).append(" = attrs").append(index).append("['items'].iterator();");
                s.append("for (_").append(index).append("_i = 1; _iter").append(index).append(".hasNext(); _").append(index).append("_i++) {");
                s.append("_item").append(index).append(" = _iter").append(index).append(".next();");
                s.append("setProperty(attrs").append(index).append("['as'] ?: '_', _item").append(index).append(");");
                s.append("setProperty(attrs").append(index).append("['as']+'_index', _").append(index).append("_i);");
                s.append("setProperty(attrs").append(index).append("['as']+'_isLast', !_iter").append(index).append(".hasNext());");
                s.append("setProperty(attrs").append(index).append("['as']+'_isFirst', _").append(index).append("_i == 1);");
                s.append("setProperty(attrs").append(index).append("['as']+'_parity', _").append(index).append("_i%2==0?'even':'odd');");
                break;
            case END:
                s.append("};");
                s.append("} else { play.templates.TagContext.parent().data.put('_executeNextElse', true); }");
                break;
        }
        return s.toString();
    }

}
