package com.forestfull.common.file;

import com.forestfull.common.ResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${file.directory.absolute}")
    private String absolutePath;

    private final FileMapper fileMapper;

    /**
     * 엄격한 파일명 검증: 허용되는 문자만, 경로 관련 문자 차단
     */
    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Filename must not be empty");
        }
        // 허용 문자: 영숫자, 마침표(.), 언더스코어(_), 하이픈(-)
        if (!filename.matches("[a-zA-Z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid filename format");
        }
        // 추가로 경로 조작을 위한 문자가 있는지 확인
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename: path traversal characters detected");
        }
        return filename;
    }

    /**
     * 베이스 경로(absolutePath)와 결합한 후 정규화(normalize)해서
     * 베이스 경로 밖으로 나가는지 확인합니다. 안전한 File 객체를 반환합니다.
     */
    private File safePath(String relativeOrAbsoluteDirectory) {
        Path base = Paths.get(absolutePath).toAbsolutePath().normalize();
        Path filePath = base.resolve(relativeOrAbsoluteDirectory).normalize();
        if (!filePath.startsWith(base)) {
            throw new SecurityException("Path traversal attempt detected");
        }
        return filePath.toFile();
    }

    public FileDTO getFileById(Long id) {
        return fileMapper.getFileById(id);
    }

    public List<FileDTO> getEmojiList(String emojiFileName) {
        String safeFilename = null;
        if (StringUtils.hasText(emojiFileName)) {
            safeFilename = sanitizeFilename(emojiFileName);
        }

        final List<FileDTO> emojiList = fileMapper.getEmojiList(safeFilename);
        if (Objects.isNull(emojiList) || emojiList.isEmpty()) return Collections.emptyList();

        return emojiList.stream()
                .peek(fileDTO -> {
                    try {
                        fileDTO.setFile(safePath(fileDTO.getDirectory()));
                    } catch (Exception ex) {
                        // 안전을 위해 문제가 있는 항목은 null 처리
                        fileDTO.setFile(null);
                    }
                })
                .toList();
    }

    /**
     * 안전한 파일 저장
     * - 파일명 검증
     * - Path.resolve + normalize
     * - basePath.startsWith 체크
     * - 폴더 생성은 Files.createDirectories 사용
     */
    public Mono<ResponseException> saveFile(FilePart filePart, String type, String fileName) {
        String safeFileName;
        try {
            safeFileName = sanitizeFilename(fileName);
        } catch (IllegalArgumentException ex) {
            return Mono.just(ResponseException.fail(ex.getMessage()));
        }

        final LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        final Path basePath = Paths.get(absolutePath).toAbsolutePath().normalize();

        // 디렉토리 구조를 Path 기반으로 안전하게 구성
        Path targetRelative = Paths.get("")
                .resolve(String.valueOf(now.getYear()))
                .resolve(String.valueOf(now.getMonth().getValue()))
                .resolve(String.valueOf(now.getDayOfMonth()))
                .resolve(UUID.randomUUID() + "_" + safeFileName);

        Path targetPath = basePath.resolve(targetRelative).normalize();
        if (!targetPath.startsWith(basePath)) {
            return Mono.just(ResponseException.fail("Invalid file path"));
        }

        try {
            // 상위 디렉토리 생성
            Files.createDirectories(targetPath.getParent());
        } catch (IOException e) {
            return Mono.just(ResponseException.fail("Failed to create directories: " + e.getMessage()));
        }

        // 실제 파일로 전송
        File dest = targetPath.toFile();

        return filePart.transferTo(dest)
                .then(Mono.fromRunnable(() -> {
                    // DB에는 베이스 경로를 제외한 상대 경로를 저장
                    String dbDirectory = basePath.relativize(targetPath).toString().replace('\\', '/');
                    fileMapper.saveFile(
                            FileDTO.builder()
                                    .type(type)
                                    .name(safeFileName)
                                    .directory(dbDirectory)
                                    .build()
                    );
                }))
                .thenReturn(ResponseException.ok())
                .onErrorResume(throwable -> {
                    // 전송 중 에러가 나면 파일을 지우는 시도
                    try {
                        if (dest.exists()) dest.delete();
                    } catch (Exception ignored) {
                    }
                    return Mono.just(ResponseException.fail("Failed to save file: " + throwable.getMessage()));
                });
    }

    public ResponseException deleteFile(Long id) {
        FileDTO fileById = getFileById(id);
        if (Objects.isNull(fileById)) return ResponseException.fail("Invalid file id");

        File file;
        try {
            file = safePath(fileById.getDirectory());
        } catch (SecurityException ex) {
            return ResponseException.fail("Invalid file path");
        }

        // DB 먼저 삭제하거나 트랜잭션 전략에 따라 조정
        fileMapper.deleteFile(id);

        try {
            if (file.exists()) Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            // 파일 삭제 실패는 로그로 남기고 실패 응답 반환 가능
            return ResponseException.fail("Failed to delete physical file: " + e.getMessage());
        }

        return ResponseException.ok();
    }
}
