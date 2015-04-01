package com.meistermeier.lcd;


import static com.meistermeier.lcd.LcdBackgroundColor.*;


public class LcdDemo {

    public static void main(String[] args) throws Exception {
        LcdPlate lcd = new LcdPlate(1, 0x20, 2, 16);
        lcd.backlight(LcdBackgroundColor.WHITE.getColorValue());
        lcd.message("Hello World!");
        Thread.sleep(500);
        lcd.clear();
        lcd.backlight(YELLOW.getColorValue());
        Thread.sleep(500);
        lcd.clear();
        lcd.backlight(GREEN.getColorValue());
        Thread.sleep(1000);
        lcd.clear();
        lcd.backlight(RED.getColorValue());
        lcd.message("Two line\ntext...");
        Thread.sleep(2000);
        lcd.clear();
        lcd.backlight(WHITE.getColorValue());
        lcd.message("Try buttons");

        long countdownTimestamp = System.currentTimeMillis();
        while (true) {
            // you could also use LcdPlate#buttons() to retrieve a bitmask
            // this will allow simultaneous pressing
            boolean rightPressed = lcd.buttonPressed(LcdPlate.BUTTON_RIGHT) == 1;
            boolean leftPressed = lcd.buttonPressed(LcdPlate.BUTTON_LEFT) == 1;
            boolean upPressed = lcd.buttonPressed(LcdPlate.BUTTON_UP) == 1;
            boolean downPressed = lcd.buttonPressed(LcdPlate.BUTTON_DOWN) == 1;
            boolean selectPressed = lcd.buttonPressed(LcdPlate.BUTTON_SELECT) == 1;
            if (rightPressed) {
                lcd.clear();
                lcd.message("right");
            } else if (leftPressed) {
                lcd.clear();
                lcd.message("left");
            } else if (upPressed) {
                lcd.clear();
                lcd.message("up");
            } else if (downPressed) {
                lcd.clear();
                lcd.message("down");
            } else if (selectPressed) {
                lcd.clear();
                lcd.message("select");
                if (System.currentTimeMillis() - countdownTimestamp > 2000) {
                    lcd.clear();
                    lcd.backlight(OFF.getColorValue());
                    lcd.stop();
                    System.exit(0);
                }
            } else {
                lcd.clear();
                lcd.message("Try buttons");
                countdownTimestamp = System.currentTimeMillis();
            }
            Thread.sleep(50);

        }
    }
}