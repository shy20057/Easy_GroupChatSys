package com.easychat.entity.enums;

public enum UserStatusEnum {

    DISABLED(0,"禁用"),
    ENABLED(1,"启用");

    private Integer status;
    private String desc;

    private UserStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static UserStatusEnum getByStatus(Integer status) {
        for (UserStatusEnum value : UserStatusEnum.values()) {
            if (value.getStatus().equals(status)) {
                return value;
            }
        }
        return null;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
