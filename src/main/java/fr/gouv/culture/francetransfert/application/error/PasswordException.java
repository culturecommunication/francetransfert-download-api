package fr.gouv.culture.francetransfert.application.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PasswordException extends RuntimeException {
    private String type;
    private String id;
    private int count;
}
