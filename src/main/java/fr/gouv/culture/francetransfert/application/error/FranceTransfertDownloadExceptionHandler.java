package fr.gouv.culture.francetransfert.application.error;


import com.amazonaws.SdkClientException;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import fr.gouv.culture.francetransfert.domain.exceptions.BusinessDomainException;
import fr.gouv.culture.francetransfert.domain.exceptions.DomainNotFoundException;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import fr.gouv.culture.francetransfert.domain.exceptions.ExpirationEnclosureException;
import fr.gouv.culture.francetransfert.francetransfert_metaload_api.utils.RedisUtils;
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

import java.util.UUID;

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

        return new ResponseEntity<>(new ApiError(status.value(), "NOT FOUND", "NOT_FOUND"), status);
    }


    @ExceptionHandler(DomainNotFoundException.class)
    public ResponseEntity<Object>  handleDomainNotFoundException(Exception ex)  {
        String errorId = RedisUtils.generateGUID();
        LOGGER.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), errorId, ex.getMessage());
        return new ResponseEntity<>(new ApiError(HttpStatus.NOT_FOUND.value(), ErrorEnum.TECHNICAL_ERROR.getValue(), errorId), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({AccessDeniedException.class,JWTDecodeException.class,JWTCreationException.class, })
    public ResponseEntity<Object>  handleUnauthorizedException(Exception ex)  {
        String errorId = RedisUtils.generateGUID();
        LOGGER.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), errorId, ex.getMessage());
        return new ResponseEntity<>(new ApiError(HttpStatus.UNAUTHORIZED.value(), ErrorEnum.TECHNICAL_ERROR.getValue(), errorId), HttpStatus.UNAUTHORIZED);

    }

    @ExceptionHandler(BusinessDomainException.class)
    public ResponseEntity<Object>  handleBusinessDomainException(Exception ex)  {
         return generateError(ex, ErrorEnum.TECHNICAL_ERROR.getValue());
    }

    @ExceptionHandler(SdkClientException.class)
    public ResponseEntity<Object>  handleSdkClientException(Exception ex)  {
        return generateError(ex, ErrorEnum.TECHNICAL_ERROR.getValue());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Object>  handleUnauthorizedAccessException(Exception ex)  {
//        return generateError(ex, ErrorEnum.WRONG_PASSWORD.getValue());
        LOGGER.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), null, ex.getMessage());
        return new ResponseEntity<>(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorEnum.WRONG_PASSWORD.getValue(), ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ExpirationEnclosureException.class)
    public ResponseEntity<Object>  handleExpirationEnclosureException(Exception ex)  {
        LOGGER.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), null, ex.getMessage());
        return new ResponseEntity<>(new ApiError(HttpStatus.NOT_FOUND.value(), ErrorEnum.TECHNICAL_ERROR.getValue(), ex.getMessage()), HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(DownloadException.class)
    public ResponseEntity<Object>  handleDownloadException(DownloadException ex)  {
        LOGGER.error("Type: {} -- id: {} -- message: {}", ex.getType(), ex.getId(), ex.getMessage());
        return new ResponseEntity<>(new ApiError(HttpStatus.OK.value(), ex.getType(), ex.getId()), HttpStatus.OK);
    }

    private ResponseEntity<Object> generateError(Exception ex, String errorType) {
        String errorId = UUID.randomUUID().toString();
        LOGGER.error("Type: {} -- id: {} -- message: {}", errorType, errorId, ex.getMessage());
        return new ResponseEntity<>(new ApiError(HttpStatus.OK.value(), errorType, errorId), HttpStatus.OK);
    }

}
