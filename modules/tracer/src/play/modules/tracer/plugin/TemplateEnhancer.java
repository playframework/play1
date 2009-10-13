package play.modules.tracer.plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.templates.Template;
import play.templates.TemplateCompiler;

public class TemplateEnhancer {
	
	Template template;
	StringBuffer source;
	
	private class SourceLine {
		int number;
		int start;
		int end;
		
		public SourceLine(int number, int start, int end) {
			this.number = number;
			this.start = start;
			this.end = end;
		}
		
		public String toString() {
			return source.substring(start, end);
		}
	}

	Map<Integer, SourceLine> groovySourceLines = new LinkedHashMap<Integer, SourceLine>();
	
	public TemplateEnhancer(Template template) {
		this.template = template;
		this.source = new StringBuffer(template.groovySource);
		String[] lines = template.groovySource.split("\n");
		int index = 0;
		for(int i = 0; i < lines.length; i++) {
			SourceLine sl = new SourceLine(i, index, index + lines[i].length());
			index = sl.end + 1;
			groovySourceLines.put(i, sl);
		}
	}
	
	public void parse() {
		int numberEnclosingBraces = 3 + TemplateCompiler.extensionsClassnames.size();
		
		Pattern pattern = Pattern.compile("\\{", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(template.groovySource);
		for(int i = 0; i < numberEnclosingBraces; i++) {
			matcher.find();
		}
		int startBody = matcher.end();
		int startLine = findGroovySourceLine(startBody +1).number;
		
		pattern = Pattern.compile("\\}");
		matcher = pattern.matcher(new StringBuffer(template.groovySource).reverse());
		for(int i = 0; i < numberEnclosingBraces; i++) {
			matcher.find();
		}
		int endBody = template.groovySource.length() - matcher.end();
		int endLine = findGroovySourceLine(endBody - 1).number;
		
		int currentLine = -1;
		for(Entry<Integer, Integer> entry : template.linesMatrix.entrySet()) {
			if(currentLine != entry.getValue()) {
				prependAt(entry.getKey() - 1, "play.modules.tracer.plugin.Tracer.startLine("+entry.getValue()+");");
				if(currentLine > -1)
					prependAt(entry.getKey() - 1, "play.modules.tracer.plugin.Tracer.endLine(binding); ");
				currentLine = entry.getValue();
			}
		}
		
		prependAt(startLine, "play.modules.tracer.plugin.Tracer.enterTemplate(\""+template.name+"\");");
		appendAt(endLine, "play.modules.tracer.plugin.Tracer.endLine(binding);");
		appendAt(endLine, "play.modules.tracer.plugin.Tracer.exitTemplate();");
                System.out.println(source);
	}
	
	private void appendAt(int line, String str) {
		insertAt(line, str, true);
	}
	
	private void prependAt(int line, String str) {
		insertAt(line, str, false);
	}
	
	private void insertAt(int line, String str, boolean end) {
		SourceLine sl = groovySourceLines.get(line);
		int insert = sl.start;
		if(end) {
			insert = sl.toString().indexOf("//");
			if(insert < 0)
				insert = sl.end;
			else insert += sl.start;
		}
		source.insert(insert, str);
		int length = str.length();
		sl.end += length;
		for(int i = sl.number + 1; i < groovySourceLines.size(); i++) {
			SourceLine s = groovySourceLines.get(i);
			s.start += length;
			s.end += length;
		}
	}
	
	private SourceLine findGroovySourceLine(int index) {
		for(SourceLine sl : groovySourceLines.values())
			if(sl.start <= index && sl.end >= index)
				return sl;
		return null;
	}
	
	
	public static void enhance(Template template) {
		TemplateEnhancer templateEnhancer = new TemplateEnhancer(template);
		templateEnhancer.parse();
		template.groovySource = templateEnhancer.source.toString();
	}
}
