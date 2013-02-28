/*
 * @(#)WikiConnectionException.java   Mar 6, 2007
 *
 * Copyright 2007 GigaSpaces Technologies Inc.
 */
package deployer.report.wiki;

/**
 * This exception thrown by {@link WikiClient} on wiki operation failure.
 * 
 * @author Igor Goldenberg
 * @since  1.0
 **/
public class WikiConnectionException
		extends Exception
{
	private static final long	serialVersionUID	= 1L;

	public WikiConnectionException( String msg, Throwable ex )
	{
	  super( msg, ex );
	}
	
	public WikiConnectionException( String msg )
	{
	  super( msg );	
	}
}
