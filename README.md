# SpectrumTV_webview V2.0.5b
Spectrum TV App for Android TV platform.
Just a simple webview that launches the watch.spectrum.net website to be able to watch live tv.
Manipulates javascript to hide unnecessary html elements, etc...

V1.0  "dyay108/SpectrumTV_webview"<br>
Rigth DPad to bring up mini guide.

V1.2 "dyay108/SpectrumTV_webview"<br>
Bring up TV Guide with D-Pad Up

V1.2.1   "gigem/SpectrumTV_webview"<br>
- Added Support for Intent Filters<br>
  intent can be passed two ways, -e channelId [tsmid]<br>
  or https://watch.spectrum.net/livetv?channelId=[tsmid]<br>
- Added "Continue" , 4 hour timeout
- Minimum SDK level set to 25 to support FireStick

Example<br>
adb shell am start -n com.workaround.spectv/com.workaround.spectv.MainActivity -e  channelNum 55,<br>
adb shell am start -n com.workaround.spectv/com.workaround.spectv.MainActivity
-d  https://watch.spectrum.net/livetv?channelNum=124

V2.0
Channel Number Support / Keyboard Support
- Enter channel number followed by D-Pad Center or Enter Key<br>
  On Guide or MiniGuide scroll to channel number entered<br>
  On Player, switch to channel number entered<br>
- Channel Up/Down  -  switches to next channel or previous channel in numeric order<br>
  Up Keys -  PLUS, PAGE-UP, CHANNEL_UP<br>
  Down Keys - MINUS, PAGE-DOWN, CHANNEL_DOWN<br>
- Toggle Channel -  switches between last two channels viewed<br>
  Toggel Keys - DEL, LAST_CHANNEL, LEFT-ARROW<br>
- Sort MiniGuide by channel number
- Performance Improvements <br>
  Avoid reloading Spectrum URL when changing channels<br>
- Addition ADB Support/Intent Filters <br>
  added support for new intent scheme "spectv", avoids ambiguous "https"<br>
  added support for channel number intents,  -e channelNum [chnum] and
  spectv://watch.spectrum.net/livetv?channelNum=[chnum] <br>
- ADB sendtext/keyevent - play or scroll to channel number<br>
  adb shell input text "55" ; input keyevent 66";<br>

V2.0.1
- Bug fixes for scanning miniguide

V2.0.2
- Auto process channel number input after about 2.5 seconds, removes the requirement to hit enter/dpad center to process the channel
- Closed Caption support <br>
  On video view, arrow down button on dpad toggles "Closed Captions" off/on . <br>

V2.0.3
- Add support for "Away From Home Network", requires Spectrum Login and rescan of miniguide.
- Redesigned miniguide scan function to eliminate async scrolling and onScroll listener.

V2.0.4
- Add support for "Left Arrow (DPAD_LEFT)" to toggle previous/last channel

V2.0.5b
- Mute fix for Spectrum 'emergency alert'
- Webview Cleanup when app moved to background or closed
- Fix 'Continue' fails during initial login due to Spectrum HTML changes

### Installation
- On first startup, after the initial channel is loaded a scan of the miniguide will be started. <br>
  A message will be displayed "Scanning MiniGuide" which will close when the scan has completed, around 45 seconds.<br>
  After the scan has completed the data is saved in the filesystem the new channel number support is available.<br>
- Rescan MiniGuide<br>
  If the Spectrum lineup changes you will need to rescan the miniguide. This can be done in two ways.<br>
1. Uninstall and reinstall the "Spectv" package
2. From the Application settings on the device, Force stop "Spectv", Delete the application data, restart "Spectv"

### Assets
<a href="https://github.com/brodfueh/SpectrumTV_webview/releases/download/Spectv-2.0.5b/spectv.2.0.5b.apk">
Download Spectv v2.0.5b  apk</a>




### Notes
1. The application has only been tested on "Fire TV Stick 4K", "Chromecast with Google TV - 4K", "MECOOL KM2 PLUS Deluxe" and "Onn Google TV 4K"
2. Changes to the Spectrum website may cause Spectv to fail and require updating to address the changes
