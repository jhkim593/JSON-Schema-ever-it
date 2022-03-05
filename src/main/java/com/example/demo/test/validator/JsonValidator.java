package com.example.demo.test.validator;

import com.example.demo.test.dto.JsonSchemaErrorDto;
import lombok.RequiredArgsConstructor;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JsonValidator {

    private final ResourceLoader resourceLoader;
    private Map<String, Schema> jsonSchemaMap = new HashMap<>();

    @PostConstruct
    private void init(){
        getResources();
    }

    private void getResources() {
        Resource[] resources;
        try {
            resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources("classpath*:/schemas/*.json");

            for (Resource resource : resources) {
                BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
                StringBuffer stringBuffer = new StringBuffer();
                String str = null;
                while ((str = br.readLine()) != null) {
                    stringBuffer.append(str).append("\n");
                }
                int endIndex = resource.getFilename().indexOf(".schema");
                String name = resource.getFilename().substring(0, endIndex);

                JSONObject jsonObject = new JSONObject(stringBuffer.toString());

                SchemaLoader schemaLoader = SchemaLoader.builder()
                        .schemaClient(SchemaClient.classPathAwareClient())  // 스키마간 참조를 위한 classPath 설정
                        .schemaJson(jsonObject)
                        .build();
                Schema schema = schemaLoader.load().build();


                jsonSchemaMap.put(name, schema);
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void messageCheck(ValidationException e, Map<String,String> requiredMessageMap, Map<String,String>messageMap, List<JsonSchemaErrorDto>errorDtoList){
        if(e.getCausingExceptions().size()>0){
            e.getCausingExceptions().stream().forEach(c->{
                Map<String, String> newRequiredMessageMap = (HashMap<String, String>) c.getViolatedSchema().getUnprocessedProperties().get("requiredMessages");
                Map<String, String> newMessageMap = (HashMap<String, String>) c.getViolatedSchema().getUnprocessedProperties().get("messages");

                newMessageMap=(newMessageMap==null)? messageMap : newMessageMap;
                newRequiredMessageMap=(newRequiredMessageMap==null)?requiredMessageMap:newRequiredMessageMap;

                messageCheck(c, newRequiredMessageMap, newMessageMap ,errorDtoList);
            });
        }
        else {
            JsonSchemaErrorDto  errorDto = new JsonSchemaErrorDto();

            String pointer=e.getPointerToViolation();

            //keyword required 일 때
            if(e.getKeyword().equals("required")) {
                int startIndex = e.getMessage().indexOf("[");
                int endIndex = e.getMessage().indexOf("]");
                String index = e.getMessage().substring(startIndex+1, endIndex);
                pointer+="/"+index;

                HashMap<String, String> newRequiredMessageMap = (HashMap<String, String>) e.getViolatedSchema().getUnprocessedProperties().get("requiredMessages");

                if(newRequiredMessageMap!=null) {
                    errorDto.setMessage(newRequiredMessageMap.get(index));
                }
                else{
                    if(requiredMessageMap!=null){
                        errorDto.setMessage(requiredMessageMap.get(index));
                    }
                }
            }

            //keyword required 아닐 때
            else {
                HashMap<String, String> newMessageMap = (HashMap<String, String>) e.getViolatedSchema().getUnprocessedProperties().get("messages");
                if(newMessageMap!=null){
                    errorDto.setMessage(newMessageMap.get(e.getKeyword()));
                }
                else{
                    if(messageMap!=null) {
                        errorDto.setMessage(messageMap.get(e.getKeyword()));
                    }
                }
            }

            if(errorDto.getMessage()==null){
                errorDto.setMessage("invalid");
            }
            errorDto.setPointer(pointer);
            errorDto.setKeyword(e.getKeyword());
            errorDtoList.add(errorDto);
        }
    }
    public List<JsonSchemaErrorDto> valid(String schemaName, String jsonStr) {

        try {
            if (jsonSchemaMap.containsKey(schemaName)) {
                if (jsonStr.charAt(0) == '[') {
                    JSONArray jsonArray = new JSONArray(jsonStr);
                    jsonSchemaMap.get(schemaName).validate(jsonArray);
                } else {
                    JSONObject jsonObject = new JSONObject(jsonStr);
                    jsonSchemaMap.get(schemaName).validate(jsonObject);
                }

            }
        } catch (ValidationException e) {
            ArrayList<JsonSchemaErrorDto> errorDtoList = new ArrayList<>();

            Map<String, String> requiredMessageMap = (HashMap<String, String>) e.getViolatedSchema().getUnprocessedProperties().get("requiredMessages");
            Map<String, String> messageMap = (HashMap<String, String>) e.getViolatedSchema().getUnprocessedProperties().get("messages");
            messageCheck(e,requiredMessageMap,messageMap,errorDtoList);
            return errorDtoList;
        }

        return null;
    }

}
