package edu.ucsb.federate.entities;

import com.google.re2j.Pattern;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RegexConverter implements AttributeConverter<Pattern, String> {

  @Override
  public String convertToDatabaseColumn(Pattern attribute) {
    return attribute.pattern();
  }

  @Override
  public Pattern convertToEntityAttribute(String dbData) {
    return Pattern.compile(dbData);
  }
}
