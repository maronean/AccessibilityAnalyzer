# AccessibilityAnalyzer
An blackbox testing tool for assessing the Accessibility of Android applications. Tests adapted from the Accessibility Test Framework For Android

This is primarily designed as a research tool for testing a large set of different apps. As such it is not ideal for development and is definitely not intended to replace custom accessibility testing which should be specific to the application in test.
This tool performs checks for the following accessibility problems:

1. Incompatible Text fields: contain readable content  descriptions for accessibility tools.
2. Clickable spans used as buttons: Should not be used in place of buttons because they are less readable to screen readers.
3. Duplicate Clickable objects: Duplicate touch targets will confuse the layout for accessibility tools.
4. Redundant content descriptions: Ensure that contend description does not contain confusing or generic language (i.e. A button with the description button)
5. Missing Speak-able Text: Check to make sure that each view has some content that can be read by accessibility services like ‘TalkBack’
6. Touch target too small: Ensure that all touch targets meet minimum size requirements.


#Usage
Once packaged into an install onto an Android device running Android 5.0 or later.

Once running, testing can be performed simply by running the application to be tested. Accessibiltiy Analyzer will run passively, recording test results on all views currently in the foreground.

These results can be most easily collected using the Android Debug Bridge (adb) in the Android SDK tools.
Easiest way to collect this output is using the following command

adb -s [device name] logcat | grep "AccessibilityAnalyzer" 

#Results Format
Results will be recorded in the following format 
AccessibityAnalyzer: [name of tested package]: true/false,true/false,true/false,true/false,true/false,true/false
True or false values represent whether or not a particular test has failed atleast once in any views tested for that application. 
True = test passes
False = test failed

Order of tests correspond ot the order of tests listed above.
