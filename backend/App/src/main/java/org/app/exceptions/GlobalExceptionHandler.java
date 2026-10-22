package org.app.exceptions;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.app.utils.MessageResolverI18n;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.http.fileupload.impl.InvalidContentTypeException;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.app.exceptions.ErrorResponse.of;
import static org.app.utils.Response.badRequest;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.status;

@Log4j2
@ControllerAdvice
public class GlobalExceptionHandler {
    private final MessageResolverI18n resolver;

    public GlobalExceptionHandler(MessageResolverI18n resolver) {
        this.resolver = resolver;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Error en GlobalExceptionHandler", ex);
        return badRequest().body(of(resolver.get("exception.internal-server-error")));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
       log.info("Error en GlobalExceptionHandler autenticacion", ex);
        var status = UNAUTHORIZED;
        var response = status(status);
        if (ex instanceof BadCredentialsException) {
            return response.body(of(resolver.get("authentication.invalid-credentials"), status));
        }
        return response.body(of(ex.getMessage(), status));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        var constraintViolations = ex.getConstraintViolations();
        var errors = new ValidationErrorResponse();
        constraintViolations.forEach(constraintViolation -> {
            var path = constraintViolation.getPropertyPath().toString().split("\\.");
            var param = path[path.length - 1];
            errors.addError(param, constraintViolation.getMessage());
        });

        return badRequest().body(errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        return badRequest().body(of(resolver.get("exception.missing-request-parameter", ex.getParameterName())));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPartException(MissingServletRequestPartException ex) {
        return badRequest().body(of(resolver.get("exception.missing-request-parameter", ex.getRequestPartName())));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        return badRequest().body(of(resolver.get("exception.missing-request-header", ex.getHeaderName())));
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> handleMissingPathVariableException(MissingPathVariableException ex) {
        return badRequest().body(of(resolver.get("exception.missing-path-variable", ex.getVariableName())));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        return badRequest().body(of(resolver.get("exception.media-type-not-supported", ex.getContentType(), ex.getSupportedMediaTypes())));
    }

    @ExceptionHandler(InvalidContentTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidContentTypeException(InvalidContentTypeException ex) {
        return badRequest().body(of(resolver.get("exception.invalid-content-type")));
    }

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<Object> handleClientException(ClientException ex) {
        return status(ex.getHttpStatusCode()).headers(ex.getHeaders()).body(ex.getBody());
    }

    @ExceptionHandler(JWTDecodeException.class)
    public ResponseEntity<ErrorResponse> handleJWTDecodeException(JWTDecodeException ex) {
        return badRequest().body(of(resolver.get("jwt.invalid-token")));
    }

    @ExceptionHandler(SignatureVerificationException.class)
    public ResponseEntity<ErrorResponse> handleSignatureVerificationException(SignatureVerificationException ex) {
        return badRequest().body(of(resolver.get("jwt.invalid-signature")));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpiredException(TokenExpiredException ex) {
        return badRequest().body(of(resolver.get("jwt.expired-token")));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return badRequest().body(of(resolver.get("jwt.user-not-found")));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {

        var errors = new ValidationErrorResponse();

        ex.getBindingResult().getAllErrors().forEach((ObjectError error) -> {
            if (error instanceof FieldError errField) {
                errors.addError(errField.getField(), errField.getDefaultMessage());
            }
        });

        return badRequest().body(errors);
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException ex) {
        log.warn("Se recibió una excepción de SQLIntegrityConstraintViolationException, se recomienda implementar el método onSQLIntegrityConstraintViolationException de la clase   GlobalExcepcionHandler para evitar fugas de información del esquema de base de datos: {}", ex.getMessage());
        return badRequest().body(of("Error en la consistencia de datos"));
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotWritableException(HttpMessageNotWritableException ex) {
        return badRequest().body(of(resolver.get("exception.write-error")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error(ex);
        return badRequest().body(of(resolver.get("exception.read-error")));
    }


    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return badRequest().body(of(resolver.get("exception.method-not-supported", ex.getMethod())));
    }


    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        return status(HttpStatus.NOT_FOUND).body(of(ex.getResourcePath(), HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(TransientAiException.class)
    public ResponseEntity<ErrorResponse> handleTransientAiException(TransientAiException ex) {
        // TransientAiException representa errores temporales que fallaron después de los reintentos (incluyendo 429)
        String message = ex.getMessage();
        if (message != null && message.contains("429")) {
            return status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(of(resolver.get("exception.too-many-requests"), HttpStatus.TOO_MANY_REQUESTS));
        }
        return status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(of(resolver.get("exception.internal-server-error"), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(NonTransientAiException.class)
    public ResponseEntity<ErrorResponse> handleNonTransientAiException(NonTransientAiException ex) {
        // NonTransientAiException representa errores que no deben reintentarse (como 400, 401)
        return status(HttpStatus.BAD_REQUEST)
                .body(of(resolver.get("exception.bad-request"), HttpStatus.BAD_REQUEST));
    }


}