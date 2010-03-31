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
   
    public static String _list(int index, CALL f) {
        StringBuffer s = new StringBuffer();
        switch(f) {
            case START:
                s.append("if(!attrs"+index+"['as']) {attrs"+index+"['as'] = '';};");
                s.append("if(!attrs"+index+"['items']) {attrs"+index+"['items'] = attrs"+index+"['arg'];};");
                s.append("if(attrs"+index+"['items']) { play.templates.TagContext.parent().data.put('_executeNextElse', false);");
                s.append("_iter"+index+" = attrs"+index+"['items'].iterator();");
                s.append("for (_"+index+"_i = 1; _iter"+index+".hasNext(); _"+index+"_i++) {");
                s.append("_item"+index+" = _iter"+index+".next();");
                s.append("setProperty(attrs"+index+"['as'] ?: '_', _item"+index+");");
                s.append("setProperty(attrs"+index+"['as']+'_index', _"+index+"_i);");
                s.append("setProperty(attrs"+index+"['as']+'_isLast', !_iter"+index+".hasNext());");
                s.append("setProperty(attrs"+index+"['as']+'_isFirst', _"+index+"_i == 1);");
                s.append("setProperty(attrs"+index+"['as']+'_parity', _"+index+"_i%2==0?'even':'odd');");
                break;
            case END:
                s.append("};");
                s.append("} else { play.templates.TagContext.parent().data.put('_executeNextElse', true); }");
                break;
        }
        return s.toString();
    }

}
