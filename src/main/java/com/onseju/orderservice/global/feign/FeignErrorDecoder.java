package com.onseju.orderservice.global.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onseju.orderservice.global.exception.BaseException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;

public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.body() == null) {
            return new BaseException("응답 본문이 없습니다.", HttpStatus.valueOf(response.status()));
        }

        String message = extractExceptionMessage(response);
        return new BaseException(message, HttpStatus.valueOf(response.status()));
    }

    private String extractExceptionMessage(Response response) {
        try (InputStream bodyIs = response.body().asInputStream()) {
            FeignExceptionResponse exceptionMessage = objectMapper.readValue(bodyIs, FeignExceptionResponse.class);
            return exceptionMessage.message();
        } catch (IOException e) {
            throw new BaseException("Feign 클라이언트 응답을 처리하는 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}