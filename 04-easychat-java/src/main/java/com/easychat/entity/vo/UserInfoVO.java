package com.easychat.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String nickName;
    private Integer sex;
    private Integer joinType;
    private String personalSignature;
    private String areaName;
    private String areaCode;
    private String token;
    private Boolean admin; // 是否是管理员
    private Integer contactStatus;
}
