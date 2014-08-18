package play.libs.mail;

public abstract class AbstractMailSystemFactory {

    public static final AbstractMailSystemFactory DEFAULT = new DefaultMailSystemFactory();

    public abstract MailSystem currentMailSystem();

}
