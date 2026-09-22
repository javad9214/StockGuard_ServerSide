package com.stockguard.data.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.stockguard.data.enums.ResponseCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDTO<T> {
    @Schema(title = "response code")
    @Builder.Default
    private int resCode = ResponseCode.OK_OPERATION.getCode();

    @Schema(title = "response message")
    @Builder.Default
    private String resMessage = ResponseCode.OK_OPERATION.getDescription();

    @Schema(title = "response information")
    private T info;

    @Override
    public String toString() {
        return "{" +
                "resCode=" + resCode +
                ", resMessage='" + resMessage + '\'' +
                ", info=" + info +
                '}';
    }
}