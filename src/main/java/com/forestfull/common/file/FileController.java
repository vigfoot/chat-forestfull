package com.forestfull.common.file;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
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

    // 새로 추가: 브라우저에서 파일 직접 접근
    @ResponseBody
    @GetMapping("/emoji/resource/{directory:.+}")
    public Mono<ResponseEntity<Resource>> getEmoji(@PathVariable("directory") String directory) {
        Resource resource = fileService.getFileResource(directory);
        if (resource.exists()) {
            return Mono.just(ResponseEntity.ok().body(resource));
        } else {
            return Mono.just(ResponseEntity.notFound().build());
        }
    }
}