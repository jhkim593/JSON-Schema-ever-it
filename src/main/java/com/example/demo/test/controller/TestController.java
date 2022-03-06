package com.example.demo.test.controller;

import com.example.demo.test.dto.JsonSchemaErrorDto;
import com.example.demo.test.validator.JsonValidator;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final JsonValidator jsonValidator;


    @RequestMapping(value = "/json/test", method = RequestMethod.POST)
    public ResponseEntity jsonTest(@RequestBody String body , @RequestParam String schemaName)  {
        List<JsonSchemaErrorDto> conditionTest = jsonValidator.valid(schemaName, body);
        if(conditionTest==null){
            return new ResponseEntity(HttpStatus.OK);
        }
        return new ResponseEntity(conditionTest,HttpStatus.BAD_REQUEST);
    }
}
