package com.squad6.deneasybot.service;

import com.squad6.deneasybot.repository.ExampleRepository;
import org.springframework.stereotype.Service;

@Service
public class ExampleService {

    private final ExampleRepository repository;

    public ExampleService(ExampleRepository repository) {
        this.repository = repository;
    }

    public String processExample(String message) {
    	
        return repository.mockProcess(message);
    }
}
