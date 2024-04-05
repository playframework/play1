package controllers;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;

import org.apache.commons.io.IOUtils;

import play.data.binding.As;
import play.i18n.Lang;
import play.mvc.Controller;
import play.utils.Utils;

public class DataBinding extends Controller {
    
    @play.mvc.Before(unless = "myInputStream") static void lang(String lang) {
        System.out.println(lang);
    }

    public static void showDefaultDateFormat(Date date) {
        renderText(new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(date));
    }

    public static void changeLanguage(String lang) {
        Lang.change(lang);
    }

    public static void showLocaleDateFormat(Date date) {
        renderText(new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(date));
    }

    public static void showDefaultLocaleDateFormat(Date date) {
        renderText(new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(date));
    }

    public static void showDateFormat(@As("MM-dd-yyyy'T'HH:mm:ss") Date date) {
        renderText(new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(date));
    }

    public static void showList(@As("/") List<String> mailboxes) {
        renderText(mailboxes);
    }

    public static void showCalendar(@As("dd-MMM-yyyy") Calendar cal) {
        renderText(new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(cal.getTime()));
    }


    public static void showCalendar2(@As(lang={"fr,de","*"}, value={"dd-MM-yyyy","MM-dd-yyyy"}) Calendar cal) {
        renderText(new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(cal.getTime()));
    }

    public static void signinPage() {
        render();
    }
    
    public static void customeBinding(@As(binder=utils.TestBinder.class) String yop) {
        renderText(yop);
    }
    
    public static void globalBinder(java.awt.Point p) {
        if(validation.hasErrors()) {
            renderText(validation.errors());
        }
        renderText(p.x + "|" + p.y);
    }


    

    public static void printParams() {
        Map<String, String> paramMap = params.allSimple();
        String out = "";
        for (String key : paramMap.keySet()) {
            out += key + " " + paramMap.get(key) + "\n";
        }
        renderText(out);
    }


    public static void myInputStream(String productCode) throws Exception {
        renderText(productCode + " - " + IOUtils.toString(request.body));
    }

    public static class BeanWithByteArray {
        public byte[] ba;
    }
    
    public static void bindBeanWithByteArray(BeanWithByteArray b) {
        if ( b == null) {
            renderText("b==null");
        }
        
        if ( b.ba == null) {
            renderText("b.ba==null");
        }
        
        renderText("b.ba.length=" + b.ba.length);
    }

}

