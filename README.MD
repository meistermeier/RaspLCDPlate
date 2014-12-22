## Raspberry PI CharLCDPlate java support

### About
This is a straight forward java implementation of [adafruit]s python code for the CharLCDPlate.

### Current state
The most features given by the adafruit sample exist were transferred to this java version.
#### Overview
##### Existing
- Control of Background color
- Write (two-line) messages
- Toggle cursor blink or underscore
- General movement of cursor (write position)
- Read button input

##### WIP
- Clean up code (naming, exceptions...)
- Create more samples (and physically test all functions)
- Documentation of new and existing features


[adafruit]:https://github.com/adafruit/Adafruit-Raspberry-Pi-Python-Code/blob/master/Adafruit_CharLCDPlate/Adafruit_CharLCDPlate.py