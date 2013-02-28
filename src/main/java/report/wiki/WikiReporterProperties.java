package deployer.report.wiki;

import java.util.Properties;

/**
 * Properties wrapper extracted from wikireporter.properties file
 * 
 * @author moran
 */
public class WikiReporterProperties {

    final static String WIKI_SERVER_PROP      = "server";
    final static String WIKI_USERNAME_PROP    = "username";
    final static String WIKI_PASSWORD_PROP    = "password";
    final static String WIKI_SPACE_PROP       = "space";

    private final Properties props;

    public WikiReporterProperties(Properties props) {
        this.props = props;
    }

    public String getWikiServerUrl() {
        return props.getProperty(WIKI_SERVER_PROP);
    }

    public String getUsername() {
        return props.getProperty(WIKI_USERNAME_PROP);
    }

    public String getPassword() {
        return props.getProperty(WIKI_PASSWORD_PROP);
    }

    public String getWikiSpace() {
        return props.getProperty(WIKI_SPACE_PROP);
    }
}
