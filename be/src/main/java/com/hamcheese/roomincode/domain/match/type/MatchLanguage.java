package com.hamcheese.roomincode.domain.match.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MatchLanguage {
    JAVA("Java"),
    PYTHON("Python"),
    CPP("Cpp");

    private final String value;

    MatchLanguage(String value) {
        this.value = value;
    }

    @JsonCreator
    public static MatchLanguage fromString(String text) {
        if (text == null) {
            return null;
        }
        for (MatchLanguage lang : MatchLanguage.values()) {
            if (lang.value.equalsIgnoreCase(text) || lang.name().equalsIgnoreCase(text)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("No constant with value " + text + " found");
    }

    @JsonValue
    public String toJson() {
        return value;
    }
}
