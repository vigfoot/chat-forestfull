package com.forestfull.admin;

import com.forestfull.chat.ChatDTO;
import com.forestfull.chat.message.ChatMessageService;
import com.forestfull.chat.room.ChatRoomService;
import com.forestfull.common.CommonResponse;
import com.forestfull.common.file.FILE_TYPE;
import com.forestfull.common.file.FileDTO;
import com.forestfull.common.file.FileService;
import com.forestfull.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final FileService fileService;
    private final ChatRoomService chatRoomService;
    private final AdminUserService adminUserService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatMessageService chatMessageService;

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

    // 🚩 HTML 태그 생성 유틸리티
    private String createMediaHtml(FileDTO fileDto, String fileType, String caption) {
        String url = "/file/" + fileDto.getDirectory(); // FileService의 addResourceHandlers 설정 경로 반영
        String html = "";

        if (FILE_TYPE.IMAGE.name().equals(fileType)) {
            // 이미지 태그 (클릭 시 원본 보기 등을 위해 클래스 추가 권장)
            html = String.format("<img src=\"%s\" alt=\"%s\" class=\"file-image\" loading=\"lazy\">", url, fileDto.getName());
        } else if (FILE_TYPE.VIDEO.name().equals(fileType)) {
            // 비디오 태그 (controls 필수)
            html = String.format("<video src=\"%s\" controls class=\"file-video\"></video>", url);
        }

        // 캡션이 있다면 HTML 하단에 추가 (클라이언트에서 줄바꿈 처리 필요)
        if (StringUtils.hasText(caption)) {
            html += "<p class=\"file-caption\">" + caption + "</p>";
        }

        // 🚨 중요: 캡션이나 HTML 자체에 XSS 공격 위험이 있으므로, 프론트엔드에서 메시지를 렌더링할 때
        // 이 메시지(HTML)는 escape 없이 raw로 innerHTML/jQuery.html()로 삽입해야 합니다.
        // 일반 텍스트 메시지와 파일 메시지를 구분하는 플래그/로직이 필요합니다.

        return html;
    }

    @PostMapping(value = "/file/upload-chat/{roomId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse> uploadFileAndSendMessage(
            @PathVariable("roomId") Long roomId,
            @RequestPart("file") MultipartFile filePart,
            @RequestPart(name = "message", required = false) String caption, // 캡션을 message 필드로 사용
            Principal principal) throws AccessDeniedException {

        // ... (인증 및 사용자 정보 추출 로직 생략 - User user 획득) ...
        if (!(principal instanceof UsernamePasswordAuthenticationToken token)) {
            throw new AccessDeniedException("Access denied: Invalid principal token.");
        }
        Object userDetails = token.getPrincipal();
        if (!(userDetails instanceof User user)) {
            throw new AccessDeniedException("Access denied: Invalid user details.");
        }

        // --- 1. 파일 타입 검증 ---
        final String contentType = filePart.getContentType();
        String fileType = null;
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(filePart.getOriginalFilename()));

        if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            return ResponseEntity.badRequest().body(CommonResponse.fail("Unsupported file type. Only images and videos are allowed."));
        }
        if (contentType.startsWith("image/")) fileType = FILE_TYPE.IMAGE.name();
        if (contentType.startsWith("video/")) fileType = FILE_TYPE.VIDEO.name();

        // --- 2. 파일 저장 및 저장된 DTO 획득 ---
        // 🚨 FileService.saveFile은 성공 시 FileDTO를 CommonResponse.getData()에 담아 반환해야 합니다.
        final CommonResponse commonResponse = fileService.saveFile(filePart, fileType, originalFilename);

        if (!commonResponse.isSuccess() || commonResponse.getData() == null) {
            return ResponseEntity.internalServerError().body(commonResponse);
        }

        FileDTO savedFileDTO = (FileDTO) commonResponse.getData();

        // --- 3. HTML 태그 생성 ---
        final String mediaHtmlMessage = createMediaHtml(savedFileDTO, fileType, caption);

        // --- 4. 메시지 DTO 생성 및 저장 ---
        final ChatDTO.Message message = ChatDTO.Message.builder()
                .roomId(roomId)
                .user(user)
                .message(mediaHtmlMessage) // 🚩 HTML 태그를 메시지 본문에 담음
                .type(ChatDTO.Message.MessageType.TALK)
                .build();

        ChatDTO.Message saved = chatMessageService.saveMessage(message);

        message.setCreatedAt(LocalDateTime.now(Clock.systemUTC()));
        // --- 5. WebSocket 브로드캐스트 ---
        simpMessagingTemplate.convertAndSend("/topic/rooms/" + saved.getRoomId(), saved);

        return ResponseEntity.ok(CommonResponse.ok());
    }

    @PostMapping("/chat/rooms")
    public ResponseEntity<ChatDTO.Room> createRoom(@RequestBody ChatDTO.Room roomInfo, @AuthenticationPrincipal User user) {
        if (!StringUtils.hasText(roomInfo.getName())) return ResponseEntity.badRequest().build();

        ChatDTO.Room created = chatRoomService.createRoom(roomInfo.getName(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}