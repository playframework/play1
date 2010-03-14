package utils;

import groovy.lang.*;
import java.util.*;
import java.io.*;

import play.templates.*;
import play.templates.GroovyTemplate.*;

@FastTags.Namespace("my.tags")
public class MyTags extends FastTags {

    public static void _hello(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        String name;
        if(args.containsKey("arg")) {
            name = (String)args.get("arg");
        } else if(args.containsKey("name")) {
            name = (String)args.get("name");
        } else {
            name = JavaExtensions.toString(body);
        }
        out.println("Hello " + name);
    }
    
}
