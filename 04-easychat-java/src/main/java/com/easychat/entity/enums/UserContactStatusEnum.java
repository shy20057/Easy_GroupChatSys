package com.easychat.entity.enums;

public enum UserContactStatusEnum {
    NOT_FRIEND(0,"非好友"),
    FRIEND(1,"好友"),
    DEL(2,"已删好友"),
    DEL_BE(3,"被好友删除"),
    BLACKLIST(4,"已拉黑好友"),
    BLACKLIST_BE(5,"被好友拉黑"),
    BLACKLIST_BE_FIRST(6,"首次被好友拉黑");

    private Integer code;
    private String desc;

    private UserContactStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserContactStatusEnum getByCode(Integer code) {
        for (UserContactStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static UserContactStatusEnum getByCode(String code) {
        for (UserContactStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
