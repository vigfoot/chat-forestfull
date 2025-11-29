package com.forestfull.admin;

import com.forestfull.common.ResponseException;
import com.forestfull.common.file.FILE_TYPE;
import com.forestfull.common.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final FileService fileService;

    @PostMapping("/file/emoji/{filename}")
    Mono<ResponseEntity<ResponseException>> saveEmoji(MultipartFile file, @PathVariable String filename){
        try {
            final ResponseException body = fileService.saveFile(file, FILE_TYPE.EMOJI.name(), filename);
            return Mono.just(ResponseEntity.ok(body));
        } catch (IOException e) {
            return Mono.just(ResponseEntity.internalServerError().build());
        }
    }

}