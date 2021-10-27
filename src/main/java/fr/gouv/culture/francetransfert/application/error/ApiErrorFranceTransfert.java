package fr.gouv.culture.francetransfert.application.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ApiErrorFranceTransfert {
    private HttpStatus status;
    private String message;
    private Map<String,String> errors;

    public ApiErrorFranceTransfert(HttpStatus status, String message, Map<String,String> errors) {
        super();
        this.status = status;
        this.message = message;
        this.errors = errors;
    }
}
