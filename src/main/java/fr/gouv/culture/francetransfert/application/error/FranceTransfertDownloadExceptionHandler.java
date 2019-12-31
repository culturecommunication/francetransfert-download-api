package fr.gouv.culture.francetransfert.application.error;


import com.amazonaws.SdkClientException;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import fr.gouv.culture.francetransfert.domain.exceptions.BusinessDomainException;
import fr.gouv.culture.francetransfert.domain.exceptions.DomainNotFoundException;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The type StarterKit exception handler.
 * @author Open Group
 * @since 1.0.0
 */
@ControllerAdvice
public class FranceTransfertDownloadExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FranceTransfertDownloadExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

        return new ResponseEntity<>(new ApiError(status.value(), "NOT FOUND"), status);
    }


    @ExceptionHandler(DomainNotFoundException.class)
    public ResponseEntity<Object>  handleDomainNotFoundException(Exception ex)  {
        LOGGER.error(ex.getMessage());
        return new ResponseEntity<>(new ApiError(HttpStatus.NOT_FOUND.value(),ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessDomainException.class)
    public ResponseEntity<Object>  handleBusinessDomainException(Exception ex)  {
        LOGGER.error(ex.getMessage());
        return new ResponseEntity<>(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(),ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({AccessDeniedException.class,JWTDecodeException.class,JWTCreationException.class, })
    public ResponseEntity<Object>  handleUnauthorizedException(Exception ex)  {
        LOGGER.error(ex.getMessage());
        return new ResponseEntity<>(new ApiError(HttpStatus.UNAUTHORIZED.value(),ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(SdkClientException.class)
    public ResponseEntity<Object>  handleSdkClientException(Exception ex)  {
        LOGGER.error(ex.getMessage());
        return new ResponseEntity<>(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(),ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DownloadException.class)
    public ResponseEntity<Object>  handleDownloadException(Exception ex)  {
        LOGGER.error(ex.getMessage());
        return new ResponseEntity<>(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(),ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Object>  handleUnauthorizedAccessException(Exception ex)  {
        LOGGER.error(ex.getMessage());
        return new ResponseEntity<>(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(),ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
