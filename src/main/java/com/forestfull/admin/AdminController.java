package com.forestfull.admin;

import com.forestfull.common.ResponseException;
import com.forestfull.common.file.FILE_TYPE;
import com.forestfull.common.file.FileService;
import com.forestfull.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final FileService fileService;

    private final AdminUserService adminUserService;

    @GetMapping("/users")
    public List<User> listUsers() {
        return adminUserService.getAllUsers();
    }

    @PutMapping("/users/{username}/roles")
    ResponseEntity<?> updateRoles(@PathVariable String username,
                                  @RequestBody Map<String, String> body) {
        return adminUserService.updateUserRoles(username, body.get("roles")) ? ResponseEntity.ok(Map.of("message", "Roles updated")) : ResponseEntity.internalServerError().build();
    }

    @DeleteMapping("/users/{username}")
    ResponseEntity<?> deleteUser(@PathVariable String username) {
        return adminUserService.deleteUser(username) ? ResponseEntity.ok(Map.of("message", "User deleted")) : ResponseEntity.internalServerError().build();
    }

    @PostMapping(value = "/emoji/{filename}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ResponseException> saveEmoji(
            @RequestPart("file") MultipartFile filePart,
            @PathVariable String filename) {

        ResponseException responseException = fileService.saveFile(filePart, FILE_TYPE.EMOJI.name(), filename);

        return responseException.isSuccess() ? ResponseEntity.ok(responseException) : ResponseEntity.badRequest().body(responseException);
    }

    @DeleteMapping("/emoji/{id}")
    ResponseEntity<ResponseException> deleteEmoji(@PathVariable Long id) {
        final ResponseException result = fileService.deleteFile(id);
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }
}