package fr.gouv.culture.francetransfert.application.error;

public class MaxTryException extends RuntimeException {
    /**
     * Unauthorized Access Exception
     * @param msg
     */
    public MaxTryException(String msg){
        super(msg);
    }
}
