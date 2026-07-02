package com.artdecor.workforce.application.attendance;

public class AttendanceException extends RuntimeException {
    private final String code;

    public AttendanceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

