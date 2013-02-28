/*
 * @(#)WikiClient.java   Mar 6, 2007
 *
 * Copyright 2007 GigaSpaces Technologies Inc.
 */

package deployer.report.wiki;


/**
 * This class provides wiki client functionality, store/remove/add attachment page on wiki site.
 *
 * @author		 Igor Goldenberg
 * @since		 6.0
 **/
public class WikiClient
{
   static enum WikiAction
	{
	  storePage, removePage	
	}
	
  private String serverAddress;	
  private String userName;
  private String password;
	
  private WikiClient( String serverAddress, String userName, String password )
  {
	 this.serverAddress = serverAddress;  
	 this.userName = userName;
	 this.password = password;
  }
  
  public String getServerAdString()
  {
	 return serverAddress; 
  } 
 
  public String getUserName()
  {
	 return userName;
  }

  public String getPassword()
  {
	 return password;  
  }
  
  public static WikiClient login( String serverAddress, String userName, String password )
  {
	return new WikiClient( serverAddress, userName, password );
  }
  
  public void uploadPage( WikiPage wikiPage ) 
  	 throws WikiConnectionException
  {
	 onAction( WikiAction.storePage, wikiPage );
  }

    public void removePage( WikiPage wikiPage )
            throws WikiConnectionException
    {
        onAction( WikiAction.removePage, wikiPage );
    }
  
  private Object onAction( WikiAction action, WikiPage wikiPage ) 
  	throws WikiConnectionException
  {
	 String[] command = buildCommand( action, wikiPage );  
	 try
	 {
	   switch( action )
	   {
		  case storePage:
              org.swift.confluence_soap.SoapClient.main(command);
              break;
          case removePage:
              org.swift.confluence_soap.SoapClient.main(command);
              break;
	   }
		  
		/* so far nothing return */
		return null;
	 }catch( Exception ex )
	 {
	   throw new WikiConnectionException("Action: " + action + " failed.", ex);	 
	 }
  }
  
  
  private String[] buildCommand( WikiAction action, WikiPage wikiPage )
  {
		String[] wikiCommand = new String[] 
		                 				     {"--server", serverAddress,
		                 						"--user", userName,
		                 						"--password", password,
		                 						"--action", action.toString(),
		                 						"--title", wikiPage.getTitlePage(),
		                 						"--space", wikiPage.getWikiSpace(),
		                 						"--content", wikiPage.getContext(),
		                 						"--parent", wikiPage.getParentPage()
//		                 						,"-v" /* debug */
		                 						};
		
		return wikiCommand;
  }
}
