package com.meistermeier.rasplcdplate;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LcdPlate {

    // Port expander input pin definitions
    public static final int BUTTON_SELECT = 0;
    public static final int BUTTON_RIGHT = 1;
    public static final int BUTTON_DOWN = 2;
    public static final int BUTTON_UP = 3;
    public static final int BUTTON_LEFT = 4;

    // LED colors
    public static final int BACKGROUND_OFF = 0x00;
    public static final int BACKGROUND_RED = 0x01;
    public static final int BACKGROUND_GREEN = 0x02;
    public static final int BACKGROUND_BLUE = 0x04;
    public static final int BACKGROUND_YELLOW = BACKGROUND_RED + BACKGROUND_GREEN;
    public static final int BACKGROUND_TEAL = BACKGROUND_GREEN + BACKGROUND_BLUE;
    public static final int BACKGROUND_VIOLET = BACKGROUND_RED + BACKGROUND_BLUE;
    public static final int BACKGROUND_WHITE = BACKGROUND_RED + BACKGROUND_GREEN + BACKGROUND_BLUE;
    public static final int BACKGROUND_ON = BACKGROUND_RED + BACKGROUND_GREEN + BACKGROUND_BLUE;

    // Port expander registers
    private static final int MCP23017_IOCON_BANK0 = 0x0A;// IOCON when Bank 0 active
    private static final int MCP23017_IOCON_BANK1 = 0x15; // IOCON when Bank 1 active
    // These are register addresses when in Bank 1 only:
    private static final int MCP23017_GPIOA = 0x09;
    private static final int MCP23017_IODIRB = 0x10;
    private static final int MCP23017_GPIOB = 0x19;

    // LCD Commands
    private static final int LCD_CLEARDISPLAY = 0x01;
    private static final int LCD_RETURNHOME = 0x02;
    private static final int LCD_ENTRYMODESET = 0x04;
    private static final int LCD_DISPLAYCONTROL = 0x08;
    private static final int LCD_CURSORSHIFT = 0x10;
    private static final int LCD_FUNCTIONSET = 0x20;
    private static final int LCD_SETCGRAMADDR = 0x40;
    private static final int LCD_SETDDRAMADDR = 0x80;

    // Flags for display on/off control
    private static final int LCD_DISPLAYON = 0x04;
    private static final int LCD_DISPLAYOFF = 0x00;
    private static final int LCD_CURSORON = 0x02;
    private static final int LCD_CURSOROFF = 0x00;
    private static final int LCD_BLINKON = 0x01;
    private static final int LCD_BLINKOFF = 0x00;

    // Flags for display entry mode
    private static final int LCD_ENTRYRIGHT = 0x00;
    private static final int LCD_ENTRYLEFT = 0x02;
    private static final int LCD_ENTRYSHIFTINCREMENT = 0x01;
    private static final int LCD_ENTRYSHIFTDECREMENT = 0x00;

    // Flags for display/cursor shift
    private static final int LCD_DISPLAYMOVE = 0x08;
    private static final int LCD_CURSORMOVE = 0x00;
    private static final int LCD_MOVERIGHT = 0x04;
    private static final int LCD_MOVELEFT = 0x00;

    // Line addresses for up to 4 line displays.  Maps line number to DDRAM address for line.
    private static final Map<Integer, Integer> LINE_ADDRESSES = new HashMap<>(3);

    static {
        // { 1: 0xC0, 2: 0x94, 3: 0xD4 }
        LINE_ADDRESSES.put(1, 0xC0);
        LINE_ADDRESSES.put(2, 0x94);
        LINE_ADDRESSES.put(3, 0xD4);
    }

    // Truncation constants for message function truncate parameter.
    private static final int NO_TRUNCATE = 0;
    private static final int TRUNCATE = 1;
    private static final int TRUNCATE_ELLIPSIS = 2;

    // The LCD data pins (D4-D7) connect to MCP pins 12-9 (PORTB4-1), in
    // that order.  Because this sequence is 'reversed,' a direct shift
    // won't work.  This table remaps 4-bit data values to MCP PORTB
    // outputs, incorporating both the reverse and shift.
    private static final int[] FLIP = {0b00000000, 0b00010000, 0b00001000, 0b00011000,
            0b00000100, 0b00010100, 0b00001100, 0b00011100,
            0b00000010, 0b00010010, 0b00001010, 0b00011010,
            0b00000110, 0b00010110, 0b00001110, 0b00011110};

    private static final int[] ROW_OFFSETS = new int[]{0x00, 0x40, 0x14, 0x54};

    private final I2CDevice lcdDevice;
    private int portA = 0;
    private int portB = 0;

    private int ddrb = 0b00000010;
    private int displayShift = (LCD_CURSORMOVE | LCD_MOVERIGHT);
    private int displayMode = (LCD_ENTRYLEFT | LCD_ENTRYSHIFTDECREMENT);
    private int displayControl = (LCD_DISPLAYON | LCD_CURSOROFF | LCD_BLINKOFF);
    private int currLine;
    private int numLines;
    private int numCols;


    public LcdPlate(int busNumber, int deviceAddress, int rows, int cols) {
        lcdDevice = connectToLcdDevice(busNumber, deviceAddress);
        numCols = cols;
        numLines = rows;
        initializeConnection();
    }

    private I2CDevice connectToLcdDevice(int busNumber, int deviceAddress) {
        try {
            I2CBus i2cBus = I2CFactory.getInstance(busNumber);
            return i2cBus.getDevice(deviceAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initializeConnection() {
        try {
            lcdDevice.write(MCP23017_IOCON_BANK1, (byte) 0);

            byte[] buffer = {
                    0b00111111,   // IODIRA    R+G LEDs=outputs, buttons=inputs
                    (byte) ddrb,    // IODIRB    LCD D7=input, Blue LED=output
                    0b00111111,   // IPOLA     Invert polarity on button inputs
                    0b00000000,   // IPOLB
                    0b00000000,   // GPINTENA  Disable interrupt-on-change
                    0b00000000,   // GPINTENB
                    0b00000000,   // DEFVALA
                    0b00000000,   // DEFVALB
                    0b00000000,   // INTCONA
                    0b00000000,   // INTCONB
                    0b00000000,   // IOCON
                    0b00000000,   // IOCON
                    0b00111111,   // GPPUA     Enable pull-ups on buttons
                    0b00000000,   // GPPUB
                    0b00000000,   // INTFA
                    0b00000000,   // INTFB
                    0b00000000,   // INTCAPA
                    0b00000000,   // INTCAPB
                    (byte) portA,   // GPIOA
                    (byte) portB,   // GPIOB
                    (byte) portA,   // OLATA
                    (byte) portB    // OLATB
            };

            lcdDevice.write(0, buffer, 0, buffer.length);

            lcdDevice.write(MCP23017_IOCON_BANK0, (byte) 0b10100000);

            writeInternalCommand(0x33); // Init
            writeInternalCommand(0x32); // Init
            writeInternalCommand(0x28); // 2 line 5x8 matrix
            writeInternalCommand(LCD_CLEARDISPLAY);
            writeInternalCommand(LCD_CURSORSHIFT | displayShift);
            writeInternalCommand(LCD_ENTRYMODESET | displayMode);
            writeInternalCommand(LCD_DISPLAYCONTROL | displayControl);
            writeInternalCommand(LCD_RETURNHOME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeInternalCommand(int value) {
        try {
            pollToClear();

            int bitMask = portB & 0b00000001;

            byte[] data = out4(bitMask, value);
            lcdDevice.write(MCP23017_GPIOB, data, 0, 4);
            portB = data[3];
            // If a poll-worthy instruction was issued, reconfigure D7
            //pin as input to indicate need for polling on next call.
            if (value == LCD_CLEARDISPLAY || value == LCD_RETURNHOME) {
                ddrb |= 0b00000010;
                lcdDevice.write(MCP23017_IODIRB, (byte) ddrb);
            }
        } catch (Exception e) {
            // not quite everything alright ;)
            e.printStackTrace();
        }
    }

    private void writeBitmap(int value) {
        try {
            pollToClear();

            int bitMask = portB & 0b00000001;
            bitMask |= 0b10000000;

            byte[] data = out4(bitMask, value);
            lcdDevice.write(MCP23017_GPIOB, data, 0, 4);
            portB = data[3];
        } catch (Exception e) {
            // not quite everything alright ;)
            e.printStackTrace();
        }
    }

    public void write(String message) {
        try {
            pollToClear();

            int stringValueLength = message.length();
            int dataLength = 4 * stringValueLength;

            byte[] data = new byte[dataLength];
            int bitMask = portB & 0b00000001;
            bitMask |= 0b10000000;

            for (int i = 0; i < stringValueLength; i++) {
                // Append 4 bytes to list representing PORTB over time.
                // First the high 4 data bits with strobe (enable) set
                // and unset, then same with low 4 data bits (strobe 1/0).
                byte[] bytes = out4(bitMask, message.charAt(i));
                for (int j = 0; j < 4; j++) {
                    data[(i * 4) + j] = bytes[j];
                }

            }

            // original block:
            // I2C block data write is limited to 32 bytes max.
            // If limit reached, write data so far and clear.
            // Also do this on last byte if not otherwise handled.
            // java version:
            // write whole message at once
            lcdDevice.write(MCP23017_GPIOB, data, 0, dataLength);
            portB = data[dataLength - 1];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void pollToClear() throws IOException {
        // The speed of LCD accesses is inherently limited by I2C through the
        // port expander.  A 'well behaved program' is expected to poll the
        // LCD to know that a prior instruction completed.  But the timing of
        // most instructions is a known uniform 37 mS.  The enable strobe
        // can't even be twiddled that fast through I2C, so it's a safe bet
        // with these instructions to not waste time polling (which requires
        // several I2C transfers for reconfiguring the port direction).
        // The D7 pin is set as input when a potentially time-consuming
        // instruction has been issued (e.g. screen clear), as well as on
        // startup, and polling will then occur before more commands or data
        // are issued.

        // If pin D7 is in input state, poll LCD busy flag until clear.
        if ((ddrb & 0b00000010) != 0) {
            int lo = (portB & 0b00000001) | 0b01000000;
            int hi = lo | 0b00100000; // E=1 (strobe)
            lcdDevice.write(MCP23017_GPIOB, (byte) lo);
            while (true) {
                // Strobe high (enable)
                lcdDevice.write((byte) hi);
                // First nybble contains busy state
                int aByte = lcdDevice.read();
                // Strobe low, high, low.  Second nybble (A3) is ignored.
                lcdDevice.write(MCP23017_GPIOB, new byte[]{(byte) lo, (byte) hi, (byte) lo}, 0, 3);
                if ((aByte & 0b00000010) == 0) {
                    break;
                }
            }

            portB = lo;
            // Polling complete, change D7 pin to output
            ddrb &= 0b11111101;
            lcdDevice.write(MCP23017_IODIRB, (byte) ddrb);
        }
    }

    // Low-level 4-bit interface for LCD output.  This doesn't actually
    // write data, just returns a byte array of the PORTB state over time.
    // Can concatenate the output of multiple calls (up to 8) for more
    // efficient batch write.
    private byte[] out4(int bitMask, int value) {
        int hi = bitMask | FLIP[value >> 4];
        int lo = bitMask | FLIP[value & 0x0F];
        return new byte[]{(byte) (hi | 0b00100000), (byte) hi, (byte) (lo | 0b00100000), (byte) lo};
    }

    public void stop() throws IOException {
        portA = 0b11000000; // Turn off LEDs on the way out
        portB = 0b00000001;

        lcdDevice.write(MCP23017_IOCON_BANK1, (byte) 0);
        byte[] buffer = {
                0b00111111,//IODIRA
                (byte) ddrb,//IODIRB
                0b00000000,//IPOLA
                0b00000000,//IPOLB
                0b00000000,//GPINTENA
                0b00000000,//GPINTENB
                0b00000000,//DEFVALA
                0b00000000,//DEFVALB
                0b00000000,//INTCONA
                0b00000000,//INTCONB
                0b00000000,//IOCON
                0b00000000,//IOCON
                0b00111111,//GPPUA
                0b00000000,//GPPUB
                0b00000000,//INTFA
                0b00000000,//INTFB
                0b00000000,//INTCAPA
                0b00000000,//INTCAPB
                (byte) portA,//GPIOA
                (byte) portB,//GPIOB
                (byte) portA,//OLATA
                (byte) portB//OLATB
        };

        lcdDevice.write(buffer, 0, buffer.length);
    }

    public void clear() {
        writeInternalCommand(LCD_CLEARDISPLAY);
    }

    public void home() {
        writeInternalCommand(LCD_RETURNHOME);
    }

    public void setCursor(int col, int row) {

        if (row > numLines) {
            row = numLines - 1;
        } else if (row < 0) {
            row = 0;
        }
        writeInternalCommand(LCD_SETDDRAMADDR | (col + ROW_OFFSETS[row]));
    }

    /**
     * Turn the display on (quickly)
     */
    public void display() {
        displayControl |= LCD_DISPLAYON;
        writeInternalCommand(LCD_DISPLAYCONTROL | displayControl);
    }

    /**
     * Turn the display off (quickly)
     */
    public void noDisplay() {
        displayControl &= ~LCD_DISPLAYON;
        writeInternalCommand(LCD_DISPLAYCONTROL | displayControl);
    }

    /**
     * Underline cursor on
     */
    public void cursor() {
        displayControl |= LCD_CURSORON;
        writeInternalCommand(LCD_DISPLAYCONTROL | displayControl);
    }

    /**
     * Underline cursor off
     */
    public void noCursor() {
        displayControl &= ~LCD_CURSORON;
        writeInternalCommand(LCD_DISPLAYCONTROL | displayControl);
    }

    /**
     * Toggles the underline cursor On/Off
     */
    public void toggleCursor() {
        displayControl ^= LCD_CURSORON;
        writeInternalCommand(LCD_DISPLAYCONTROL | displayControl);
    }

    /**
     * Turn on the blinking cursor
     */
    public void blink() {
        displayControl |= LCD_BLINKON;
        writeInternalCommand(LCD_DISPLAYCONTROL | displayControl);
    }

    /**
     * Turn off the blinking cursor
     */
    public void noBlink() {
        displayControl &= ~LCD_BLINKON;
        writeInternalCommand(LCD_DISPLAYCONTROL | displayControl);
    }

    /**
     * Toggles the blinking cursor
     */
    public void toggleBlink() {
        displayControl ^= LCD_BLINKON;
        writeInternalCommand(LCD_DISPLAYCONTROL | displayControl);
    }

    /**
     * These commands scroll the display without changing the RAM
     */
    public void scrollDisplayLeft() {
        displayShift = LCD_DISPLAYMOVE | LCD_MOVELEFT;
        writeInternalCommand(LCD_CURSORSHIFT | displayShift);
    }

    /**
     * These commands scroll the display without changing the RAM
     */
    public void scrollDisplayRight() {
        displayShift = LCD_DISPLAYMOVE | LCD_MOVERIGHT;
        writeInternalCommand(LCD_CURSORSHIFT | displayShift);
    }

    /**
     * This is for text that flows left to right
     */
    public void leftToRight() {
        displayMode |= LCD_ENTRYLEFT;
        writeInternalCommand(LCD_ENTRYMODESET | displayMode);
    }

    /**
     * This is for text that flows right to left
     */
    public void rightToLeft() {
        displayMode &= ~LCD_ENTRYLEFT;
        writeInternalCommand(LCD_ENTRYMODESET | displayMode);
    }

    /**
     * This will 'right justify' text from the cursor
     */
    public void autoscroll() {
        displayMode |= LCD_ENTRYSHIFTINCREMENT;
        writeInternalCommand(LCD_ENTRYMODESET | displayMode);
    }

    /**
     * This will 'left justify' text from the cursor
     */
    public void noAutoscroll() {
        displayMode &= ~LCD_ENTRYSHIFTINCREMENT;
        writeInternalCommand(LCD_ENTRYMODESET | displayMode);
    }

    public void createChar(int location, int bitmap) {
        writeInternalCommand(LCD_SETCGRAMADDR | ((location & 7) << 3));
        writeBitmap(bitmap);
        writeInternalCommand(LCD_SETDDRAMADDR);
    }

    public void message(String text) {
        message(text, NO_TRUNCATE);
    }

    /**
     * Send string to LCD. Newline wraps to second line
     *
     * @param text
     * @param truncate
     */
    public void message(String text, int truncate) {
        String[] lines = text.split("\n");//Split at newline(s)
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Integer address = LINE_ADDRESSES.get(i);
            if (address != null) {
                writeInternalCommand(address); // set DDRAM address to line
            }
            // Handle appropriate truncation if requested.
            int lineLength = line.length();
            if (truncate == TRUNCATE && lineLength > numCols) {
                write(line.substring(0, numCols));
            } else if (truncate == TRUNCATE_ELLIPSIS && lineLength > numCols) {
                write(line.substring(0, numCols - 3) + "...");
            } else {
                write(line);
            }
        }
    }

    /**
     * Set background color
     * @param color
     * @throws Exception
     */
    public void backlight(int color) throws Exception {
        int c = ~color;
        portA = (portA & 0b00111111) | ((c & 0b011) << 6);
        portB = (portB & 0b11111110) | ((c & 0b100) >> 2);
        // Has to be done as two writes because sequential operation is off.
        lcdDevice.write(MCP23017_GPIOA, (byte) portA);
        lcdDevice.write(MCP23017_GPIOB, (byte) portB);
    }

    // Read state of single button
    public int buttonPressed(int b) throws Exception {
        return lcdDevice.read(MCP23017_GPIOA) >> b & 1;
    }

    // Read and return bitmask of combined button state
    public int buttons() throws Exception {
        return lcdDevice.read(MCP23017_GPIOA) & 0b11111;
    }

}
