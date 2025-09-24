package com.squad6.deneasybot.repository;

import org.springframework.stereotype.Repository;

@Repository
public class ExampleRepository {

    public String mockProcess(String message) {

    	return "Mensagem '" + message + "' processada no repository";
    }
}
