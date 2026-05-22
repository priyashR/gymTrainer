package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.outbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

/**
 * JPA AttributeConverter that serializes/deserializes a list of {@link SectionProgressDto}
 * to/from a JSON string for storage in a JSONB column.
 */
@Converter
class SectionProgressListConverter implements AttributeConverter<List<SectionProgressDto>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final TypeReference<List<SectionProgressDto>> TYPE_REF = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<SectionProgressDto> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize section progresses to JSON", e);
        }
    }

    @Override
    public List<SectionProgressDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(dbData, TYPE_REF);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize section progresses from JSON", e);
        }
    }
}
