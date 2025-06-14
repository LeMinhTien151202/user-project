package com.example.DemoUser.Controllers;

import com.example.DemoUser.dtos.UserLoginDTO;
import com.example.DemoUser.exceptions.DataNotFoundException;
import com.example.DemoUser.models.User;
import com.example.DemoUser.responses.LoginResponse;
import com.example.DemoUser.responses.ResponseObject;
import com.example.DemoUser.services.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final IUserService userService;

    @GetMapping("")
    public ResponseEntity<?> getUserAll() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Get list of user successfully")
                .status(HttpStatus.OK)
                .data(users)
                .build());
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") Long userId) throws Exception {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Get user by id successfully")
                .status(HttpStatus.OK)
                .data(user)
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody UserLoginDTO userLoginDTO
    ) {
        // Kiểm tra thông tin đăng nhập và sinh token
        try {
            String token = userService.login(
                    userLoginDTO.getUserName(),
                    userLoginDTO.getPassword(),
                    userLoginDTO.getRole() == null ? "user" : userLoginDTO.getRole()
            );
            User user = userService.getUserDetailsFromToken(token);
            return ResponseEntity.ok(LoginResponse.builder()
                    .message("login successfully")
                    .token(token)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value="/register",consumes = {"multipart/form-data"})
    public ResponseEntity<ResponseObject> createUser(
            @ModelAttribute @Valid User user,
            BindingResult result,
            @RequestParam(value = "avatar", required = false) MultipartFile avatarlFile
    ) throws IOException {
        User savedUser = userService.saveUserWithAvatar(user, avatarlFile);
        return ResponseEntity.ok(
                ResponseObject.builder()
                        .message("Create new book successfully")
                        .status(HttpStatus.CREATED)
                        .data(savedUser)
                        .build());
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ResponseObject> updateUser(
            @PathVariable("id") Long userId,
            @ModelAttribute @Valid User user,
            BindingResult result,
            @RequestParam(value = "avatar", required = false) MultipartFile avatarlFile
    ) throws Exception {
        User savedUser = userService.updateUser(userId,user, avatarlFile);
        return ResponseEntity.ok(
                ResponseObject.builder()
                        .message("Create new book successfully")
                        .status(HttpStatus.CREATED)
                        .data(savedUser)
                        .build());
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping("/details")
    public ResponseEntity<ResponseObject> getUserDetails(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        try {
            String extractedToken = authorizationHeader.substring(7); // Loại bỏ "Bearer " từ chuỗi token
            User user = userService.getUserDetailsFromToken(extractedToken);
            return ResponseEntity.ok(ResponseObject.builder()
                    .message("get detail user successfully")
                    .status(HttpStatus.CREATED)
                    .data(user)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<ResponseObject> deleteUser(@PathVariable("id") Long userId) throws DataNotFoundException {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("delete user successfully")
                .status(HttpStatus.OK)
                .build());
    }
}
