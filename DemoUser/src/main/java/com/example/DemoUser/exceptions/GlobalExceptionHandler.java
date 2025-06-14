package com.example.DemoUser.exceptions;

import com.example.DemoUser.responses.ResponseObject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ResponseObject> handleGeneralException(Exception exception) {
        return ResponseEntity.internalServerError().body(
                ResponseObject.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .message(exception.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(DataNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<?> handleResourceNotFoundException(DataNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseObject.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(exception.getMessage())
                .build());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseObject> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        String firstErrorMessage = null;

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
            if (firstErrorMessage == null) {
                firstErrorMessage = error.getDefaultMessage();
            }
        }

        String responseMessage;
        if (errors.size() == 1 && firstErrorMessage != null) {
            responseMessage = firstErrorMessage;
        } else {
            responseMessage = "One or more validation errors occurred. See 'data' for details.";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseObject.builder()
                        .message(responseMessage)
                        .status(HttpStatus.BAD_REQUEST)
                        .data(errors)
                        .build());
    }
    /**
     * Handler mới cho ConstraintViolationException (validation ở Persistence Layer).
     * Bắt các lỗi validation xảy ra khi entity được persist/update vào DB.
     *
     * @param ex Ngoại lệ ConstraintViolationException được ném ra.
     * @return ResponseEntity chứa ResponseObject với thông báo lỗi và status BAD_REQUEST.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseObject> handleConstraintViolationException(
            ConstraintViolationException ex) {

        Map<String, String> errors = new HashMap<>();
        String firstErrorMessage = null;
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();

        // Lặp qua các vi phạm để thu thập tất cả và lấy thông báo lỗi đầu tiên
        for (ConstraintViolation<?> violation : violations) {
            String fieldName = violation.getPropertyPath().toString();
            // Lấy tên trường cuối cùng nếu propertyPath là chuỗi dài (ví dụ: "book.title")
            if (fieldName.contains(".")) {
                fieldName = fieldName.substring(fieldName.lastIndexOf('.') + 1);
            }
            errors.put(fieldName, violation.getMessage());

            if (firstErrorMessage == null) {
                firstErrorMessage = violation.getMessage();
            }
        }

        String responseMessage;
        // Nếu chỉ có một lỗi, trả về thông báo lỗi đó trực tiếp
        if (errors.size() == 1 && firstErrorMessage != null) {
            responseMessage = firstErrorMessage;
        } else {
            // Nếu có nhiều lỗi, trả về thông báo chung và chi tiết trong 'data'
            responseMessage = "One or more validation errors occurred during persistence. See 'data' for details.";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseObject.builder()
                        .message(responseMessage)
                        .status(HttpStatus.BAD_REQUEST)
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseObject> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String rootCauseMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        String userFriendlyMessage = "An unexpected database error occurred."; // Thông báo mặc định

        // --- Xử lý lỗi UNIQUE CONSTRAINT (MySQL/MariaDB) ---
        // Ví dụ: Duplicate entry 'tien le1' for key 'users.username'
        Pattern mysqlPattern = Pattern.compile("Duplicate entry '(.*?)' for key '(.*?)'");
        Matcher mysqlMatcher = mysqlPattern.matcher(rootCauseMessage);

        if (mysqlMatcher.find()) {
            String duplicateValue = mysqlMatcher.group(1);
            String keyName = mysqlMatcher.group(2);

            if (keyName.contains("username")) {
                userFriendlyMessage = "Tên người dùng '" + duplicateValue + "' đã tồn tại.";
            } else if (keyName.contains("email")) {
                userFriendlyMessage = "Email '" + duplicateValue + "' đã tồn tại.";
            } else if (keyName.contains("phone")) {
                userFriendlyMessage = "Số điện thoại '" + duplicateValue + "' đã tồn tại.";
            } else {
                userFriendlyMessage = "Giá trị trùng lặp cho trường " + keyName.replace("users.", "") + ": " + duplicateValue;
            }
        }
        // --- Xử lý lỗi UNIQUE CONSTRAINT (PostgreSQL) ---
        // Ví dụ: ERROR: duplicate key value violates unique constraint "users_username_key"
        // Detail: Key (username)=(tien le1) already exists.
//        else if (rootCauseMessage.contains("violates unique constraint")) {
//            Pattern pgPattern = Pattern.compile("Key \\((.*?)\\)=\\((.*?)\\) already exists\\.");
//            Matcher pgMatcher = pgPattern.matcher(rootCauseMessage);
//            if (pgMatcher.find()) {
//                String fieldName = pgMatcher.group(1);
//                String value = pgMatcher.group(2);
//
//                if ("username".equals(fieldName)) {
//                    userFriendlyMessage = "Tên người dùng '" + value + "' đã tồn tại.";
//                } else if ("email".equals(fieldName)) {
//                    userFriendlyMessage = "Email '" + value + "' đã tồn tại.";
//                } else if ("phone".equals(fieldName)) {
//                    userFriendlyMessage = "Số điện thoại '" + value + "' đã tồn tại.";
//                } else {
//                    userFriendlyMessage = "Giá trị " + value + " cho trường " + fieldName + " đã tồn tại.";
//                }
//            } else {
//                userFriendlyMessage = "Một mục nhập duy nhất đã tồn tại. Vui lòng kiểm tra lại thông tin bạn nhập.";
//            }
//        }
        // --- Có thể thêm xử lý cho các loại lỗi DataIntegrityViolation khác (ví dụ: NOT NULL, FOREIGN KEY) ---
        // Dựa vào `rootCauseMessage` để phân tích và trả về thông báo phù hợp.
        // Ví dụ: nếu muốn xử lý lỗi NOT NULL, bạn có thể tìm chuỗi "violates not-null constraint"

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseObject.builder()
                        .message(userFriendlyMessage)
                        .status(HttpStatus.BAD_REQUEST)
                        .data(null)
                        .build());
    }
}
