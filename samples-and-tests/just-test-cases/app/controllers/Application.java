package controllers;

import java.util.*;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import play.*;
import play.mvc.*;
import play.i18n.Lang;
import play.libs.*;
import play.jobs.*;

import models.*;
import utils.*;
import jobs.*;

import javax.mail.internet.InternetAddress;

@play.db.jpa.Transactional
public class Application extends Controller {

    // bug

    public static void aa() {
        try {
            @SuppressWarnings("unused")
            int test = 1;
        } catch (Exception ex) {
        }
    }
    
    public static void helloZen() {
        renderText("Hello");
    }
    
    public static void myHomePage(String clientName) {
        renderText(clientName);
    }
    
    public static void some1() {
        renderText(Invoker.InvocationContext.current());
    }
    
    @Youhou
    public static void some2() {
        renderText(Invoker.InvocationContext.current());
    }
    
    @Youhou
    public static void some3() throws Exception {
        JobWithContext job = new JobWithContext();
        Future<String> future = job.now();
        renderText(future.get());
    }
    

    // bug

    public static void aaa() {
        try {
            boolean test = TestUtil.invokeTest("a");
            Logger.info("test:" + test);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        renderText("IT WORKS");
    }

    public static void ok(String re) {
        renderText("OK: " + re);
    }

    public static void index() {
        routeArgs.put("lucky", "strike");
        render();
    }
    
    public static void showIt() {
        renderText("Done");
    }

    public static void index2() {
        renderText(Router.reverse("Application.index2"));
    }

    public static void simpleStatusCode() {
        response.status = 204;
    }
    
    public static void imagesAssets() {
        
    }
    
    public static void dashboard(String client) {
        
    }

    public static void hello(String name) {
        render(name);
    }

    public static void yop() {
        render();
    }

    public static void alertConfirmPrompt() {
        render();
    }

    public static void dynamicClassBinding(boolean fail) {
        render(fail);
    }

    public static void tagContexts() {
        render();
    }
    
    public static void book(Date at) {
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("dd/MM/yy");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        renderText("Booked at %s !!", df.format(at));
    }

    public static void escapeData() {
        String oops = "&nbsp;<i>Yop</i>&nbsp;";
        render(oops);
    }

    public static void aGetForm(String name) {
        render("Application/hello.html", name);
    }

    public static void aGetForm2(String name) {
        name = "2" + name;
        render("Application/hello.html", name);
    }

    public static void optional() {
        renderText("OK");
    }

    public static void reverserouting() {
        render("Application/reverse.html");
    }

    public static void reverserouting2() {
        render("Application/reverse2.html");
    }

    public static void reverserouting3() {
        Object def = reverse(); {
            JPABinding.save(new Project("COLCOZ"));
        }
        renderText(def);
    }

    public static void mail() {
        notifiers.Welcome.welcome();
        renderText("OK");
    }

    public static void mail2() {
        Welcome.welcome();
        renderText("OK2");
    }

    public static void mail3() {
        notifiers.Welcome.welcome2();
        renderText("OK3");
    }

    public static void mail4() {
        notifiers.Welcome.welcome3();
        renderText("OK4");
    }

    public static void mail5() throws EmailException {
        HtmlEmail email = new HtmlEmail();
        email.setHtmlMsg("<html><body><h1>A Title</h1></body></html>");
        email.setTextMsg("alternative message");
        EmailAttachment attachment = new EmailAttachment();
        attachment.setDescription("An image");
        attachment.setDisposition(EmailAttachment.ATTACHMENT);
        attachment.setPath(Play.applicationPath.getPath() + java.io.File.separator + "test" + java.io.File.separator + "fond2.png");
        EmailAttachment attachment2 = new EmailAttachment();
        attachment2.setName("fond3.jpg");
        attachment2.setPath(Play.applicationPath.getPath() + java.io.File.separator + "test" + java.io.File.separator + "fond3.jpg");
        email.attach(attachment);
        email.attach(attachment2);
        email.setFrom("test@localhost");
        email.addTo("test@localhost");
        email.setSubject("test attachments");
        Mail.send(email);
        renderText("OK5");
    }
    
    public static void mailWithUrls() {
        notifiers.Welcome.welcome_mailWithUrls(false);
        renderText("OK_mailWithUrls");
    }
    
    public static class MailJob extends Job {
        @Override
        public void doJob() {
            notifiers.Welcome.welcome_mailWithUrls(true);
        }
    }
    
    public static void mailWithUrlsInJob() throws Exception {
        new MailJob().now().get();
        renderText("OK_mailWithUrlsInJob");
    }

    public static void ifthenelse() {
        boolean a = true;
        boolean b = false;
        String c = "";
        String d = "Yop";
        int e = 0;
        int f = 5;
        Boolean g = null;
        Boolean h = true;
        Object i = null;
        Object j = new Object();
        render(a, b, c, d, e, f, g, h, i, j);
    }

    public static void listTag() {
        List<String> a = new ArrayList<String>();
        a.add("aa");
        a.add("ab");
        a.add("ac");
        int[] b = new int[]{0, 1, 2, 3};
        Iterator d = a.iterator();
        render(a, b, d);
    }

    public static void a() {
        render();
    }

    public static void useSpringBean() {

    }

    @javax.inject.Inject
    static Test myTest;

    public static void useSpringBeanInject() {
        renderText(myTest.yop());
    }

    public static void googleSearch(String word) throws InterruptedException, ExecutionException {
        WS.HttpResponse response = WS.url("http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=%s", word).get();
        long results = response.getJson().getAsJsonObject().getAsJsonObject("responseData").getAsJsonObject("cursor").getAsJsonPrimitive("estimatedResultCount").getAsLong();
        renderText(results);
    }

    public static void selectTag(){
        List<User> users = new ArrayList<User>(10);
        User user;
        
        // Check html escaping
        user = new User("<All>");
        users.add(user);
        
        for(long i = 0; i < 10; i++) {
        	user = new User("User-" + i);
        	user.k = i;
        	user.i = (int) i;
        	users.add(user);
        }
        
        render(users);
    }
}
