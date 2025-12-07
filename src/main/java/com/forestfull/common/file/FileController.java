package com.forestfull.common.file;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    @GetMapping(path = {"/emoji", "/emoji/{filename}"})
    ResponseEntity<List<FileDTO>> getEmojiList(@PathVariable(required = false) String filename) {
        return ResponseEntity.ok(fileService.getEmojiList(filename));
    }
}