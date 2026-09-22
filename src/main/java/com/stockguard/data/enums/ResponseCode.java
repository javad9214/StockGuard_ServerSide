package com.stockguard.data.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ResponseCode {

    OK_OPERATION(Code.OK_OPERATION, "ok.message.successful_operation"),

    // error responses
    INTERNAL_ERROR(Code.INTERNAL_ERROR, "errors.message.internal_error"),
    ACCESS_DENIED(Code.ACCESS_DENIED, "errors.message.access_denied"),
    NOT_FOUND(Code.NOT_FOUND, "errors.message.resource_not_found"),
    VALIDATION_ERROR(Code.VALIDATION_ERROR, "errors.message.validation_error"),
    INVALID_STATE_TRANSITION(Code.INVALID_STATE_TRANSITION, "errors.message.invalid_state_transition");

    private final int code;
    private final String description;

    public static ResponseCode getValue(int code) {
        ResponseCode[] enums = ResponseCode.values();
        for (ResponseCode item : enums) {
            if (item.code == code)
                return item;
        }
        return null;
    }

    public static class Code {
        public static final int OK_OPERATION = 1;
        public static final int INTERNAL_ERROR = 2;
        public static final int ACCESS_DENIED = 3;
        public static final int NOT_FOUND = 4;
        public static final int VALIDATION_ERROR = 5;
        public static final int INVALID_STATE_TRANSITION = 6;
    }

}