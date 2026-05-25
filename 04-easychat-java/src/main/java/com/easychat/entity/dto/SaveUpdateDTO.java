package com.easychat.entity.dto;

import io.swagger.models.auth.In;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class SaveUpdateDTO {

    private Integer id;
    @NotEmpty
    private String version;
    @NotEmpty
    private String updateDesc;
    @NotNull
    private Integer fileType;
    String outerLink;
    MultipartFile file;
}
