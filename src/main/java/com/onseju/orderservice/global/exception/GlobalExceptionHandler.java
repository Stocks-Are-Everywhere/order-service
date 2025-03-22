package com.onseju.orderservice.global.exception;

import com.onseju.orderservice.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    // Handle all exceptions
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        log.error("[{}] error occurred: {}",
            ex.getClass().getSimpleName(),
            ex.getMessage()
        );

        return ResponseEntity
            .status(ex.getStatus())
            .body(new ApiResponse<>(
                ex.getMessage(),
                null,
                ex.getStatus().value()
            ));
    }

    
    // Handle unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(
                "서버 오류가 발생했습니다",
                null,
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            ));
    }
}
