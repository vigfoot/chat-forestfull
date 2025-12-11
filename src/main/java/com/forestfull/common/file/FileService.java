package com.forestfull.common.file;

import com.forestfull.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FileService implements WebMvcConfigurer {

    @Value("${file.directory.absolute}")
    private String absolutePath;

    private final FileMapper fileMapper;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/file/**")
                .addResourceLocations("file:" + absolutePath) // 실제 디렉토리 절대 경로
                .setCachePeriod(3600); // 선택: 캐시 1시간
    }

    /**
     * 엄격한 파일명 검증: 허용되는 문자만, 경로 관련 문자 차단
     */
    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename))
            return filename;

        if (!filename.matches("[\\w\\s._-]+"))
            throw new IllegalArgumentException("Invalid filename format");

        // 추가로 경로 조작을 위한 문자가 있는지 확인
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\"))
            throw new IllegalArgumentException("Invalid filename: path traversal characters detected");

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
        return fileMapper.getEmojiList(emojiFileName);
    }

    /**
     * 안전한 파일 저장
     * - 파일명 검증
     * - Path.resolve + normalize
     * - basePath.startsWith 체크
     * - 폴더 생성은 Files.createDirectories 사용
     */
    public CommonResponse saveFile(MultipartFile filePart, String type, String fileName) {
        String safeFileName;
        try {
            safeFileName = sanitizeFilename(fileName);
        } catch (IllegalArgumentException ex) {
            return CommonResponse.fail(ex.getMessage());
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
        if (!targetPath.startsWith(basePath)) return CommonResponse.fail("Invalid file path");


        try {
            // 상위 디렉토리 생성
            Files.createDirectories(targetPath.getParent());
            // 실제 파일로 전송
            File dest = targetPath.toFile();
            filePart.transferTo(dest);

            // DB에는 베이스 경로를 제외한 상대 경로를 저장
            String dbDirectory = basePath.relativize(targetPath).toString().replace('\\', '/');
            fileMapper.saveFile(
                    FileDTO.builder()
                            .type(type)
                            .name(safeFileName)
                            .directory(dbDirectory)
                            .build()
            );
            return CommonResponse.ok();
        } catch (IOException e) {
            return CommonResponse.fail("Failed to create directories: " + e.getMessage());
        } catch (Exception e) {
            return CommonResponse.fail("Failed to save file: " + e.getMessage());
        }
    }

    public CommonResponse deleteFile(Long id) {
        FileDTO fileById = getFileById(id);
        if (Objects.isNull(fileById)) return CommonResponse.fail("Invalid file id");

        File file;
        try {
            file = safePath(fileById.getDirectory());
        } catch (SecurityException ex) {
            return CommonResponse.fail("Invalid file path");
        }

        // DB 먼저 삭제하거나 트랜잭션 전략에 따라 조정
        fileMapper.deleteFile(id);

        try {
            if (file.exists()) Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            // 파일 삭제 실패는 로그로 남기고 실패 응답 반환 가능
            return CommonResponse.fail("Failed to delete physical file: " + e.getMessage());
        }

        return CommonResponse.ok();
    }

    // 🚩 MODIFIED: 프로필 이미지 저장을 위한 특화 메서드

    /**
     * 프로필 이미지를 저장하고 성공 시 DB ID를 반환합니다.
     *
     * @param filePart 업로드된 파일
     * @param userId   사용자 ID (FileDTO에 저장할 용도)
     * @return 저장 성공 시 FileDTO의 ID를, 실패 시 null을 반환합니다.
     */
    // FileService.java (수정된 saveProfileImage 메서드)
    public Long saveProfileImage(MultipartFile filePart, Long userId) {
        if (filePart == null || filePart.isEmpty()) return null;
        if (userId == null || userId <= 0) return null;

        // 1. 파일명 처리
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(filePart.getOriginalFilename()));
        String uniqueFileName = UUID.randomUUID() + "_" + originalFilename;

        String safeFileName;
        try {
            safeFileName = sanitizeFilename(uniqueFileName);
        } catch (IllegalArgumentException ex) {
            // 파일명 검증 실패 시
            return null;
        }

        final Path basePath = Paths.get(absolutePath).toAbsolutePath().normalize();

        // 2. 🚩 MODIFIED: 디렉토리 구조 변경: /profiles/{userId}/filename
        Path targetRelative = Paths.get("profiles")
                // 🚩 MODIFIED: userId를 폴더명으로 사용
                .resolve(String.valueOf(userId))
                .resolve(safeFileName);

        Path targetPath = basePath.resolve(targetRelative).normalize();
        if (!targetPath.startsWith(basePath)) return null;

        try {
            // 3. 파일 저장
            Files.createDirectories(targetPath.getParent());
            File dest = targetPath.toFile();
            filePart.transferTo(dest);

            // 4. DB 저장
            String dbDirectory = basePath.relativize(targetPath).toString().replace('\\', '/');

            FileDTO fileDto = FileDTO.builder()
                    .type("PROFILE")
                    .name(safeFileName)
                    .directory(dbDirectory)
                    .build();

            fileMapper.saveFile(fileDto);

            return fileDto.getId();
        } catch (IOException e) {
            // 실패 시 로깅
            log.error("File save failed (IOException): " + e.getMessage());
            return null;
        } catch (Exception e) {
            // 기타 실패 시 로깅
            log.error("File save failed (Exception): " + e.getMessage());
            return null;
        }
    }
}