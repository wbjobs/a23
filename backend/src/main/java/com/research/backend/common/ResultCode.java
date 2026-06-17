package com.research.backend.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "权限不足，禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    LOGIN_SUCCESS(20001, "登录成功"),
    LOGIN_FAIL(20002, "用户名或密码错误"),
    USER_NOT_FOUND(20003, "用户不存在"),
    USER_ALREADY_EXIST(20004, "用户名已存在"),
    USER_DISABLED(20005, "账号已被禁用"),
    TOKEN_INVALID(20006, "Token无效或已过期"),
    TOKEN_MISSING(20007, "Token缺失"),
    REGISTER_SUCCESS(20008, "注册成功"),

    PAPER_NOT_FOUND(30001, "论文不存在"),
    PAPER_UPLOAD_SUCCESS(30002, "论文上传成功"),
    PAPER_UPLOAD_FAIL(30003, "论文上传失败"),
    PAPER_STATUS_ERROR(30004, "论文状态错误"),

    REVIEWER_NOT_FOUND(40001, "评审专家不存在"),
    REVIEW_SUBMIT_SUCCESS(40002, "评审提交成功"),
    REVIEW_SUBMIT_FAIL(40003, "评审提交失败"),
    REVIEW_ALREADY_EXIST(40004, "已提交过评审"),

    FILE_UPLOAD_SUCCESS(50001, "文件上传成功"),
    FILE_UPLOAD_FAIL(50002, "文件上传失败"),
    FILE_NOT_FOUND(50003, "文件不存在"),

    EMAIL_SEND_FAIL(60001, "邮件发送失败"),
    PYTHON_SERVICE_ERROR(60002, "Python服务调用失败"),
    DATABASE_ERROR(60003, "数据库操作异常");

    private final Integer code;

    private final String message;
}
