package com.stockguard.controller;

import com.stockguard.configuration.MessageSourceConfiguration;
import com.stockguard.data.dto.common.ResponseDTO;
import com.stockguard.data.enums.ResponseCode;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@FieldDefaults(level = AccessLevel.PROTECTED)
public class BaseController {

    @Autowired
    MessageSourceConfiguration messageSource;

    protected <T> ResponseEntity<T> generateResponse(T body) {
        return ResponseEntity.ok()
                .header("Expires", "0")
                .header("Cache-Control", "must-revalidate, post-check=0, pre-check=0")
                .header("Pragma", "public")
                .body(body);
    }

    protected <T> ResponseEntity<ResponseDTO<T>> generateOKResponse(T info) {
        ResponseDTO<T> responseDTO = new ResponseDTO<>();
        responseDTO.setResMessage(messageSource.getMessage(ResponseCode.OK_OPERATION.getDescription()));
        responseDTO.setResCode(ResponseCode.OK_OPERATION.getCode());
        responseDTO.setInfo(info);
        return generateResponse(responseDTO);
    }

    /**
     * @param message specific failure reason shown to the client; when null
     *                the generic localized message for the code is used
     */
    protected <T> ResponseEntity<ResponseDTO<T>> generateErrorResponse(HttpStatus status, ResponseCode code, String message) {
        ResponseDTO<T> responseDTO = new ResponseDTO<>();
        responseDTO.setResCode(code.getCode());
        responseDTO.setResMessage(message != null ? message : messageSource.getMessage(code.getDescription()));
        return ResponseEntity.status(status).body(responseDTO);
    }
}
