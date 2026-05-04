package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.config;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadFormatter;
import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.upload.domain.UploadParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers upload domain objects as Spring beans.
 *
 * <p>{@link UploadParser} and {@link UploadFormatter} are pure domain classes with
 * zero framework imports. They are instantiated here so Spring can inject them into
 * the application services without adding framework annotations to the domain layer.
 */
@Configuration
public class UploadConfig {

    @Bean
    public UploadParser uploadParser() {
        return new UploadParser();
    }

    @Bean
    public UploadFormatter uploadFormatter() {
        return new UploadFormatter();
    }
}
