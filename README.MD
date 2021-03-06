## Raspberry PI CharLCDPlate java support

### About
This is a straight forward java implementation of [adafruit-git]s python code for the CharLCDPlate. The plate for the
raspberry pi can be found [here at adafruit][adafruit-plate]

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

#### How to execute the sample
1. Mount the lcd plate to your pi.
2. Fire up the code by enter `./gradlew run` in the project root directory.

[adafruit-git]:https://github.com/adafruit/Adafruit-Raspberry-Pi-Python-Code/blob/master/Adafruit_CharLCDPlate/Adafruit_CharLCDPlate.py
[adafruit-plate]:http://www.adafruit.com/product/1109