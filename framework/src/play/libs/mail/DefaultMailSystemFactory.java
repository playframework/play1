package play.libs.mail;

import play.Play;
import play.libs.Mail;

class DefaultMailSystemFactory extends AbstractMailSystemFactory {

    private static final MailSystem LEGACY_MOCK_MAIL_SYSTEM = new Mail.LegacyMockMailSystem();
    private static final MailSystem PRODUCTION_MAIL_SYSTEM  = new ProductionMailSystem();

    @Override
    public MailSystem currentMailSystem() {
        if (Play.useDefaultMockMailSystem()) {
            return LEGACY_MOCK_MAIL_SYSTEM;
        } else {
            return PRODUCTION_MAIL_SYSTEM;
        }
    }

}
