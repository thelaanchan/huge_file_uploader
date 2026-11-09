package com.example.fileupload;

import com.example.fileupload.config.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FileUploadApplication {

    public static void main(String[] args) {
        // Load environment variables from .env file into System properties
        EnvLoader.load();
        SpringApplication.run(FileUploadApplication.class, args);
    }
}
