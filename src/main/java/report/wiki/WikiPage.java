/*
 * @(#)WikiPage.java Mar 6, 2007 Copyright 2007 GigaSpaces Technologies Inc.
 */
package deployer.report.wiki;

/**
 * This class provides Wiki page information.
 * 
 * @author Igor Goldenberg
 * @since 1.0
 * @see WikiClient
 */
public class WikiPage
{
    private final String	wikiSpace;
    private final String	titlePage;
    private final String	context;
    private final String parentPage;

    /**
     * Constructor.
     * @param wikiSpace name of wiki space.
     * @param parentPage title parent page, can be <code>null</code> if this page doesn't have a parent page.
     * @param titlePage name of the wiki page.
     * @param context wiki page context.
     */
    public WikiPage(String wikiSpace, String parentPage, String titlePage, String context)
    {
        if ( wikiSpace == null || titlePage == null )
            throw new IllegalArgumentException("wikiSpace or title page can not be null");

        this.wikiSpace = wikiSpace;
        this.parentPage = parentPage;
        this.titlePage = titlePage;
        this.context = context;
    }

    public String getContext()
    {
        return context;
    }

    public String getTitlePage()
    {
        return titlePage;
    }

    public String getWikiSpace()
    {
        return wikiSpace;
    }

    public String getParentPage()
    {
        return parentPage;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(getWikiSpace());
        output.append(" > ");
        output.append(getParentPage() == null ? "" : getParentPage() + " > ");
        output.append(getTitlePage());
        return output.toString();
    }
}
