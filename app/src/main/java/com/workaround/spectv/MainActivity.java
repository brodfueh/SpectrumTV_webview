package com.workaround.spectv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
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

    final String uaString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
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
    public static boolean restartingFromDreaming = false;
    boolean miniGuideIsShowing = false;
    GuideManager guideManager = null;
    DreamReceiver myReceiver = new DreamReceiver();


    String playerInitJS = """
              var loopVar = setInterval(
               function() {
                  Spectv.MyDebug('page loading ' +
                     window.location.pathname + ' starting playerInitJS');
                  try {
                     // Accept initial prompts
                     if (document.querySelector('.continue-button')?.childNodes?.length > 0) {
                        document.querySelector('.continue-button')?.childNodes[0].click();
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
                        // Load Guide
                        Spectv.MyDebug('start preloading guide running playerInitJS');
                        Spectv.preloadGuide();
                        Spectv.MyDebug('done preloading guide running playerInitJS ');
                        clearInterval(loopVar);
                        
                        // gigem - Wat for for and click the Still there? Continue button.
                         var observer = new MutationObserver(function(mutations) {
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
                  }, 1000);
               if (Spectv.channelIsQueued()) {
                  Spectv.MyDebug('channel is queued, reload URL, running playerInitJS');
                  Spectv.reloadStartupChannel();
               } else {
                  Spectv.setSpecPlayerReady();
               };
              
               function toggleGuide(s) { Spectv.channelGuide(s) };
               function toggleMiniGuide(s) { Spectv.channelGuide(s) };
              
               // check if video is ready, when ready create the guide database
               // if required
               var loopActiveDB = setInterval(function() {
                  try {
                     var vready = $('video')[0].readyState;
                     if (vready && vready == 4) {
                        Spectv.MyDebug('loopActiveDB,  build guide db if needed');
                        Spectv.sortMiniGuide();
                        clearInterval(loopActiveDB);
                     };
                  } catch (e) { console.log('ERROR in sortMiniGuide monitoring', e); }
               }, 500);
               
               // To be able to save last viewed channel from mmini guide.
               // hacky way since cant listen to click event on channel-list items
               /*  This function is no longer needed, dispatchKeyEvent() handles miniguide
                    user interactions
               var loopActiveEl = setInterval(() => {
                  try {
                     if (document?.activeElement?.id?.includes('channel-list-item-')) {
                        if ($('video')[0].paused) {
                           var strArr = document?.activeElement?.id.split('-');
                           var channelId = strArr[strArr.length - 1];
                           var channelNum = strArr[strArr.length - 2]
                           Spectv.MyDebug('loopActiveEl tsmid =' +
                              channelId + ' chnum  = ' +  channelNum);
                           Spectv.saveLastChannel(channelId, channelNum);
                           Spectv.toggleMiniGuideWindow('CLOSE');
                        }
                     }
                  } catch (e) { console.log('ERROR in miniguide monitoring', e); }
               }, 1000);
               */
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
        specPlayerQueue = "";

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DREAMING_STARTED);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

     //   DreamReceiver myReceiver = new DreamReceiver();
        registerReceiver(myReceiver, filter);

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
        MyDebug("onStop()");
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
        String[] info = parseIntentFilter(intent);
        // info[0] is chnum, infor[1] is tssmid
        // check if restarting and what channel is requested
        String curchnum = sharedPref.getString("currentChannel", DEFAULTCHANNEL);

        //   Fix issue with resume from background buffering for a long time. Seems to be
        // and issue with timeline being behind current time when device goes to sleep. Doing a load on the video
        // seems to fix the problem, buffers get flushed and timeline is updated to current time
        if (!info[0].isEmpty() && !curchnum.equals(info[0]) ) {
            MyDebug("onNewIntent playing new ch = " + info[0] );
            restartingFromDreaming = false;
            miniGuidePlayChannel(info[0], info[1]);
            return;
        }
        if (restartingFromDreaming) {
            if (info[0].isEmpty() ) {
                info[0] = curchnum;
                info[1] = guideManager.getTsmid(curchnum);
            }
            restartingFromDreaming = false;
            specPlayerReady = false;
            MyDebug("onNewIntent navToChannel  ch = " + info[0] );
            navToChannel(info[1],info[0],false);
        } else {
            MyDebug("onNewIntent  ch = " + info[0] + "already playing" );
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
    return info;
}

    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        // Handle key events to consistently bring up the mini channel guide
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
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
                    return true;
                }
                spectrumPlayer.evaluateJavascript("toggleGuide('SHOWGUIDE');", null);
//              scroll Guide to current channel playing
                String curchannel = sharedPref.getString("currentChannel",DEFAULTCHANNEL);
                scrollToGuideChannel(curchannel);
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
                    return true;
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
                MyDebug("KEYCODE EVENT " + (char) event.getUnicodeChar());
                String chNumText = chNumTextView.getText() + Character.toString((char) event.getUnicodeChar());
                chNumTextView.setText(chNumText);
                return true;
            }
            if ( event.getKeyCode() == KeyEvent.KEYCODE_ENTER ||
                    event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER )  {
                String chNumText = (String) chNumTextView.getText();
                if (  !chNumText.equals("") ) {
                    if (spectrumGuide.getVisibility() == View.VISIBLE) {
                        scrollToGuideChannel(chNumText);
                    } else if (spectrumPlayer.getVisibility() == View.VISIBLE) {
                        MyDebug("chnumber = " + chNumText + " ENTER KEY");
                        String mappedTsmid = guideManager.getTsmid(chNumText);
                        if (!mappedTsmid.trim().isEmpty()) {
                            if (miniGuideIsShowing) {
                                scrollToMiniGuideChannel(chNumText, mappedTsmid);
                            } else {
                                if (specPlayerReady && !restartingFromDreaming) {
                                    miniGuidePlayChannel(chNumText, mappedTsmid);
                                } else {
                                    MyDebug("adding chnumber = " + chNumText + " to specPlayerQueue");
                                    specPlayerQueue = chNumText;
                                }
                            }
                        } else {
                            Toast.makeText(getBaseContext(), "Channel " + chNumText + " not found",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    chNumText = "";
                    chNumTextView.setText(chNumText);
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
                    event.getKeyCode() == KeyEvent.KEYCODE_DEL ) &&
                    spectrumPlayer.getVisibility() == View.VISIBLE ) {
                String newchannel = sharedPref.getString("prevChannel",DEFAULTCHANNEL);
                EpgMapData epg = guideManager.getGuideEntry(newchannel);
                miniGuidePlayChannel(epg.chnum, epg.tsmid);
                return true;
            }

            MyDebug("Not handled key event down " + event.getKeyCode());
        }

        return super.dispatchKeyEvent(event);
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
            // could add function to find a nearby channel
            chnum = guideManager.getAvailableChnum(chnum);
            MyDebug("Starting  scrollToGuideChannel channel not found " + chnum);
        }
        String scrollToGuideChannelJS2 =
            "var chnum = '" + chnum + "' ;" +
            """
                        var element;
                                        
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
                                    "$('" + cssid + "').focus();" +
                                    "$('" + cssid + "').click();" +
                                    "$('mini-guide').last().removeClass('mini-guide-open');" +
                                    "$('video').eq(0).focus(); " +
                                    "};" +
                                    "$('ul#channel-browser.channel-list.ng-scope').animate({ scrollTop: " +
                                    offset + "} , '100', function () { " +
                                    "   setTimeout(waitForDOM, 20);" +
                                    "   }" +
                                    ");";

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
                    saveLastChannel(channelId,chNum);
                    spectrumPlayer.loadUrl(baseLiveChannelURL + channelId);
                    spectrumGuide.setVisibility(View.GONE);
                    if (goback) {
                        spectrumGuide.evaluateJavascript("history.back();", null);
                    }
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
                spectrumPlayer.evaluateJavascript(playerInitJS, null);

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
        spectrumGuide.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

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
                    String scanMiniGuideJS2 =
                            """
                            var cnt = -1;
                            var rowpx = 0;
                            var licnt = 0;
                            var SCANDONE = false;
                            var lastoffset = 0;
                            var prevLastOffset = -1;
                            $('ul#channel-browser.channel-list.ng-scope').on('scrollend', function() {
                               // start processing the channels
                               if (!SCANDONE && cnt >= 0) {
                                  rowpx = $('#channel-browser li').eq(1).height();
                                  licnt = $('#channel-browser li').length;
                                  var centeroffset = (rowpx * (licnt / 2)) - rowpx;
                                  $('#channel-browser li').each(function(index, e) {
                                     let css = $(e).attr('id');
                                     if (css !== undefined) {
                                        let chname = $(e).find('div.callsign.ng-binding').text();
                                        let chnum = $(e).find('div.channel-number.ng-binding.ng-scope').text();
                                        let offsettop = e.offsetTop - centeroffset;
                                        lastoffset = e.offsetTop;
                                        if (offsettop < 0) {
                                           offsettop = e.offsetTop;
                                        };
                                        Spectv.addMiniGuideEntry(chnum, chname, css, offsettop.toString());
                                        if (lastoffset === prevLastOffset) {
                                           SCANDONE = true;
                                        }
                                     };
                                  });
                                  prevLastOffset = lastoffset;
                                  // move past header , footers and last entry
                                  var nextpos = lastoffset + rowpx + rowpx + rowpx;
                                  Spectv.scrollMiniGuide(nextpos.toString());
                               }
                               if (SCANDONE) {
                                  Spectv.setMiniGuideLoaded();
                                  Spectv.toggleMiniGuideWindow("CLOSE");
                                  $('ul#channel-browser.channel-list.ng-scope').off('scrollend');
                                 
                               }
                            });
                            if (cnt == -1) {
                               // goto the top of the miniguide , start the scan
                               // broken if miniguide already at zero
                               Spectv.MyDebug('Start scanMiniGuideJS2 goto offseet 0');
                               Spectv.scrollMiniGuide('10');
                               cnt++;
                            };
                            """;
                    // open the miniguide
                    MyDebug("  ScanMiniGuide - about to Show miniguide ");
                    toggleMiniGuideWindow("OPEN");
                    // run javascript to scan miniguide
                    MyDebug("  ScanMiniGuide - about to Scan  miniguide ");
                    spectrumPlayer.evaluateJavascript(scanMiniGuideJS2, null);
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
        guideManager.addMiniGuideEntry(chnum, chname, css, offsetpx);
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
    @JavascriptInterface
    // used is scan miniguide
    public void scrollMiniGuide(String px) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String jumpToPX ="$('ul#channel-browser.channel-list.ng-scope').scrollTop(" + px +" );";
                    spectrumPlayer.evaluateJavascript(jumpToPX, null);
                }
            });

        } catch (Exception e) {
            MyDebug("scrollMiniGuide Exception " + e.toString());
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

