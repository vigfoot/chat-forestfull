package com.forestfull.common.file;

import com.forestfull.common.ResponseException;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
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

    public ResponseException uploadFile(MultipartFile file, String type, String fileName) throws IOException {
        if (!StringUtils.hasText(fileName) || fileName.length() > 255)
            return ResponseException.fail("no file name, check please");

        final LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        final String directory = now.getYear() + File.pathSeparator
                + now.getMonth().getValue() + File.pathSeparator
                + UUID.randomUUID() + "_" + fileName;

        File dest = new File(absolutePath + directory);
        file.transferTo(dest);

        fileMapper.insertFile(FileDTO.builder()
                .type(type)
                .name(fileName)
                .directory(directory)
                .build()
        );

        return ResponseException.ok();
    }

    public ResponseException deleteFile(Long id) {
        FileDTO fileById = getFileById(id);
        if (Objects.isNull(fileById)) return ResponseException.fail("no file id, check please");

        getFile(fileById.getDirectory()).deleteOnExit();

        return ResponseException.ok();
    }
}