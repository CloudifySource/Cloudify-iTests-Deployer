package deployer.report.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;

public class SimpleMail {

    public static void send(String mailHost, final String user, final String password, String title, String content, List<String> recipients) throws Exception {

        Properties props = new Properties();

        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", mailHost);
        props.setProperty("mail.user", user);
        props.setProperty("mail.password", password);
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.port", "587");
        props.put("mail.smtp.socketFactory.fallback", "false");


        Session mailSession = Session.getDefaultInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
        Transport transport = mailSession.getTransport();

        InternetAddress[] address = new InternetAddress[1];
        address[0] = new InternetAddress("tgrid@gigaspaces.com");

        MimeMessage message = new MimeMessage(mailSession);
        message.addFrom(address);
        message.setSubject(title);
        message.setContent(content, "text/html; charset=ISO-8859-1");

        InternetAddress[] recipientAddresses = new InternetAddress[recipients.size()];
        for (int i = 0; i < recipients.size(); i++) {
            recipientAddresses[i] = new InternetAddress(recipients.get(i));
        }
        message.addRecipients(Message.RecipientType.TO, recipientAddresses);

        transport.connect();
        transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
        transport.close();
    }
}
