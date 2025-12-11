package com.forestfull.admin;

import com.forestfull.common.CommonResponse;
import com.forestfull.common.file.FILE_TYPE;
import com.forestfull.common.file.FileService;
import com.forestfull.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @PutMapping("/users/{id}/roles")
    ResponseEntity<?> updateRoles(@PathVariable Long id,
                                  @RequestBody Map<String, String> body) {
        return adminUserService.updateUserRoles(id, body.get("roles")) ? ResponseEntity.ok(Map.of("message", "Roles updated")) : ResponseEntity.internalServerError().build();
    }

    @DeleteMapping("/users/{id}")
    ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return adminUserService.deleteUser(id) ? ResponseEntity.ok(Map.of("message", "User deleted")) : ResponseEntity.internalServerError().build();
    }

    @PostMapping(value = "/emoji/{filename}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse> saveEmoji(@RequestPart("file") MultipartFile filePart, @PathVariable String filename) {
        if (filePart.isEmpty()) return ResponseEntity.badRequest().body(CommonResponse.fail("empty"));

        CommonResponse commonResponse = fileService.saveFile(filePart, FILE_TYPE.EMOJI.name(), filename);
        return commonResponse.isSuccess()
                ? ResponseEntity.ok(commonResponse)
                : ResponseEntity.badRequest().body(commonResponse);
    }

    @DeleteMapping("/emoji/{id}")
    ResponseEntity<CommonResponse> deleteEmoji(@PathVariable Long id) {
        final CommonResponse result = fileService.deleteFile(id);
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }
}