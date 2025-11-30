package com.forestfull.common.file;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    @ResponseBody
    @GetMapping(path = {"/emoji", "/emoji/{filename}"})
    Mono<ResponseEntity<List<FileDTO>>> getEmojiList(@PathVariable(required = false) String filename) {
        return Mono.just(ResponseEntity.ok(fileService.getEmojiList(filename)));
    }
}