package com.synochip.sdk.ukey;

public class UsbException extends Exception{
	
	private static final long serialVersionUID = 1L; 
	
	public UsbException() {   
		super();   
	}   
	public UsbException(String msg) {  
        super(msg);  
    } 
	public UsbException(Throwable throwable) {
		    super(throwable);
	}

	public UsbException(String detailMessage, Throwable throwable) {
		    super(detailMessage, throwable);
	}
}
