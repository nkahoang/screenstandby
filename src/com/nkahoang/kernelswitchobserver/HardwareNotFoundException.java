package com.nkahoang.kernelswitchobserver;

public class HardwareNotFoundException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String mMessage;
	HardwareNotFoundException(String hardwareName)
	{
		mMessage = "Hardware path " + hardwareName + " not found (Does kernel have this hardware?)";
	}
	public String getMessage()
	{
		return mMessage;
	}
}
