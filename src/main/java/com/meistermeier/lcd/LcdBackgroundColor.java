package com.meistermeier.lcd;

public enum LcdBackgroundColor {

    OFF(0x00),
    RED(0x01),
    GREEN(0x02),
    BLUE(0x04),
    YELLOW(RED.colorValue + GREEN.colorValue),
    TEAL(GREEN.colorValue + BLUE.colorValue),
    VIOLET(RED.colorValue + BLUE.colorValue),
    WHITE(RED.colorValue + GREEN.colorValue + BLUE.colorValue);

    private final int colorValue;

    LcdBackgroundColor(int colorValue) {

        this.colorValue = colorValue;
    }

    public int getColorValue() {
        return colorValue;
    }

}
