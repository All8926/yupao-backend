package com.example.common;

public enum ErrorCode {

    SUCCESS(0,"OK",""),
    PARAMS_ERROR(40000,"请求参数错误",""),
    NULL_ERROR(40001,"请求数据为空",""),
    NOT_LOGIN(40100,"未登录",""),
    NO_AUTH(40300,"无权限",""),
    SYSTEM_ERROR(50000,"系统错误","");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 状态信息
     */
    private final String message;

    /**
     * 状态信息描述
     */
    private final String description;

    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}
