package com.easychat.entity.enums;

public enum GroupStatusEnum {
    NORMAL(1,"正常"),
    DISSOLUTION(0,"解散");

    private Integer status;
    private String desc;

    GroupStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static GroupStatusEnum getByStatus(Integer status) {
        for (GroupStatusEnum value : GroupStatusEnum.values()) {
            if (value.status.equals(status)) {
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
