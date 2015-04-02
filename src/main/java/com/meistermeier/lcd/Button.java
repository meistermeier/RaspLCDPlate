package com.meistermeier.lcd;

public enum Button {
    SELECT(0),
    RIGHT(1),
    DOWN(2),
    UP(3),
    LEFT(4);

    private final int mappedCode;

    Button(int mappedCode) {
        this.mappedCode = mappedCode;
    }

    public int getMappedCode() {
        return mappedCode;
    }
}
