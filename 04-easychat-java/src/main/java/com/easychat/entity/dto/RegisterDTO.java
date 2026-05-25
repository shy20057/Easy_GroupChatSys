package com.easychat.entity.dto;

import com.easychat.entity.constants.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO {

    @NotEmpty
    @Email
    private String email;
    @NotEmpty
    @Pattern(regexp = Constants.REGEX_PASSWORD)
    private String password;
    @NotEmpty
    private String nickName;
    @NotEmpty
    private String checkCode; // 验证码
    @NotEmpty
    private String checkCodeKey; // 验证码key
}
