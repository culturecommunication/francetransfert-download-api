package com.opengroup.jsbapi.application.error;

/**
 * Exception used in access treatment
 *
 */
public class UnauthorizedAccessException extends RuntimeException  {


    /**
     * Unauthorized Access Exception
     * @param msg
     */
    public UnauthorizedAccessException(String msg){
        super(msg);
    }
}
