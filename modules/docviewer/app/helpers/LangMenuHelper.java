package helpers;

import play.Play;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LangMenuHelper {
    private static final File baseDir = new File(Play.frameworkPath, "documentation");
    private static final Pattern ptn = Pattern.compile("manual_(.*)");

    public static List<LangMenu> getMenuList() {
        List<LangMenu> langMenuList = new ArrayList<LangMenu>();
        LangMenu defaultLangMenu = new LangMenu();
        defaultLangMenu.key = "en";
        defaultLangMenu.value = "English";
        langMenuList.add(defaultLangMenu);
        File[] dirs = baseDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && ptn.matcher(file.getName()).find();
            }
        });
        for (final File dir : dirs) {
            Matcher m = ptn.matcher(dir.getName());
            String langCd = "";
            if (m.find()) {
                langCd = m.group(1);
            }
            if (langCd.length() <= 0) continue;
            LangMenu langMenu = new LangMenu();
            langMenu.key = langCd;
            langMenu.value = new Locale(langCd).getDisplayName();
            langMenuList.add(langMenu);
        }

        return langMenuList;
    }

    public static class LangMenu {
        String key;
        String value;
    }
}
