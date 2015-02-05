package notifiers;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.imageio.ImageIO;

import play.mvc.*;

public class Welcome extends Mailer {
    
    public static void welcome() {
        String msg = "Welcome";
        setFrom("x@x.com");
        setSubject("Yop");
        addRecipient("toto@localhost");
        send(msg);
    }    
  
   public static void welcome2() {
        String msg = "Welcome";
        setFrom("x@x.com");
        setSubject("Yop3");
        addRecipient("toto@localhost");
        addBcc("nicolas@localhost");
        addCc("guillaume@localhost");
        send(msg);
    }    
    
    public static void welcome3() {
        String msg = "Welcome";
        setFrom("x@x.com");
        setSubject("Yop4");
        addRecipient("toto@localhost");
        send(msg);
    }
    
    public static void subjectwithpercent() {
        String msg = "Welcome";
        setFrom("x@x.com");
        setSubject("Yop4 % o");
        addRecipient("toto@localhost");
        send(msg);
    }
    public static void seleniumTest() {
        String msg = "Selenium";
        setFrom("x@x.com");
        setSubject("Berillium Subject");
        addRecipient("boron@localhost");
        send(msg);
    }
    
    public static void welcome_mailWithUrls(boolean fromJob) {
        String msg = "Welcome";
        setFrom("x@x.com");
        setSubject("Yop4");
        if( fromJob ) {
            addRecipient("mailWithUrlsJob@localhost");
        } else {
            addRecipient("mailWithUrls@localhost");
        }

        send(msg);
    }

    public static void mailWithEmbeddedImage() {
        String msg = "Welcome";
        setFrom("x@x.com");
        setSubject("Mail With Embedded Images");
        addRecipient("mailWithEmbeddedImage@localhost");
        send(msg);
    }
    
    public static void mailWithDynamicEmbeddedImage() {
        String msg = "Welcome";
        setFrom("x@x.com");
        setSubject("Mail With Dynamic Embeded Image");
        addRecipient("mailWithDynamicEmbeddedImage@localhost");
        
		String imageCidHref = null;

		try {
			BufferedImage image =  new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = image.createGraphics();
			g2.drawRect(1, 1, 98, 98);
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(image, "png", os);
			
			final byte[] data = os.toByteArray();
			
			DataSource dataSource = new DataSource() {
				@Override
				public String getContentType() { return "image/png"; }
				@Override
				public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(data); }
				@Override
				public String getName() { return "testImage"; }
				@Override
				public OutputStream getOutputStream() throws IOException { throw new UnsupportedOperationException(); }
			};
			
			imageCidHref = attachInlineEmbed(dataSource, "testImage");
		} catch (IOException e) {
			throw new IllegalStateException("", e);
		}
        
        send(msg, imageCidHref);
    }
}
