package fr.gouv.culture.francetransfert.application.error;

import lombok.Getter;

/**
 * Exception used in access treatment
 *
 */
@Getter
public class UnauthorizedAccessException extends RuntimeException {

	private String type;

	/**
	 * Unauthorized Access Exception
	 * 
	 * @param msg
	 */
	public UnauthorizedAccessException(String msg) {
		super(msg);
		this.type = ErrorEnum.WRONG_PASSWORD.getValue();
	}

	public UnauthorizedAccessException(String msg, String type) {
		super(msg);
		this.type = type;
	}
}
