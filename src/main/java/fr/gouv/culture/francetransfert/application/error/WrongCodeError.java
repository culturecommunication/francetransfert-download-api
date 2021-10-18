package fr.gouv.culture.francetransfert.application.error;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WrongCodeError {
    /**
     * Http Status Code
     */
    private int statusCode;

    private int codeTryCount;

    private String message;

}
