package com.forestfull.common.file;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileMapper {

    FileDTO getFileById(@Param("id") Long id);

    List<FileDTO> getEmojiList(@Param("id") String emojiFileName);

    void saveFile(FileDTO dto);

    void deleteFile(@Param("id") Long id);
}