package fr.gouv.culture.francetransfert.domain.exceptions;

/**
 * Class to throw a custom business exception
 */
public class BusinessDomainException extends RuntimeException {

	/**
	 * throw business domain exception
	 * 
	 * @param message
	 */
	public BusinessDomainException(String message) {
		super(message);
	}
}
