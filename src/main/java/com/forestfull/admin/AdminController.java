package com.forestfull.admin;

import com.forestfull.common.ResponseException;
import com.forestfull.common.file.FILE_TYPE;
import com.forestfull.common.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final FileService fileService;

    @PostMapping(value = "/emoji/{filename}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ResponseException>> saveEmoji(
            @RequestPart("file") Mono<FilePart> filePartMono,
            @PathVariable String filename) {

        return filePartMono.flatMap(filePart ->
                fileService.saveFile(filePart, FILE_TYPE.EMOJI.name(), filename)
                        .map(result -> result.isSuccess()
                                ? ResponseEntity.ok(result)
                                : ResponseEntity.badRequest().body(result))
        );
    }

    @DeleteMapping("/emoji/{id}")
    Mono<ResponseEntity<ResponseException>> deleteEmoji(@PathVariable Long id) {
        final ResponseException result = fileService.deleteFile(id);
        return Mono.just(result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result));
    }
}