package com.forestfull.common.file;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    @GetMapping("/emoji/{filename}")
    Mono<ResponseEntity<List<FileDTO>>> getEmojiList(@PathVariable String filename) {
        return Mono.just(ResponseEntity.ok(fileService.getEmojiList(filename)));
    }
}