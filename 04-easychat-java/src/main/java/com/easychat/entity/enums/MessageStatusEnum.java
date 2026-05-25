package com.easychat.entity.enums;

public enum MessageStatusEnum {
    SENDING(0,"发送中"),
    SENDED(1,"发送成功");

    private Integer status;
    private String desc;

    MessageStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static MessageStatusEnum getStatus(Integer status) {
        for (MessageStatusEnum value : MessageStatusEnum.values()) {
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
