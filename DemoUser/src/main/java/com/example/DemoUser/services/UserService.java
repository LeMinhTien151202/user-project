package com.example.DemoUser.services;

import com.example.DemoUser.componnents.JwtTokenUtils;
import com.example.DemoUser.exceptions.DataNotFoundException;
import com.example.DemoUser.models.User;
import com.example.DemoUser.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService{
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtils jwtTokenUtil;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) throws Exception {
        return userRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Cannot find user with id: "+id));
    }

    public User saveWithAvatar(User user, MultipartFile avatarFile) throws IOException{
        // Xử lý upload file ảnh nếu có
        if (avatarFile != null && !avatarFile.isEmpty()) {
            // Tạo thư mục uploads nếu chưa tồn tại
            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs();
            }
            // Xóa file ảnh cũ nếu tồn tại
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                File oldFile = new File(user.getAvatar().substring(1)); // Bỏ dấu "/" đầu tiên
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }
            // Tạo tên file duy nhất để tránh trùng lặp
            String fileName = UUID.randomUUID() + "_" + avatarFile.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, fileName);

            // Lưu file vào thư mục uploads
            Files.write(filePath, avatarFile.getBytes());

            // Lưu đường dẫn file vào cột thumbnail
            user.setAvatar("/" + uploadDir + fileName);
        }
        return user;
    }

    @Override
    public User saveUserWithAvatar(User user, MultipartFile avatarFile) throws IOException {
        User userNew = saveWithAvatar(user,avatarFile);
        // **Mã hóa mật khẩu**
        String password = userNew.getPassword();
        String encodedPassword = passwordEncoder.encode(password);
        userNew.setPassword(encodedPassword);

        // Lưu book vào cơ sở dữ liệu
        return userRepository.save(userNew);
    }

    @Override
    public User updateUser(Long id, User user, MultipartFile avatarFile) throws Exception {
        User existingUserOpt = userRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Cannot find user with id: "+id));
        user.setAvatar(existingUserOpt.getAvatar());
        User userNew = saveWithAvatar(user,avatarFile);
        existingUserOpt.setUsername(user.getUsername());
        existingUserOpt.setEmail(user.getEmail());
        existingUserOpt.setPhone(user.getPhone());
        existingUserOpt.setPassword(user.getPassword());
        existingUserOpt.setDateOfBirth(user.getDateOfBirth());
        existingUserOpt.setActive(user.isActive());
        existingUserOpt.setAvatar(userNew.getAvatar());
        return userRepository.save(existingUserOpt);
    }

    @Override
    public String login(String username, String password, String roleId) throws Exception {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            throw new Exception("Invalid code or password");
        }
        User existingUser = optionalUser.get();

        // **Kiểm tra mật khẩu**
        if (!passwordEncoder.matches(password, existingUser.getPassword())) {
            throw new BadCredentialsException("Invalid code or password");
        }

        // Kiểm tra role
//        Optional<Role> optionalRole = roleRepository.findById(roleId);
        if (!roleId.equals(existingUser.getRole())) {
            throw new Exception("Role does not exist");
        }

        if (!existingUser.isActive()) {
            throw new Exception("User is locked");
        }

        // Xác thực với Spring Security
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                username, password, existingUser.getAuthorities()
        );
        authenticationManager.authenticate(authenticationToken);

        // Tạo token
        return jwtTokenUtil.generateToken(existingUser);
    }



    @Override
    public User getUserDetailsFromToken(String token) throws Exception {
        if (jwtTokenUtil.isTokenExpired(token)) {
            throw new Exception("Token is expired");
        }
        String userName = jwtTokenUtil.extractCode(token);
        Optional<User> user = userRepository.findByUsername(userName);
        if (user.isPresent()) {
            return user.get();
        } else {
            throw new Exception("User not found");
        }
    }

    @Override
    public void deleteUser(Long id) throws DataNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Cannot find user with id: "+id));
        user.setActive(false);
        userRepository.save(user);
    }


}
