package cn.bran.japid.perf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;

public class CreateDummyTemplates {
	// this thing is used to generate lots of template files of the same content to test the play 
	// precompile performance
	@Test
	public void createDummyTemplates() throws IOException {
		String text = "<body> \r\n" + 
				"  <div class=\"container_12\"> \r\n" + 
				"    <div class=\"grid_4 prefix_2 title\"> \r\n" + 
				"      <h1><a href=\"/\">git ready</a></h1> \r\n" + 
				"    </div> \r\n" + 
				"    <div class=\"grid_4 suffix_2 info\"> \r\n" + 
				"      <div class=\"extra\">learn git one commit at a time</div> \r\n" + 
				"      <div class=\"author\">by Nick Quaranto</div> \r\n" + 
				"    </div> \r\n" + 
				"    <div class=\"grid_8 prefix_2 suffix_2 main\"> \r\n" + 
				"      <div id=\"post\" class=\"grid_8 alpha content\"> \r\n" + 
				"  <h2><a href=\"/beginner/2009/01/11/reverting-files.html\"> \r\n" + 
				"    reverting files\r\n" + 
				"  </a></h2> \r\n" + 
				"  <div id=\"date\">committed 11 Jan 2009</div> \r\n" + 
				"  <p>This is a topic that is a constant source of confusion for many git users, basically because there&#8217;s more than one way to skin the proverbial cat. Let&#8217;s go over some of the basic commands that you&#8217;ll need to undo your work.</p> \r\n" + 
				"  <div id=\"disqus\"> \r\n" + 
				"  </div> \r\n" + 
				"</div> \r\n" + 
				" \r\n" + 
				" \r\n" + 
				"    </div> \r\n" + 
				"    <div class=\"grid_2 prefix_2 links\"> \r\n" + 
				"      <h3 id=\"green\">beginner</h3> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/03/13/smartly-save-stashes.html\">smartly save stashes</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/03/09/remote-tracking-branches.html\">remote tracking branches</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/03/06/ignoring-doesnt-remove-a-file.html\">ignoring doesn't remove a file</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/03/02/where-to-find-the-git-community.html\">where to find the git community</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/02/23/finding-who-committed-what.html\">finding who committed what</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/02/19/what-git-is-not.html\">what git is not</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/02/17/how-git-stores-your-data.html\">how git stores your data</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/02/04/converting-from-svn.html\">converting from svn</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/02/03/tagging.html\">tagging</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/02/02/push-and-delete-branches.html\">push and delete remote branches</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/01/27/installing-git.html\">installing git</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/01/25/branching-and-merging.html\">branching and merging</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/01/21/pushing-and-pulling.html\">pushing and pulling</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/01/19/ignoring-files.html\">ignoring files</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/01/18/the-staging-area.html\">the staging area</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/01/16/cleaning-up-untracked-files.html\">cleaning up untracked files</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/01/11/reverting-files.html\">reverting files</a> \r\n" + 
				"      \r\n" + 
				"        <a href=\"/beginner/2009/01/10/stashing-your-changes.html\">stashing your changes</a> \r\n" + 
				"      \r\n" + 
				"    </div> \r\n" + 
				"  </div> \r\n" + 
				"  <script type=\"text/javascript\"> \r\n" + 
				"  var gaJsHost = ((\"https:\" == document.location.protocol) ? \"https://ssl.\" : \"http://www.\");\r\n" + 
				"  document.write(unescape(\"%3Cscript src='\" + gaJsHost + \"google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E\"));\r\n" + 
				"  </script> \r\n" + 
				"  <script type=\"text/javascript\"> \r\n" + 
				"  try {\r\n" + 
				"  var pageTracker = _gat._getTracker(\"UA-6929260-1\");\r\n" + 
				"  pageTracker._trackPageview();\r\n" + 
				"  } catch(err) {}</script> \r\n" + 
				"  <div id=\"feedburner\"> \r\n" + 
				"    <a href=\"http://feeds2.feedburner.com/git-ready\"><img src=\"http://feeds2.feedburner.com//git-ready?bg=FFE3A3&amp;fg=000000&amp;anim=0\" height=\"26\" width=\"88\" style=\"border:0\" alt=\"\" /></a> \r\n" + 
				"  </div> \r\n" + 
				"</body> ";
		
		String dir = "app/japidviews/tmp/";
		new File(dir).mkdirs();
		for (int i  =0; i < 800; i++) {
			String fname = "dummy" + i + ".html";
			File file = new File(dir + fname);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			bos.write(text.getBytes("utf-8"));
			bos.close();
		}
	}
}
