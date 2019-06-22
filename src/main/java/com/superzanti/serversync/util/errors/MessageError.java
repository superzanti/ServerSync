package com.superzanti.serversync.util.errors;

import java.io.Serializable;

import com.superzanti.serversync.util.enums.EErrorType;

public class MessageError implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4141407496772125650L;
	public String message;
	public EErrorType type;
	
	public MessageError(String message, EErrorType type) {
		this.message = message;
		this.type = type;
	}
}
