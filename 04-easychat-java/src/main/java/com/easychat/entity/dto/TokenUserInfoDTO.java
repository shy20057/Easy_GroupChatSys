package com.easychat.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenUserInfoDTO implements Serializable { // 这里设置序列化的原因： token信息放在Redis里面

    private static final long serialVersionUID = 1L;

    private String token;
    private String userId;
    private String nickName;
    private Boolean admin;
}
