package fr.gouv.culture.francetransfert.application.error;

import lombok.Data;

@Data
public class MaxTryException extends RuntimeException {

	private String id;

	/**
	 * Unauthorized Access Exception
	 * 
	 * @param msg
	 */
	public MaxTryException(String msg) {
		super(msg);
	}

	public MaxTryException(String msg, String id) {
		super(msg);
		this.id = id;
	}
}
