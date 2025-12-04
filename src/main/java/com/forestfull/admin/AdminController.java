package com.forestfull.admin;

import com.forestfull.common.ResponseException;
import com.forestfull.common.file.FILE_TYPE;
import com.forestfull.common.file.FileService;
import com.forestfull.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    public ResponseEntity<?> updateRoles(@PathVariable String username,
                                         @RequestBody Map<String, String> body) {
        String roles = body.get("roles");
        adminUserService.updateUserRoles(username, roles);
        return ResponseEntity.ok(Map.of("message", "Roles updated"));
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        adminUserService.deleteUser(username);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    @PostMapping(value = "/emoji/{filename}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ResponseException>> saveEmoji(
            @RequestPart("file") Mono<FilePart> filePartMono,
            @PathVariable String filename) {

        return filePartMono.flatMap(filePart ->
                fileService.saveFile(filePart, FILE_TYPE.EMOJI.name(), filename)
                        .map(result -> result.isSuccess()
                                ? ResponseEntity.ok(result)
                                : ResponseEntity.badRequest().body(result))
        );
    }

    @DeleteMapping("/emoji/{id}")
    Mono<ResponseEntity<ResponseException>> deleteEmoji(@PathVariable Long id) {
        final ResponseException result = fileService.deleteFile(id);
        return Mono.just(result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result));
    }
}