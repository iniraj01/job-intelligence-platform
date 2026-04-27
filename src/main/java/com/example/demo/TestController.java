package com.example.demo;

import com.example.demo.dto.UploadResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "Backend is running";
    }

    @GetMapping("/api/testjson")
    public Map<String, Object> testJson() {
        Map<String, Object> response = new HashMap<>();
        response.put("fileName", "test.pdf");
        response.put("message", "Test response");
        response.put("fileSize", 1024L);
        return response;
    }

}
