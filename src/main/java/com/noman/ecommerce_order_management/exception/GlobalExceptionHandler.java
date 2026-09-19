package com.noman.ecommerce_order_management.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /*
     * Handles @Valid failures.
     *
     * Example:
     *
     * {
     *   "skuId": "SKU id is required",
     *   "quantity": "Quantity must be at least 1"
     * }
     */
    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(error -> {

                    String message =
                            error.getDefaultMessage()
                                    != null
                                    ? error.getDefaultMessage()
                                    : "Invalid value";

                    /*
                     * If multiple validators fail for
                     * the same field, return the first
                     * useful error instead of replacing it.
                     */
                    validationErrors.putIfAbsent(
                            error.getField(),
                            message
                    );
                });

        exception
                .getBindingResult()
                .getGlobalErrors()
                .forEach(error -> {

                    String message =
                            error.getDefaultMessage()
                                    != null
                                    ? error.getDefaultMessage()
                                    : "Invalid request";

                    validationErrors.putIfAbsent(
                            "_global",
                            message
                    );
                });

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                validationErrors
        );
    }

    /*
     * Handles the ResponseStatusException instances
     * already used throughout our service layer.
     *
     * Example:
     *
     * throw new ResponseStatusException(
     *     HttpStatus.NOT_FOUND,
     *     "Product not found"
     * );
     */
    @ExceptionHandler(
            ResponseStatusException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {

        HttpStatus status =
                HttpStatus.resolve(
                        exception
                                .getStatusCode()
                                .value()
                );

        if (status == null) {

            status =
                    HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message =
                exception.getReason();

        if (message == null
                || message.isBlank()) {

            message =
                    status.getReasonPhrase();
        }

        return buildResponse(
                status,
                message,
                request,
                Map.of()
        );
    }

    /*
     * Invalid JSON or invalid enum values.
     *
     * Examples:
     *
     * {
     *   "paymentMethod": "INVALID_METHOD"
     * }
     *
     * or incomplete/broken JSON.
     */
    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleMalformedJson(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                request,
                Map.of()
        );
    }

    /*
     * Example:
     *
     * /products/abc
     *
     * when productId must be Long.
     */
    @ExceptionHandler(
            MethodArgumentTypeMismatchException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {

        String message =
                "Invalid value for parameter '"
                        + exception.getName()
                        + "'";

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request,
                Map.of()
        );
    }

    /*
     * Handles required query parameters when one
     * is missing.
     */
    @ExceptionHandler(
            MissingServletRequestParameterException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {

        String message =
                "Required request parameter '"
                        + exception.getParameterName()
                        + "' is missing";

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request,
                Map.of()
        );
    }

    /*
     * Protects the API from returning raw database
     * exceptions for unique/FK constraint conflicts.
     *
     * Service code should still detect expected
     * business conflicts explicitly.
     */
    @ExceptionHandler(
            DataIntegrityViolationException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Database constraint conflict on {}",
                request.getRequestURI()
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                "Request conflicts with existing data",
                request,
                Map.of()
        );
    }

    /*
     * Unknown endpoint/resource.
     */
    @ExceptionHandler(
            NoResourceFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleResourceNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                request,
                Map.of()
        );
    }

    /*
     * Example:
     *
     * POST endpoint called using GET,
     * or vice versa.
     */
    @ExceptionHandler(
            HttpRequestMethodNotSupportedException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {

        String method =
                exception.getMethod();

        String message =
                "HTTP method "
                        + method
                        + " is not supported for this endpoint";

        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                message,
                request,
                Map.of()
        );
    }

    /*
     * Example:
     *
     * API expects JSON but the caller sends an
     * unsupported Content-Type.
     */
    @ExceptionHandler(
            HttpMediaTypeNotSupportedException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content type is not supported",
                request,
                Map.of()
        );
    }

    /*
     * Last-resort protection.
     *
     * Never expose stack traces, SQL messages,
     * passwords, implementation details, etc.
     */
    @ExceptionHandler(
            Exception.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {

        log.error(
                "Unexpected error while processing {}",
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred",
                request,
                Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse>
    buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> validationErrors
    ) {

        ApiErrorResponse response =
                ApiErrorResponse.builder()
                        .timestamp(
                                LocalDateTime.now()
                        )
                        .status(
                                status.value()
                        )
                        .error(
                                status.getReasonPhrase()
                        )
                        .message(
                                message
                        )
                        .path(
                                request.getRequestURI()
                        )
                        .validationErrors(
                                validationErrors
                        )
                        .build();

        return ResponseEntity
                .status(status)
                .body(response);
    }
}