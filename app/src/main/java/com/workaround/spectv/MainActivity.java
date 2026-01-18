package com.workaround.spectv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;


/*
 * Main Activity class that loads {@link MainFragment}.
 */
public class MainActivity extends FragmentActivity  {

    final String uaString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:147.0) Gecko/20100101 Firefox/147.0";
    final String guideUrl = "https://watch.spectrum.net/guide";
    final String baseLiveChannelURL = "https://watch.spectrum.net/livetv?tmsid=";
    final String newSessionURL = "https://watch.spectrum.net/?sessionOverride=true";
    String DEFAULTCHANNEL = "0";
    final Boolean DEBUGON = true;
    Boolean specPlayerReady = false;
    String specPlayerQueue = "";
    WebView spectrumPlayer;
    WebView spectrumGuide;
    TextView chNumTextView;
    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEdit;
    String lastChannelURL;
    String cookies;
    boolean guideLoaded = false;
    boolean loginRequired = false;
    public static boolean restartingFromDreaming = false;
    static long onStopEpochSeconds = 0;
    boolean miniGuideIsShowing = false;
    GuideManager guideManager = null;

    static int TESTNUM = 1;


    String playerInitJS = """
var loopVar = setInterval(
        function () {
        Spectv.MyDebug('page loading ' +
            window.location.pathname + ' starting playerInitJS');
        try {
            // Handle away from home prompt
            document.querySelector("#kite-modal-container > div > div.kite-modal-dialog.kite-card > div.kite-modal-body > kite-alert > div > button")?.click();
            // Accept initial prompts
            if (document.querySelector('.continue-button')?.childNodes?.length > 0) {
                // Iterate through the child nodes
                nodeList = document.querySelector('.continue-button').childNodes;
                nodeList.forEach((node, index) => {
                    console.log(`Node ${index}:`, node.nodeName);
                    if (node.nodeName === 'KITE-BUTTON') {
                        node.click();
                    }
                });
            }

            document.querySelector('[aria-label*="Continue and accept"]')?.click();
            document.querySelector('.btn-success')?.click();
            // Hide html elements except video player
            $('.site-header').attr('style', 'display: none');
            $('#video-controls').attr('style', 'display: none');
            $('.nav-triangle-pattern').attr('style', 'display: none');
            $('channels-filter').attr('style', 'display: none');
            $('.transparent-header').attr('style', 'display: none');
            // Style mini channel guide
            $('#channel-browser').attr('style', 'height: 100%');
            $('.mini-guide').attr('style', 'height: 100%');
            // To help with navigation with remote. Doesn't seem to do anything though
            $('#channel-browser').attr('style', 'tabindex: 0');
            $('#spectrum-player').attr('style', 'tabindex: 0');
            $('.site-footer').attr('style', 'display: none');
            if (Spectv.channelIsQueued()) {
                Spectv.MyDebug('channel is queued, clear loopvar running playerInitJS');
                clearInterval(loopVar);
            };
            Spectv.MyDebug('about to check video length running playerInitJS');
            if ($('video')?.length > 0) {
                // Max volume
                $('video')[0].volume = 1.0;

                // force unmute
                // check for mute
                var specmute = $('#volume-control-icon').attr('aria-pressed');
                 if ( specmute === 'true' ) {
                    $('video')[0].muted = true;
                    $('#volume-control-icon').click();
                 } else {
                    // spectv not muted, force video to unmuted
                    $('video')[0].muted = false;
                 }
                // Load Guide
                Spectv.MyDebug('start preloading guide running playerInitJS');
                Spectv.preloadGuide();
                Spectv.MyDebug('done preloading guide running playerInitJS ');
                clearInterval(loopVar);

                // gigem - Wat for for and click the Still there? Continue button.
                var observer = new MutationObserver(function (mutations) {
                    for (mutation of mutations) {
                        for (addedNode of mutation.addedNodes) {
                            var button = document.evaluate(
                                    "//button[contains(text(), 'Continue')]",
                                    addedNode, null,
                                    XPathResult.FIRST_ORDERED_NODE_TYPE,
                                    null).singleNodeValue;
                            if (button) {
                                Spectv.MyDebug('clicking 4hr continue button');
                                console.log('clicking continue button');
                                button.click();
                                return;
                            }
                        }
                    };
                });
                observer.observe(document.body, {
                    subtree: true,
                    childList: true
                });

            }
        } catch (e) {
            console.log('ERROR in livetv', e)
        }
    }, 1000); // end loopvar

if (Spectv.channelIsQueued()) {
    Spectv.MyDebug('channel is queued, reload URL, running playerInitJS');
    Spectv.reloadStartupChannel();
} else {
    Spectv.setSpecPlayerReady();
};

function toggleGuide(s) {
    Spectv.channelGuide(s)
};
function toggleMiniGuide(s) {
    Spectv.channelGuide(s)
};

// check if video is ready, when ready create the guide database
// if required
var loopActiveDB = setInterval(function () {
    try {
        var vready = $('video')[0];
        if (vready && vready.readyState == 4) {
            Spectv.MyDebug('loopActiveDB,  build guide db if needed');
            Spectv.sortMiniGuide();
            clearInterval(loopActiveDB);
        };
    } catch (e) {
        console.log('ERROR in sortMiniGuide monitoring', e);
    }
}, 500);
             """;

    String guideInitJS = """
            Spectv.MyDebug('starting guideInitJS');
            var loopVar = setInterval(
               function() {
                  try {
                     $('.site-footer-wrapper').attr('style', 'display: none');
                     $('.top-level-nav').attr('style', 'display: none');
                     $('.navbar').attr('style', 'display: none');
                     $('.time-nav').attr('style', 'display: none');
                     $('.guide').attr('style', 'width: 100%');
                     $('.guide-header.container.ng-scope').attr('style', 'width: 100%');
                     $('.site-footer').attr('style', 'display: none');
                     $('.filter-section').attr('style', 'display: none');
                     $("[role = 'tablist']").attr('style', 'display: none');

                     if ($('.channel-content').length > 0 && !$('.channel-content').is(':focus')) {
                        $('.channel-content-list-container').attr('style', 'tabindex: 1');
                 //       $('.channel-content-list-container').focus();
                     }
                     $('.channel-content-list-container').unbind('click');
                     $('.channel-content-list-container').on('click', function(event) {
                        event.preventDefault();
                        event.stopImmediatePropagation();
                        Spectv.guidePlayChannel(
                           new URL(event.target.href).searchParams.get('displayChannel'),
                           new URL(event.target.href).searchParams.get('tmsGuideServiceId'))
                     });
                  } catch (error) {
                     console.log('ERROR in guide', error);
                  }
               }, 2000);
            """;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPref = this.getSharedPreferences("com.workaround.spectv.pref", Context.MODE_PRIVATE);
        sharedPrefEdit = sharedPref.edit();
        MyDebug("start onCreate ");
        specPlayerReady = false;
        restartingFromDreaming = false;
        onStopEpochSeconds = 0;
        specPlayerQueue = "";

        // load the miniguide cache
        guideManager = new GuideManager(this);
        guideManager.readMiniGuideCache();
        if (guideManager.guideCacheIsReady()) {
            DEFAULTCHANNEL = guideManager.getDefaultChannel();
        }

        String curchnum = "";
        String[] data = parseIntentFilter(getIntent());
        String channelNum = data[0];
        String channelId = data[1];

        lastChannelURL = sharedPref.getString("lastChannel", "");
        if (data[1] != null && !data[1].isEmpty()) {
            lastChannelURL = baseLiveChannelURL + channelId;
            curchnum = channelNum;
        } else {
            curchnum = sharedPref.getString("currentChannel", DEFAULTCHANNEL);
        }
        MyDebug("Starting Spectv channel " + curchnum +
                "\n Found " +  guideManager.numberOfChannels() + " channels");
        Toast.makeText(getBaseContext(), "Starting Spectv channel " + curchnum +
                        "\n Found " +  guideManager.numberOfChannels() + " channels",
                Toast.LENGTH_LONG).show();

        chNumTextView = findViewById(R.id.chNumTextView);
        initGuide();
        spectrumGuide.addJavascriptInterface(this, "Spectv");
        initPlayer();
        spectrumPlayer.addJavascriptInterface(this, "Spectv");

    }


    @Override
    public void onResume() {
        super.onResume();
        MyDebug("onResume  ");
    }

    // Pause/Resume webview on app going to background
    @Override
    public void onPause(){
        super.onPause();
        MyDebug("onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        onStopEpochSeconds = System.currentTimeMillis() / 1000L;
        var videoStopjs = """
                $('video')[0].pause();
                $('video')[0].currentTime = 0;
                """;
   //     startRepeatingTask();
        spectrumPlayer.evaluateJavascript(videoStopjs,null);
    //    spectrumPlayer.loadUrl("file:///android_asset/Spectv.html");
        MyDebug("onStop() epoch  seconds  = " + onStopEpochSeconds);
    }

    @Override
    public void onDestroy() {
      //  stopRepeatingTask();
        if (spectrumPlayer != null) {
            // Stop any ongoing loading
            spectrumPlayer.stopLoading();

            // Clear history and cache
            //  webView.clearHistory();
            //   webView.clearCache(true);
            //   webView.clearFormData();

            // Remove from parent view to avoid leaks
            ViewGroup parent = (ViewGroup) spectrumPlayer.getParent();
            if (parent != null) {
                parent.removeView(spectrumPlayer);
            }

            // Destroy the WebView completely
            spectrumPlayer.removeAllViews();
            spectrumPlayer.destroy();

            // Nullify reference
            spectrumPlayer = null;
        }
        if (spectrumGuide != null) {
            // Stop any ongoing loading
            spectrumGuide.stopLoading();

            // Clear history and cache
            //  webView.clearHistory();
            //   webView.clearCache(true);
            //   webView.clearFormData();

            // Remove from parent view to avoid leaks
            ViewGroup parent = (ViewGroup) spectrumGuide.getParent();
            if (parent != null) {
                parent.removeView(spectrumGuide);
            }

            // Destroy the WebView completely
            spectrumGuide.removeAllViews();
            spectrumGuide.destroy();

            // Nullify reference
            spectrumGuide = null;
        }

        super.onDestroy();
        MyDebug("onDestroy()");
    }

    @Override
    public void onRestart() {
        super.onRestart();
        Toast.makeText(getBaseContext(), "Restarting Spectv  ",
                Toast.LENGTH_LONG).show();
        MyDebug("onRestart intents = " + getIntent() );
    }

    // gigem - intent can be passed two ways, -e channelId <tsmid>
    // or https://watch.spectrum.net/livetv?channelId=<tsmid>
    // adding support for channel number,  -e channelNum <chnum>
    //  https://watch.spectrum.net/livetv?channelNum=<chnum>
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        MyDebug("onNewIntent intent = " + intent);
 //       stopRepeatingTask();
        String[] info = parseIntentFilter(intent);
        // info[0] is chnum, infor[1] is tssmid
        // check if restarting and what channel is requested
        String curchnum = sharedPref.getString("currentChannel", DEFAULTCHANNEL);

        //   Fix issue with resume from background buffering for a long time. Seems to be
        // and issue with timeline being behind current time when device goes to sleep. Doing a load on the video
        // seems to fix the problem, buffers get flushed and timeline is updated to current time
        setRestartFromDreaming();
        if (!info[0].isEmpty() && !curchnum.equals(info[0]) ) {
            MyDebug("onNewIntent playing new ch = " + info[0] );
            restartingFromDreaming = false;
         //   spectrumPlayer.evaluateJavascript(videoPlayjs,null);
            miniGuidePlayChannel(info[0], info[1]);
            return;
        }
        if (info[0].isEmpty() ) {
            info[0] = curchnum;
            info[1] = guideManager.getTsmid(curchnum);
        }
       if (restartingFromDreaming) {
            restartingFromDreaming = false;
            specPlayerReady = false;
            MyDebug("onNewIntent navToChannel  ch = " + info[0] );
            navToChannel(info[1],info[0],false);
       } else {
            MyDebug("onNewIntent  ch = " + info[0] + "already playing" );
            var videoPlayjs = """
               // reset mute if needed
                $('video')[0].play();
                """;

           // set video to play
            miniGuidePlayChannel(info[0], info[1]);
        //    spectrumPlayer.evaluateJavascript(videoPlayjs,null);

       }
    }


    private  void setRestartFromDreaming() {
        long currentsec = System.currentTimeMillis() / 1000L;
        long reloadmax = 5 * 60;restartingFromDreaming = false;
        MyDebug("setRestartFromDreaming  onStopEpochSeconds = " + onStopEpochSeconds );
        MyDebug("setRestartFromDreaming  sleep time = " + (currentsec - onStopEpochSeconds) );
        if ( (currentsec - onStopEpochSeconds) > reloadmax && onStopEpochSeconds != 0 ) {
            restartingFromDreaming = true;
        }
    }


    private String[] parseIntentFilter(Intent intent) {
        String[] info = new String[2];
        String channelId = "";
        String channelNum = "";
        Uri intentData = intent.getData();
        if (intentData != null) {
            Uri uri=Uri.parse(intentData.toString());
            channelId = uri.getQueryParameter("channelId");
            channelNum = uri.getQueryParameter("channelNum");
        } else {
            channelId = intent.getStringExtra("channelId");
            channelNum = intent.getStringExtra("channelNum");
        }
        // setup return values
        if (channelNum != null && !channelNum.isEmpty())  {
            channelId = guideManager.getTsmid(channelNum);
        } else if (channelId != null && !channelId.isEmpty()) {
            channelNum = guideManager.getChnum(channelId);
        }  else {
            channelId = "";
            channelNum = "";
        }
        info[0] = channelNum;
        info[1] = channelId;
        MyDebug("parseIntentFilter() channelNum = " + channelNum + " tsmid = " + channelId );
        return info;
    }


  /*  public void TESTMUTE() {
        String js = "";
        TESTNUM = TESTNUM +1;
        if ( TESTNUM > 3 ) {
            TESTNUM = 1;
        }

        // test 1 mute both
        if ( TESTNUM == 1 ) {
            MyDebug("MuteTest() test 1 mute both");
            js = """
                    var specmute = $('#volume-control-icon').attr('aria-pressed');
                    if (specmute === 'true') {
                        $('video')[0].muted = true;
                    } else {
                       $('#volume-control-icon').click();
                       $('video')[0].muted = true;
                    }
                    """;
        }

        // test 2 mute spect, unmute video
        if ( TESTNUM == 2 ) {
            MyDebug("MuteTest() test 2 mute spect, unmute video");
            js = """
                    var specmute = $('#volume-control-icon').attr('aria-pressed');
                    if (specmute === 'true') {
                        $('video')[0].muted = false;
                    } else {
                       $('#volume-control-icon').click();
                       $('video')[0].muted = false;
                    }
                    """;
        }

        // test 3 unmute spec , mute video
        if ( TESTNUM == 3 ) {
            MyDebug("MuteTest() test 3 unmute spec , mute video");
            js = """
                    var specmute = $('#volume-control-icon').attr('aria-pressed');
                    if (specmute === 'false') {
                        $('video')[0].muted = true;
                    } else {
                       $('#volume-control-icon').click();
                       $('video')[0].muted = true;
                    }
                    """;
        }
        spectrumPlayer.evaluateJavascript(js, null);
    }
*/

    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        // Handle key events to consistently bring up the mini channel guide
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if ( loginRequired ) {
                return super.dispatchKeyEvent(event);
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP && spectrumGuide.getVisibility() == View.GONE && !miniGuideIsShowing) {
                // Simulate clicking on the video player which brings up the mini channel guide (just like on desktop)
                //spectrumPlayer.evaluateJavascript("$('#spectrum-player').focus().click();", null);

//                spectrumGuide.evaluateJavascript("window.location.href;", new ValueCallback<String>() {
//                    @Override
//                    public void onReceiveValue(String currentURL) {
//                        currentURL = currentURL.replaceAll("^\"|\"$", "");
//                        if (!currentURL.equals(guideUrl)) {
//                            spectrumGuide.evaluateJavascript("history.go(-(history.length -1))", null);
//                        }
//                        spectrumPlayer.evaluateJavascript("toggleGuide('SHOW');", null);
//                    }
//                });

                if (!guideManager.guideCacheIsReady()) {
                    MyDebug("Error  dispatchKeyEvent - Guide NOT AVAILABLE");
                    Toast.makeText(getBaseContext(), "Guide NOT AVAILABLE",
                            Toast.LENGTH_LONG).show();
                    return false;
                }

                spectrumPlayer.evaluateJavascript("toggleGuide('SHOWGUIDE');", null);
//              scroll Guide to current channel playing
                String curchannel = sharedPref.getString("currentChannel",DEFAULTCHANNEL);
                scrollToGuideChannel(curchannel);
                return true;
            }

            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN && spectrumGuide.getVisibility() == View.GONE && !miniGuideIsShowing) {
                // toggle closed caption
                spectrumPlayer.evaluateJavascript("$('.closed-caption').click();", null);
                return true;
            }


            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && spectrumGuide.getVisibility() != View.GONE) {
                if (spectrumGuide.canGoBack()) {
                    MyDebug("keycode back, guide is visable - can go back");
                    spectrumGuide.evaluateJavascript("history.back();", null);
                } else {
                    MyDebug("keycode back, guide is visable - can NOT go back");
                    spectrumPlayer.evaluateJavascript("toggleGuide('HIDEGUIDE');", null);
                }
                return true;
            }

            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && miniGuideIsShowing) {
                toggleMiniGuideWindow("CLOSE");
                return true;
            }

            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT && spectrumGuide.getVisibility() == View.GONE) {
                // ignore keyevent until app is ready, ie. miniguide data is loaded
                if (!guideManager.guideCacheIsReady()) {
                    MyDebug("Error  dispatchKeyEvent - MiniGuide NOT AVAILABLE");
                    Toast.makeText(getBaseContext(), "MiniGuide NOT AVAILABLE",
                            Toast.LENGTH_LONG).show();
                    return false;
                }

                if (miniGuideIsShowing) {
                    toggleMiniGuideWindow("CLOSE");
                } else {
                    toggleMiniGuideWindow("OPEN");
                    String curchannel = sharedPref.getString("currentChannel",DEFAULTCHANNEL);
                    String tsmid = guideManager.getTsmid(curchannel);
                    scrollToMiniGuideChannel(curchannel,tsmid);
                }
                return true;
            }

            //  add  keyboard interface
            if (event.getKeyCode() >= KeyEvent.KEYCODE_0 && event.getKeyCode() <= KeyEvent.KEYCODE_9) {
                MyDebug("KEYCODE EVENT " + Character.toString((char) event.getUnicodeChar()));
                CharSequence temp = chNumTextView.getText();
                String chNumText = temp + Character.toString((char) event.getUnicodeChar());
                chNumTextView.setText(chNumText);
                if (temp.equals("")) {
                    countDownChNum();
                }
                return true;
            }
            if ( event.getKeyCode() == KeyEvent.KEYCODE_ENTER ||
                    event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER )  {
                String chNumText = (String) chNumTextView.getText();
                if (  !chNumText.equals("") ) {
                    chNumTextView.setText("");
                    handleChNumEvent(chNumText);
                    return true;
                } else if (miniGuideIsShowing) {
                    // user hits enter on channel to play
                    dispatchMiniGuideEvent();
                    return true;
                }
            }
            if ((event.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_UP ||
                    event.getKeyCode() == KeyEvent.KEYCODE_PLUS ||
                    event.getKeyCode() == KeyEvent.KEYCODE_PAGE_UP) &&
                    spectrumPlayer.getVisibility() == View.VISIBLE ) {
                String curchannel = sharedPref.getString("currentChannel",DEFAULTCHANNEL);
                EpgMapData epg = guideManager.getChannelUp(curchannel);
                String chnum = epg.chnum;
                miniGuidePlayChannel(chnum, epg.tsmid);
                return true;
            }

            if ((event.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_DOWN||
                    event.getKeyCode() == KeyEvent.KEYCODE_MINUS ||
                    event.getKeyCode() == KeyEvent.KEYCODE_PAGE_DOWN) &&
                    spectrumPlayer.getVisibility() == View.VISIBLE ) {
                String curchannel = sharedPref.getString("currentChannel",DEFAULTCHANNEL);
                EpgMapData epg = guideManager.getChannelDown(curchannel);
                String chnum = epg.chnum;
                miniGuidePlayChannel(chnum, epg.tsmid);
                return true;
            }

            if ((event.getKeyCode() == KeyEvent.KEYCODE_LAST_CHANNEL ||
                    event.getKeyCode() == KeyEvent.KEYCODE_DEL ||
                    event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT  ) &&
                    spectrumPlayer.getVisibility() == View.VISIBLE &&
                    !miniGuideIsShowing && spectrumGuide.getVisibility() == View.GONE ) {
                String newchannel = sharedPref.getString("prevChannel",DEFAULTCHANNEL);
                EpgMapData epg = guideManager.getGuideEntry(newchannel);
            //    TESTMUTE();
                miniGuidePlayChannel(epg.chnum, epg.tsmid);
                return true;
            }

            MyDebug("Not handled key event down " + event.getKeyCode());
        }

        return super.dispatchKeyEvent(event);
    }

    private void handleChNumEvent(String chnum) {
        if (spectrumGuide.getVisibility() == View.VISIBLE) {
            scrollToGuideChannel(chnum);
        } else if (spectrumPlayer.getVisibility() == View.VISIBLE) {
            MyDebug("chnumber = " + chnum + " ENTER KEY");
            if (miniGuideIsShowing) {
                scrollToMiniGuideChannel(chnum, "");
            } else {
                String mappedTsmid = guideManager.getTsmid(chnum);
                if (!mappedTsmid.trim().isEmpty()) {
                    if (specPlayerReady && !restartingFromDreaming) {
                        miniGuidePlayChannel(chnum, mappedTsmid);
                    } else {
                        MyDebug("adding chnumber = " + chnum + " to specPlayerQueue");
                        specPlayerQueue = chnum;
                    }
                } else {
                    Toast.makeText(getBaseContext(), "Channel " + chnum + " not found",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void countDownChNum() {
        new CountDownTimer(2400, 200) {
            public void onTick(long ms) {
                // if textview is empty, cancel countdown
                if (chNumTextView.getText().equals("")) {
                    cancel();
                }
            }

            public void onFinish() {
                // if textvalue not empty, process channel number
                String chnum = (String)chNumTextView.getText();
                if ( !chnum.equals("") ) {
                    chNumTextView.setText("");
                    handleChNumEvent(chnum);
                }
            }
        }.start();
    }

    private void dispatchMiniGuideEvent() {
        String playEventJS = """
                   var strArr = document?.activeElement?.id.split('-');
                           var channelId = strArr[strArr.length - 1];
                           var channelNum = strArr[strArr.length - 2];
                           Spectv.MyDebug('dispatchMiniGuideEvent() --  tsmid =' + channelId + ' chnum  = ' +  channelNum);
                           Spectv.miniGuidePlayChannel(channelNum,channelId);
                    
                """;
        spectrumPlayer.evaluateJavascript(playEventJS, null);
        toggleMiniGuideWindow("CLOSE");
    }


    private void scrollToMiniGuideChannel(String chnum, String tsmid) {
        MyDebug("Starting  scrollToMiniGuideChannel " + chnum);
        EpgMapData mapdata = guideManager.getGuideEntry(chnum);
        if ( mapdata == null ) {
            //  add function to find a nearby channel
            chnum = guideManager.getAvailableChnum(chnum);
            mapdata = guideManager.getGuideEntry(chnum);
            MyDebug("Starting  scrollToMiniGuideChannel nearby channel  " + chnum);
        }
        String offset = mapdata.offset;
        String cssid = mapdata.cssid;
        MyDebug("Starting  scrollToMiniGuideChannel chnum =" + chnum + " offset = " + offset);

        String scrollToMiniGuideChannelJS =
                // set focus after DOM settles
                "function setfocus() {" +
                        "$( '" + cssid + "').focus(); " +
                        "};" +
                        "$('ul#channel-browser.channel-list.ng-scope').animate({ scrollTop: " +
                        offset + "} , 'slow', function () { " +
                        "    setTimeout(setfocus, 20);" +
                        " });";

        spectrumPlayer.evaluateJavascript(scrollToMiniGuideChannelJS, null);
    }

    private void scrollToGuideChannel(String chnumber) {
        String chnum = chnumber;
        MyDebug("Starting  scrollToGuideChannel "  + chnum);
        EpgMapData mapdata = guideManager.getGuideEntry(chnum);
        if ( mapdata == null ) {
            //  add function to find a nearby channel
            chnum = guideManager.getAvailableChnum(chnum);
            MyDebug("Starting  scrollToGuideChannel nearby channel  " + chnum);
        }
        String scrollToGuideChannelJS2 =
                "var chnum = '" + chnum + "' ;" +
                        """
                                    var element;
                                    
                                    // Accept initial prompts, out of home network
                                 
                                 document.querySelector("#kite-modal-container > div > div.kite-modal-dialog.kite-card > div.kite-modal-body > kite-alert > div > button")?.click();
                                                                     
                                    function setfocus() {
                                    // get timeline offset to position right px
                                       var nowMil = new Date().getTime();
                                       var roundedMil = Math.floor(nowMil/1000/60/30) * 30 * 60 * 1000;
                                       var timeline = $('[time="' + roundedMil + '"]')[0].getBoundingClientRect();
                                    // get the row offset px from top viewport
                                       var domrect = element[0].getBoundingClientRect();
                                       document.elementFromPoint(timeline.left, domrect.top).focus();
                                    };
                                    var loopvar = setInterval(
                                       function() {
                                          if ($('.channel-heading p.channel-number').length > 0 &&
                                            $('.channel-content').length > 0 ) {
                                             try {
                                                // move guide to current time
                                                $('.filter-section').attr('style', 'display: inline');
                                                $('.col-md-2.now').click();
                                                $('.filter-section').attr('style', 'display: none');
                                                element = $('.channel-heading p.channel-number').filter(function(index) {
                                                   return $(this).text() === chnum;
                                                });
                                                                            
                                                let offset = element[0].offsetTop;
                                                 Spectv.MyDebug('scrollToGuideChannelJS offsetTop = ' +  offset );    
                                                $('.main-content-wrapper').animate({
                                                   scrollTop: offset
                                                }, 'slow', function() {
                                                   setTimeout(setfocus, 250);
                                                });
                                             } catch (error) {
                                                Spectv.MyDebug('scrollToGuideChannelJS exception = ' +  error.toString() );
                                             };
                                             clearInterval(loopvar);
                                          };
                                       }, 100);
                                """;

        MyDebug("scrollToGuideChannel chnum = " + chnum );
        spectrumGuide.evaluateJavascript(scrollToGuideChannelJS2, null);
    }

    @JavascriptInterface
    public void toggleMiniGuideWindow(String state) {
        MyDebug("In toggleMiniGuideWindow state = "  + state);
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (state.equals("CLOSE")) {
                        String closeJS =" $('mini-guide').last().removeClass('mini-guide-open');$('video').eq(0).focus();";
                        spectrumPlayer.evaluateJavascript(closeJS, null);
                        miniGuideIsShowing = false;
                        return;
                    }
                    if (state.equals("OPEN")) {
                        String openJS = " $('mini-guide').last().addClass('mini-guide-open');";
                        spectrumPlayer.evaluateJavascript(openJS, null);
                        miniGuideIsShowing = true;
                        return;
                    }
                    MyDebug("In toggleMiniGuideWindow unhandled state = " + state);
                }
            });

        } catch (Exception e) {
            Log.d("ERROR in toggleMiniGuideWindow", e.toString());
        }
    }

    @JavascriptInterface
    public boolean channelIsQueued() {
        return !specPlayerQueue.equals("");
    }

    @JavascriptInterface
    public void setLoginRequired() {
        loginRequired = true;
    }
    @JavascriptInterface
    public void setSpecPlayerReady() {
        specPlayerReady = true;
    }

    @JavascriptInterface
    public void reloadStartupChannel() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    specPlayerReady = true;
                    if (!specPlayerQueue.equals("")) {
                        MyDebug("running queued channel = " + specPlayerQueue);
                        String mappedid = guideManager.getTsmid(specPlayerQueue);
                        String queue = specPlayerQueue;
                        specPlayerQueue = "";
                        miniGuidePlayChannel(queue,mappedid);
                    }
                }
            });
        } catch (Exception e) {
            Log.d("ERROR in reloadStartupChannel", e.toString());
        }
    }


    @JavascriptInterface
    public void channelGuide(String action) {
        switch (action) {
            case "SHOWGUIDE":
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spectrumGuide.setVisibility(View.VISIBLE);
                            spectrumGuide.requestFocus();
                        }
                    });

                } catch (Exception e) {
                    Log.d("ERROR in showing", e.toString());
                }
                break;
            case "HIDEGUIDE":
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spectrumGuide.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    Log.d("ERROR in hiding", e.toString());
                }
                break;
        }
    }


    @JavascriptInterface
    public void miniGuidePlayChannel(String chnum, String tsmid ) {
        // build javascript to select channel
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EpgMapData mapdata = guideManager.getGuideEntry(chnum);
                    String offset = mapdata.offset;
                    String cssid = mapdata.cssid;
                    MyDebug("Starting  miniGuidePlayChannel chnum = " + chnum + " offset = " + offset);

                    // save the channel tsmid
                    saveLastChannel(tsmid, chnum);

                    String playChannelJS =
                            // set focus after DOM settles
                            "function waitForDOM() {" +
                                    "$('video')[0].play(); " +
                                    "$('video')[0].volume = 1.0;" +
                                    "$('" + cssid + "').focus();" +
                                    "$('" + cssid + "').click();" +
                                    "$('mini-guide').last().removeClass('mini-guide-open');" +
                                    "$('video').eq(0).focus(); " +
                            "};" +

                                    // force unmute
                                    " var status = $('#volume-control-icon').attr('aria-pressed'); " +
                                    " var status = $('#volume-control-icon').attr('aria-pressed'); " +
                                    " var videomute = $('video')[0].muted;" +
                                    " if (status === 'true' || videomute ) { " +
                                    "     Spectv.navToChannel(" + tsmid + "," + chnum + " , true ); " +
                                    "     Spectv.MyDebug(`audio mutted, reload url`); " +
                                    "} else {" +
                                          "$('ul#channel-browser.channel-list.ng-scope').animate({ scrollTop: " +
                                          offset + "} , '100', function () { " +
                                          "   setTimeout(waitForDOM, 20);" +
                                          "   }" +
                                    ");}";

                    spectrumPlayer.evaluateJavascript(playChannelJS, null);
                }

            });
        } catch (Exception e) {
            MyDebug("ERROR in miniGuidePlayChannel" + e.toString());
        }
    }

    @JavascriptInterface
    public void guidePlayChannel(String chNum, String channelId) {
        Toast.makeText(getBaseContext(), "Starting Channel  = " + chNum + "/" + channelId,
                Toast.LENGTH_SHORT).show();
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    spectrumGuide.setVisibility(View.GONE);
                    miniGuidePlayChannel(chNum,channelId);
                }
            });
        } catch (Exception e) {
            Log.d("ERROR in guidePlayChannel", e.toString());
        }
    }
    @JavascriptInterface
    public void navToChannel(String channelId, String chNum, boolean goback) {
        Toast.makeText(getBaseContext(), "Starting Channel  = " + chNum + "/" + channelId,
                Toast.LENGTH_SHORT).show();
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    var goBackJS = """
                            if ( history.length > 1 ) {
                              history.back();
                              Spectv.MyDebug(' navToChannel() going back ');
                            } else {
                              Spectv.MyDebug(' navToChannel() no history back ');
                            }
                            """;
                    saveLastChannel(channelId,chNum);
                    if (goback) {
                        spectrumGuide.evaluateJavascript(goBackJS, null);
                    }
                    spectrumPlayer.loadUrl(baseLiveChannelURL + channelId);
                    spectrumGuide.setVisibility(View.GONE);
                 }
            });
        } catch (Exception e) {
            Log.d("ERROR in live channel nav", e.toString());
        }
    }

    @JavascriptInterface
    public void saveLastChannel(String channelId, String channelNum) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String lastCurrentChannel = sharedPref.getString("currentChannel", DEFAULTCHANNEL);
                    sharedPrefEdit.putString("lastChannel", baseLiveChannelURL + channelId);
                    if (!lastCurrentChannel.equals(channelNum)) {
                        sharedPrefEdit.putString("currentChannel", channelNum);
                        sharedPrefEdit.putString("prevChannel", lastCurrentChannel);
                    }
                    sharedPrefEdit.apply();
                }
            });
        } catch (Exception e) {
            Log.d("ERROR in live channel nav", e.toString());
        }
    }



    @JavascriptInterface
    public void preloadGuide() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!guideLoaded) {
                        spectrumGuide.loadUrl(guideUrl);
                    }
                }
            });
        } catch (Exception e) {
            Log.d("ERROR in live channel nav", e.toString());
        }
    }

    private void initWebviews(WebSettings wv) {
        wv.setJavaScriptEnabled(true);
        wv.setDomStorageEnabled(true);
        wv.setMediaPlaybackRequiresUserGesture(false);
        wv.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        wv.setUserAgentString(uaString);
        wv.setCacheMode(WebSettings.LOAD_DEFAULT);
    }

    private void initPlayer() {
        spectrumPlayer = findViewById(R.id.spectv);

        //    spectrumPlayer.clearCache(true);
        spectrumPlayer.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
//                Log.d("**************PLAYER************", consoleMessage.message() + " -- From line " +
//                        consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
//                return true;
//            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                String[] resources = request.getResources();
                for (int i = 0; i < +resources.length; i++) {
                    if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resources[i])) {
                        request.grant(resources);
                        return;
                    }
                }

                super.onPermissionRequest(request);
            }

        });
        WebSettings spectrumPlayerWebSettings = spectrumPlayer.getSettings();

        initWebviews(spectrumPlayerWebSettings);

        spectrumPlayer.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                cookies = CookieManager.getInstance().getCookie(url);
                super.onPageFinished(view, url);
                MyDebug("loaded url = " + url );
                if ( url.contains("/login") && !url.contains("/login/auto")) {
                    loginRequired = true;
                }
                else if  (url.contains("/android_asset") ) {
                     return;
                } else {
                    loginRequired = false;
                    spectrumPlayer.evaluateJavascript(playerInitJS, null);
                }

            }

        });
        spectrumPlayer.setVerticalScrollBarEnabled(false);
        MyDebug("about to loadUrl " + lastChannelURL );
        spectrumPlayer.loadUrl(lastChannelURL.isEmpty() ? newSessionURL : lastChannelURL);
    }

    private void initGuide() {
        spectrumGuide = (WebView) findViewById(R.id.spectv_guide);
        spectrumGuide.setVisibility(View.GONE);

        spectrumGuide.setBackgroundColor(Color.TRANSPARENT);
     //   spectrumGuide.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        spectrumGuide.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);


        spectrumGuide.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
//                Log.d("**************GUIDE************", consoleMessage.message() + " -- From line " +
//                        consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
//                return true;
//            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                String[] resources = request.getResources();
                for (int i = 0; i < +resources.length; i++) {
                    if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resources[i])) {
                        request.grant(resources);
                        return;
                    }
                }

                super.onPermissionRequest(request);
            }


        });
        WebSettings spectrumGuideWebSettings = spectrumGuide.getSettings();
        initWebviews(spectrumGuideWebSettings);

        spectrumGuide.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!guideLoaded) {
                    guideLoaded = true;
                    CookieManager.getInstance().setCookie(guideUrl, cookies, null);
                }
                super.onPageFinished(view, url);
                spectrumGuide.evaluateJavascript(
                        guideInitJS
                        , null);
            }
        });

    }

    /////////////////////////////////////////////////////////
    //
    //  The following functions are used to Sort and Scan the MiniGuide during
    // initial install or the data for Spectv app is deleted on the device
    //
    /////////////////////////////////////////////////////////

    @JavascriptInterface
    public void sortMiniGuide() {
        if (guideManager.miniGuideScanDone() || guideManager.guideCacheIsReady() ) {
            return;
        }
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MyDebug("Starting  sortMiniGuide");
                    String sortMiniGuideJS =
                            // sort miniguide by channels number, then scan the miniguide
                            """
                             var simMouseUpEvent = new MouseEvent('mouseup', {
                                'view': window,
                                'bubbles': true,
                                'cancelable': true
                             });
                             $('channels-filter').attr('style', 'display: inline');
                             $('mini-guide').last().addClass('mini-guide-open');
                             $('channels-filter button')[0].dispatchEvent(simMouseUpEvent);
                             if (!$('channels-filter button').eq(2).hasClass("selected-item")) {
                                $('channels-filter button')[2].dispatchEvent(simMouseUpEvent);
                             } else {
                                $('channels-filter button')[0].dispatchEvent(simMouseUpEvent);
                             }
                             Spectv.MyDebug('sortMiniGuideJS done ');
                             // hide the filter button
                             $('channels-filter').attr('style', 'display: none');
                             // callback to scan miniguide
                             $('ul#channel-browser.channel-list.ng-scope').scrollTop('0');
                             Spectv.scanMiniGuide();
                             """;

                    // open the miniguide
                    MyDebug("  sortMiniGuide - about to SORT  miniguide ");
                    chNumTextView.setTextColor(Color.BLACK);
                    chNumTextView.setBackgroundColor(Color.WHITE);
                    chNumTextView.setText("Scanning MiniGuide");
                    spectrumPlayer.evaluateJavascript(sortMiniGuideJS, null);
                }
            });
        } catch (Exception e) {
            Log.d("ERROR in live channel nav", e.toString());
        }
    }

    @JavascriptInterface
    public void scanMiniGuide() {
        if (guideManager.miniGuideScanDone() || guideManager.guideCacheIsReady() ) {
            return;
        }
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MyDebug("Starting  scanMiniGuide");
                    // start scan miniguide
                    String scanMiniGuideJS3 =
                            """
                                    var rowpx = 0;
                                    var licnt = 0;
                                    var SCANDONE = false;
                                    var rowcnt = 0;
                                    var lastoffset = 0;
                                    function readChannels() {
                                       // start processing the channels
                                       if (!SCANDONE ) {
                                          rowpx = $('#channel-browser li').eq(1).height();
                                          licnt = $('#channel-browser li').length;
                                          var centeroffset = (rowpx * (licnt / 2)) - rowpx;
                                          rowcnt = 0;
                                          $('#channel-browser li').each(function(index, e) {
                                             let css = $(e).attr('id');
                                             if (css !== undefined) {
                                                rowcnt++;
                                                let chname = $(e).find('div.callsign.ng-binding').text();
                                                let chnum = $(e).find('div.channel-number.ng-binding.ng-scope').text();
                                                let offsettop = e.offsetTop - centeroffset;
                                                lastoffset = e.offsetTop;
                                                if (offsettop < 0) {
                                                   offsettop = e.offsetTop;
                                                };
                                                Spectv.addMiniGuideEntry(chnum, chname, css, offsettop.toString());
                                    
                                             } else {
                                               // must be header of footer, check if at end of miniguide
                                               let words = $(e).attr('style');
                                               // width: 100%; min-height: 0px; - end of scroll
                                                let n = words.split(" ");
                                                let h  = n[n.length - 1];
                                                if ( h === "0px;"  && rowcnt > 0 ) {
                                                     SCANDONE = true;
                                                 };
                                             };
                                          });
                                   
                                          // move past header , footers and last entry
                                          var nextpos = lastoffset + rowpx + rowpx + rowpx;
                                          $('ul#channel-browser.channel-list.ng-scope').scrollTop(nextpos.toString());
                                          $('mini-guide').last().addClass('mini-guide-open');
                                       };
                                       if (SCANDONE) {
                                          Spectv.setMiniGuideLoaded();
                                          Spectv.toggleMiniGuideWindow("CLOSE");
                                       //   $('ul#channel-browser.channel-list.ng-scope').off('scrollend');
                                         
                                       }
                                    };
                                    
                                    var scanLoop = setInterval(
                                     function() {
                                         if ( SCANDONE ) {
                                            clearInterval(scanLoop);
                                         } else {
                                           readChannels();
                                         }
                                     } , 200);
                                    
                                 
                                    """;
                    // open the miniguide
                    MyDebug("  ScanMiniGuide - about to Show miniguide ");
                    toggleMiniGuideWindow("OPEN");
                    // run javascript to scan miniguide
                    MyDebug("  ScanMiniGuide - about to Scan  miniguide ");
                    spectrumPlayer.evaluateJavascript(scanMiniGuideJS3, null);
                    MyDebug("End  ScanMiniGuide");
                }
            });
        } catch (Exception e) {
            Log.d("ERROR in live channel nav", e.toString());
        }
    }

    @JavascriptInterface
    // wrapper function to handle JS call to guideManager
    public void addMiniGuideEntry(String chnum, String chname, String css, String offsetpx) {
        if ( !chnum.equals("")) {
            guideManager.addMiniGuideEntry(chnum, chname, css, offsetpx);
        }
    }

    @JavascriptInterface
    // wrapper function to handle JS call to guideManager
    public void setMiniGuideLoaded() {
        MyDebug("in setMiniGuideLoaded");
        guideManager.setMiniGuideLoaded();
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chNumTextView.setTextColor(Color.WHITE);
                    chNumTextView.setText("");
                    chNumTextView.setBackgroundColor(Color.TRANSPARENT);
                }
            });
        } catch (Exception e) {
            MyDebug("setMiniGuideLoaded Exception " + e.toString());
        }
    }
    ///////////////////////////////////////////////////////////
    // End Sort and scan functions
    ///////////////////////////////////////////////////////////


    @JavascriptInterface
    public void MyDebug(String s1) {
        if (DEBUGON) {
            Log.d("SPECDEBUG", s1 );
        }
    }

}

