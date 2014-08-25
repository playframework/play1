package cn.bran.play.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Play;

import cn.bran.japid.util.DirUtil;
import cn.bran.japid.util.JapidFlags;

public class PlayDirUtil {


		/**
		 * create the basic layout: app/japidviews/_javatags app/japidviews/_layouts
		 * app/japidviews/_tags
		 * 
		 * then create a dir for each controller. //TODO
		 * 
		 * @throws IOException
		 * 
		 */
		public static List<File> mkdir(String root) throws IOException {
			String sep = File.separator;
			String japidViews = root + sep + DirUtil.JAPIDVIEWS_ROOT + sep;
			File javatags = new File(japidViews + DirUtil.JAVATAGS);
			if (!javatags.exists()) {
				boolean mkdirs = javatags.mkdirs();
				assert mkdirs;
				JapidFlags.info("created: " + japidViews + DirUtil.JAVATAGS);
			}
	
			File layouts = new File(japidViews + DirUtil.LAYOUTDIR);
			if (!layouts.exists()) {
				boolean mkdirs = layouts.mkdirs();
				assert mkdirs;
				JapidFlags.info("created: " + japidViews + DirUtil.LAYOUTDIR);
			}
	
			File tags = new File(japidViews + DirUtil.TAGSDIR);
			if (!tags.exists()) {
				boolean mkdirs = tags.mkdirs();
				assert mkdirs;
				JapidFlags.info("created: " + japidViews + DirUtil.TAGSDIR);
			}
			
			// email notifiers
			File notifiers = new File(japidViews + "_notifiers");
			if (!notifiers.exists()) {
				boolean mkdirs = notifiers.mkdirs();
				assert mkdirs;
				JapidFlags.info("created: " + japidViews + "_notifiers");
			}
			
			
			File[] dirs = new File[] { javatags, layouts, tags };
			List<File> res = new ArrayList<File>();
			res.addAll(Arrays.asList(dirs));
	
			// create dirs for controllers
	
	//		JapidFlags.info("JapidCommands: check default template packages for controllers.");
			try {
				String controllerPath = Play.applicationPath  + sep + "app" + sep + "controllers";
				File controllerPathFile = new File(controllerPath);
//				JapidFlags.info("PlayDirUtil: controller path: " + controllerPathFile.getAbsolutePath());
				if (controllerPathFile.exists()) {
					String[] controllers = DirUtil.getAllJavaFilesInDir(controllerPathFile);
					for (String f : controllers) {
						String cp = japidViews + f;
						File ff = new File(cp);
						if (!ff.exists()) {
							boolean mkdirs = ff.mkdirs();
							assert mkdirs == true;
							res.add(ff);
							JapidFlags.info("created: " + cp);
						}
					}
				}
			} catch (Exception e) {
				JapidFlags.error(e.toString());
			}
	
	//		JapidFlags.info("JapidCommands:  check default template packages for email notifiers.");
			try {
				String notifiersDir = Play.applicationPath  + sep + "app" + sep + "notifiers";
				File notifiersDirFile = new File(notifiersDir);
				if (!notifiersDirFile.exists()) {
					if (notifiersDirFile.mkdir()) {
						JapidFlags.info("created the email notifiers directory. ");
					}
					else {
						JapidFlags.info("email notifiers directory did not exist and could not be created for unknow reason. ");
					}
				}
				
				String[] controllers = DirUtil.getAllJavaFilesInDir(notifiersDirFile);
				for (String f : controllers) {
					// note: we keep the notifiers dir to differentiate those from the controller
					// however this means we cannot have a controller with package like "controllers.notifiers"
					// so we now use "_notifiers"
					String cp = japidViews + "_notifiers" + sep + f;
					File ff = new File(cp);
					if (!ff.exists()) {
						boolean mkdirs = ff.mkdirs();
						assert mkdirs == true;
						res.add(ff);
						JapidFlags.info("created: " + cp);
					}
				}
			} catch (Exception e) {
				JapidFlags.error(e.toString());
			}
			return res;
		}

}
