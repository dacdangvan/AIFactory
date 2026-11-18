package com.vib.cms.common.exception;
import com.vib.cms.common.api.ApiResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class GlobalExceptionHandler {
 @ExceptionHandler(ResourceNotFoundException.class) ResponseEntity<?> notFound(ResourceNotFoundException e){return ResponseEntity.status(404).body(ApiResponse.failure("NOT_FOUND",e.getMessage()));}
 @ExceptionHandler(DuplicateKeyException.class) ResponseEntity<?> conflict(DuplicateKeyException e){return ResponseEntity.status(409).body(ApiResponse.failure("CONFLICT","A record with the same unique key already exists"));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> invalid(MethodArgumentNotValidException e){String m=e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).findFirst().orElse("Invalid request");return ResponseEntity.badRequest().body(ApiResponse.failure("VALIDATION_ERROR",m));}
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> badRequest(IllegalArgumentException e){return ResponseEntity.badRequest().body(ApiResponse.failure("BAD_REQUEST",e.getMessage()));}
}
