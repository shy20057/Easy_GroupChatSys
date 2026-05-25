package com.easychat.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AppUpdateVO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Integer id;
    private String version;
    private List<String> updateList;
    private Long size;
    private String fileName;
    private Integer fileType;
    private String outerLink;
}
