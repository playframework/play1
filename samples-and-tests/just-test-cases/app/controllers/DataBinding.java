package controllers;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import play.data.binding.annotations.Bind;
import play.i18n.Lang;
import play.mvc.Controller;

public class DataBinding extends Controller {

    public static void showDefaultDateFormat(Date date) {
       renderText(date);
    }

    public static void changeLanguage(String lang) {
        Lang.change(lang);
    }

    public static void showLocaleDateFormat(Date date) {
        renderText(date);
    }

    public static void showDefaultLocaleDateFormat(Date date) {
        renderText(date);
    }

    public static void showDateFormat(@Bind(format = "MM-dd-yyyy'T'HH:mm:ss") Date date) {
        renderText(date);
    }

    public static void showList(@Bind(separator = "/") List<String> mailboxes) {
        renderText(mailboxes);
    }

    public static void showCalendar(@Bind(format = "dd-MMM-yyyy") Calendar cal) {
        renderText(cal.getTime());
    }
}
