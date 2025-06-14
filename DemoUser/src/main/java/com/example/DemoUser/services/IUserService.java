package com.example.DemoUser.services;

import com.example.DemoUser.exceptions.DataNotFoundException;
import com.example.DemoUser.models.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IUserService {
    List<User> getAllUsers();
    User getUserById(Long id) throws Exception;
    String login(String phoneNumber, String password, String roleId) throws Exception;
    User getUserDetailsFromToken(String token) throws Exception;
    User updateUser(Long id, User user, MultipartFile avatarFile) throws Exception;
    User saveUserWithAvatar(User user, MultipartFile thumbnailFile) throws IOException;
    void deleteUser(Long id) throws DataNotFoundException;
}
