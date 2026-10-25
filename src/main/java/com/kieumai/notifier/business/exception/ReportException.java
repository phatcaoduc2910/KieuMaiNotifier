package com.kieumai.notifier.business.exception;

public class ReportException extends RuntimeException {
    public enum Code {
        VALIDATION_ERROR, UNAUTHORIZED, INCIDENT_NOT_FOUND,
        REPORT_NOT_FOUND, REPORT_NOT_OWNED
    }

    private final Code code;

    public ReportException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() { return code; }
}
