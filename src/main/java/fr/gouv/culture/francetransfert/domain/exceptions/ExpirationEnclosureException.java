package fr.gouv.culture.francetransfert.domain.exceptions;

public class ExpirationEnclosureException extends RuntimeException {

    /**
     * throw business domain exception
     * @param message
     */
    public ExpirationEnclosureException(String message) {
        super( message);
    }
}
