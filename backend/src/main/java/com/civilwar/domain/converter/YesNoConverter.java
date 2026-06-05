package com.civilwar.domain.converter;

import com.civilwar.domain.enums.YesNo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class YesNoConverter implements AttributeConverter<YesNo, String> {

	@Override
	public String convertToDatabaseColumn(YesNo attribute) {
		return attribute == null ? null : attribute.name();
	}

	@Override
	public YesNo convertToEntityAttribute(String dbData) {
		return dbData == null ? null : YesNo.valueOf(dbData);
	}
}
