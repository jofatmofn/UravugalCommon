package org.sakuram.relation.util;

import org.apache.logging.log4j.LogManager;

public class AppException extends java.lang.RuntimeException
{
	private static final long serialVersionUID=2191524434441210327L;
	private String originatingClass;
	private String originatingMethod;
	private String message;
	public AppException(String pMessage, Exception pException)
	{
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
        /*
         * Reason for using the value of 2 above.
         * 0 - getStackTrace
         * 1 - thisMethod => AppException's Constructor
         * 2 - callingMethod => method calling thisMethod <-- We need this
         * 3 - method calling callingMethod
         * and so on
         *
         * Idea taken from http://swbyjeff.com/index.php/Dynamically_Retreive_Method_Name
         */
		commonConstructor(stackTraceElement.getClassName(), stackTraceElement.getMethodName(), pMessage, pException);
	}
	
	private void commonConstructor(String pClassName, String pMethodName, String pMessage, Exception pException)
	{
		this.message = pMessage;
        this.originatingClass = pClassName;
        this.originatingMethod = pMethodName;
		LogManager.getLogger().error((pException != null && pException.getMessage() != null) ? pException.getMessage() : "");
        // Only current class (AppException), current method (commonConstructor), above line number will be logged.
		// Required to be logged: pClassName, pMethodName, actual line number causing exception
        if (pException != null)
        	pException.printStackTrace();
	}
	
	public String getOriginatingClass()
	{
		return this.originatingClass;
	}

	public String getOriginatingMethod()
	{
		return this.originatingMethod;
	}

	public String getMessage()
	{
		return this.message;
	}
		public void setMessage(String message) {
		this.message = message;
	}
}
