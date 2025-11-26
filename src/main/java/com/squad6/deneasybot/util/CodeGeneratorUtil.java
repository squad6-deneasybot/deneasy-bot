package com.squad6.deneasybot.util;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class CodeGeneratorUtil {

    private final SecureRandom random = new SecureRandom();

    public String generateRandom6DigitCode() {
        int number = random.nextInt(900000) + 100000;
        return String.valueOf(number);
    }
}