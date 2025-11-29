package com.forestfull.common.file;

import com.forestfull.common.ResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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

    public File getFile(String directory) {
        return new File(absolutePath + directory);
    }

    public FileDTO getFileById(Long id) {
        return fileMapper.getFileById(id);
    }

    public List<FileDTO> getEmojiList(String emojiFileName) {
        final List<FileDTO> emojiList = fileMapper.getEmojiList(emojiFileName);
        if (ObjectUtils.isEmpty(emojiList)) return Collections.emptyList();

        return emojiList.stream()
                .peek(file -> file.setFile(getFile(file.getDirectory())))
                .toList();
    }

    public ResponseException saveFile(MultipartFile file, String type, String fileName) throws IOException {
        if (!StringUtils.hasText(fileName) || fileName.length() > 255)
            return ResponseException.fail("no file name, check please");

        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\") || fileName.contains(File.pathSeparator))
            return ResponseException.fail("invalid file name");

        final LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        final Path baseDir = Paths.get(absolutePath).normalize();

        final Path targetPath = baseDir.resolve(
                now.getYear() + "/" +
                        now.getMonth().getValue() + "/" +
                        UUID.randomUUID() + "_" + fileName
        ).normalize();

        if (!targetPath.startsWith(baseDir)) return ResponseException.fail("invalid path");

        Files.createDirectories(targetPath.getParent());
        file.transferTo(targetPath.toFile());

        fileMapper.saveFile(FileDTO.builder()
                .type(type)
                .name(fileName)
                .directory(baseDir.relativize(targetPath).toString())
                .build()
        );

        return ResponseException.ok();
    }

    public ResponseException deleteFile(Long id) {
        FileDTO fileById = getFileById(id);
        if (Objects.isNull(fileById)) return ResponseException.fail("no file id, check please");

        fileMapper.deleteFile(id);
        getFile(fileById.getDirectory()).deleteOnExit();

        return ResponseException.ok();
    }
}