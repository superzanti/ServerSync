package com.superzanti.serversync.util.errors;

import com.superzanti.serversync.util.enums.EErrorType;

import java.io.Serializable;

public class MessageError implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4141407496772125650L;
	public String message;
	public final EErrorType type;
	
	public MessageError(String message, EErrorType type) {
		this.message = message;
		this.type = type;
	}
}
