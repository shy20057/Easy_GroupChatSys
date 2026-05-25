package com.easychat.entity.enums;

public enum AppUpdateFileTypeEnum {
    LOCAL(0,"本地"),
    OUTER_LINK(1,"外链");

    private Integer type;
    private String description;

    AppUpdateFileTypeEnum(Integer type, String description) {
        this.type = type;
        this.description = description;
    }


    public Integer getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public static AppUpdateFileTypeEnum getByType(Integer type) {
        for (AppUpdateFileTypeEnum value : AppUpdateFileTypeEnum.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }

}
