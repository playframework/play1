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
                s.append("if(!attrs"+index+"['as']) {throw new play.exceptions.TagInternalException('as attribute cannot be empty')};");
                s.append("if(attrs"+index+"['items'] == null) attrs"+index+"['items'] = [];");
                s.append("_i = 0;");
                s.append("for(_item"+index+" in attrs"+index+"['items']) {");
                s.append("setProperty(attrs"+index+"['as'], _item"+index+");");
                s.append("setProperty(attrs"+index+"['as']+'_index', _i+1);");
                s.append("setProperty(attrs"+index+"['as']+'_isLast', (_i+1) == attrs"+index+"['items'].size());");
                s.append("setProperty(attrs"+index+"['as']+'_isFirst', _i == 0);");
                s.append("setProperty(attrs"+index+"['as']+'_parity', (_i+1)%2==0?'even':'odd');");
                break;
            case END:
                s.append("_i++;");
                s.append("};");
                break;
        }
        return s.toString();
    }

}
