package com.example.demo.test.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JsonSchemaErrorDto {
    private String message;
    private String keyword;
    private String pointer;
}
