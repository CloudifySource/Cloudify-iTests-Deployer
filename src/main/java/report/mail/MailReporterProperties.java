package deployer.report.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;


public class MailReporterProperties {

    final static String USERNAME_PROP    = "username";
    final static String PASSWORD_PROP    = "password";

    final static String MAIL_HOST = "mailHost";
    final static String RECIPIENTS = "recipients";
    final static String WEBUI = "webui";
    final static String CLOUDIFY = "cloudify";
    
    private final Properties props;

    public MailReporterProperties(Properties props) {
        this.props = props;
    }

    public String getUsername() {
        return props.getProperty(USERNAME_PROP);
    }

    public String getPassword() {
        return props.getProperty(PASSWORD_PROP);
    }

    public String getMailHost() {
        return  props.getProperty(MAIL_HOST);
    }

    public List<String> getRecipients() {
        List<String> _recipients = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(props.getProperty(RECIPIENTS), ",");
        while(st.hasMoreTokens() == true){
            _recipients.add(st.nextToken());
        }

        return _recipients;
    }
    
    public List<String> getWebUIRecipients() {
        List<String> _recipients = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(props.getProperty(WEBUI), ",");
        while(st.hasMoreTokens() == true){
            _recipients.add(st.nextToken());
        }

        return _recipients;
    }
    
    public List<String> getCloudifyRecipients() {
        List<String> _recipients = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(props.getProperty(CLOUDIFY), ",");
        while(st.hasMoreTokens() == true){
            _recipients.add(st.nextToken());
        }

        return _recipients;
    }

}
