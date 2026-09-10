package com.workaround.spectv;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Gravity;
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
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Arrays;

/*
 * Main Activity class that loads {@link MainFragment}.
 */
public class MainActivity extends FragmentActivity {


    // updated useragent 08/06/2026
   // final String uaString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:154.0) Gecko/20100101 Firefox/154.0";
    final String uaString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36";

    final String guideUrl = "https://watch.spectrum.net/guide";
    final String baseLiveChannelURL = "https://watch.spectrum.net/livetv?tmsid=";
    final String newSessionURL = "https://watch.spectrum.net/?sessionOverride=true";
    String DEFAULTCHANNEL = "0";
    static final Boolean DEBUGON = true;
    Boolean specPlayerReady = false;

    String specPlayerQueue = "";
     WebView spectrumPlayer;
    WebView spectrumGuide;
    String specGuideFilter = "FAV" ;  // ALL or FAV
    TextView chNumTextView;
    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEdit;
    String lastChannelURL;
    String cookies;
    boolean guideLoaded = false;
    boolean scanningMiniGuide = false;
    boolean loginRequired = false;
    public static boolean restartingFromDreaming = false;
    static long onStopEpochSeconds = 0;
    boolean miniGuideIsShowing = false;
    GuideManager guideManager = null;

    ListView listView;
    TextView listViewHeader;
    LinearLayout miniGuideLayout;
    ImageView  menuImage;
    Adapter miniGuideAdapter = null;
    int prevFirstVisiblePosition = 1000;  // force initial load of onnow data
  //  String currentMiniGuideFilter = " 2 3 5 25 28 29 30 55 97 136 1255 ";
    String favoritesFilter = "";
    String recentFilter = "";
    int selectedMiniGuideFilter = 0;  // 0-all, 1-favorites, 2-recent channels


    String playerInitJS = """
            var loopVar = setInterval(
                 function () {
                 Spectv.MyDebug('page loading ' +
                     window.location.pathname + ' starting playerInitJS');
                 // check if login required, away from home
                 needLogin = Spectv.getLoginRequired();
                 if (needLogin) {
                     return;
                 }
                 try {
                     // Handle away from home prompt
                     // document.querySelector("#kite-modal-container > div > div.kite-modal-dialog.kite-card > div.kite-modal-body > kite-alert > div > button")?.click();
                     // Accept initial prompts
                     if (document.querySelector('.continue-button')?.childNodes?.length > 0) {
                         document.querySelector('.continue-button')?.childNodes[0].click();
                     }
                     document.querySelector('[aria-label*="Continue and accept"]')?.click();
                     document.querySelector('.btn-success')?.click();
                     // away from home spectrum network flash screen
                     // document.querySelector("#kite-modal-container > div > div.kite-modal-dialog.kite-card > div.kite-modal-body > ooh-warning-modal > button")?.click()
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
                         // check for mute
                         var specmute = $('#volume-control-icon').attr('aria-pressed');
                         if (specmute === 'true') {
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
            
                         // handle away from home splash screen
                         var awayobserver = new MutationObserver(function (mutations) {
                             for (mutation of mutations) {
                                 for (addedNode of mutation.addedNodes) {
                                     var button = document.evaluate(
                                             "//button[contains(text(), 'Ok')]",
                                             addedNode, null,
                                             XPathResult.FIRST_ORDERED_NODE_TYPE,
                                             null).singleNodeValue;
                                     if (button) {
                                         Spectv.MyDebug('clicking away-from-home OK button');
                                         //  console.log('clicking Ok button');
                                         button.click();
                                         return;
                                     }
                                 }
                             };
                         });
                         awayobserver.observe(document.body, {
                             subtree: true,
                             childList: true
                         });
            
                     } // end if video > 0
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
            
             //  function toggleGuide(s) {
             //      Spectv.toggleChannelGuide(s)
             //  };
             //  function toggleMiniGuide(s) {
             //      Spectv.channelGuide(s)
             //  };
            
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
                     Spectv.MyDebug('looping guideInitJS');
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
                     var testloaded = $('ul li.channel-header-row').length;
                     if ( testloaded > 0 ) {
                         // away from home spectrum network flash screen
                         document.querySelector("#kite-modal-container > div > div.kite-modal-dialog.kite-card > div.kite-modal-body > ooh-warning-modal > button")?.click()
                         clearInterval(loopVar);
                     }
                  } catch (error) {
                     console.log('ERROR in guide', error);
                     Spectv.MyDebug('ERROR in  guideInitJS' + error);      
                  }
               }, 500);
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

        favoritesFilter = sharedPref.getString("favoritesFilter","");
        recentFilter = sharedPref.getString("recentChannels", "");
        lastChannelURL = sharedPref.getString("lastChannel", "");
        selectedMiniGuideFilter = sharedPref.getInt("selectedMiniGuideFilter",0);

        if (data[1] != null && !data[1].isEmpty()) {
            lastChannelURL = baseLiveChannelURL + channelId;
            curchnum = channelNum;
        } else {
            curchnum = sharedPref.getString("currentChannel", DEFAULTCHANNEL);
        }
        MyDebug("Starting Spectv channel " + curchnum +
                "\n Found " + guideManager.numberOfChannels() + " channels");
        Toast.makeText(getBaseContext(), "Starting Spectv channel " + curchnum +
                        "\n Found " + guideManager.numberOfChannels() + " channels",
                Toast.LENGTH_LONG).show();

        miniGuideLayout = findViewById(R.id.miniGuideLayout);
        miniGuideLayout.setVisibility(View.INVISIBLE);
        chNumTextView = findViewById(R.id.chNumTextView);
        menuImage = findViewById(R.id.menuImage);
        // set onclick listener
        menuImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getBaseContext(), "clicked menu image,scan miniguide ",
                        Toast.LENGTH_LONG).show();
                toggleMiniGuideWindow("CLOSE");
                guideManager.guideCacheIsReady = false;
                miniGuideAdapter = null;   // force new miniguide array adaptor
                sortMiniGuide();
            }
        });

        // Getting the reference of listView and setup listeners
        listView = (ListView) findViewById(R.id.listView);
      //  listView.setVisibility(View.INVISIBLE);
        if (guideManager.guideCacheIsReady()) {
           initMiniGuideAdaptor();
        }
        listViewHeader = (TextView) findViewById(R.id.listViewHeader);

        initGuide();
        spectrumGuide.addJavascriptInterface(this, "Spectv");
        initPlayer();
        spectrumPlayer.addJavascriptInterface(this, "Spectv");
    //    initGuide();
    //    spectrumGuide.addJavascriptInterface(this, "Spectv");

    }


    @Override
    public void onResume() {
        super.onResume();
        MyDebug("onResume  ");
    }

    // Pause/Resume webview on app going to background
    @Override
    public void onPause() {
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
        var videoStartjs = """
                $('video')[0].play();
                """;
        //     startRepeatingTask();
        spectrumPlayer.evaluateJavascript(videoStartjs,null);

        MyDebug("onRestart intents = " + getIntent());
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
        setRestartFromDreaming();
        if (!info[0].isEmpty() && !curchnum.equals(info[0])) {
            MyDebug("onNewIntent playing new ch = " + info[0]);
            restartingFromDreaming = false;
            miniGuidePlayChannel(info[0], info[1]);
            return;
        }
        if (restartingFromDreaming) {
            if (info[0].isEmpty()) {
                info[0] = curchnum;
                info[1] = guideManager.getTsmid(curchnum);
            }
            restartingFromDreaming = false;
            specPlayerReady = false;
            MyDebug("onNewIntent navToChannel  ch = " + info[0]);
            navToChannel(info[1], info[0], false);
        } else {
            MyDebug("onNewIntent  ch = " + info[0] + "already playing");
            // set video to play
            miniGuidePlayChannel(info[0], info[1]);
        }
    }

    private void setRestartFromDreaming() {
        long currentsec = System.currentTimeMillis() / 1000L;
        long reloadmax = 5 * 60;
        MyDebug("setRestartFromDreaming  onStopEpochSeconds = " + onStopEpochSeconds);
        MyDebug("setRestartFromDreaming  sleep time = " + (currentsec - onStopEpochSeconds));
        if ((currentsec - onStopEpochSeconds) > reloadmax && onStopEpochSeconds != 0) {
            restartingFromDreaming = true;
        }
    }

    private String[] parseIntentFilter(Intent intent) {
        String[] info = new String[2];
        String channelId = "";
        String channelNum = "";
        Uri intentData = intent.getData();
        if (intentData != null) {
            Uri uri = Uri.parse(intentData.toString());
            channelId = uri.getQueryParameter("channelId");
            channelNum = uri.getQueryParameter("channelNum");
        } else {
            channelId = intent.getStringExtra("channelId");
            channelNum = intent.getStringExtra("channelNum");
        }
        // setup return values
        if (channelNum != null && !channelNum.isEmpty()) {
            channelId = guideManager.getTsmid(channelNum);
        } else if (channelId != null && !channelId.isEmpty()) {
            channelNum = guideManager.getChnum(channelId);
        } else {
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
        if (event.getAction() == KeyEvent.ACTION_UP  && miniGuideIsShowing) {
            if ( event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN ) {
                // assume end of scroll event , update onnow data in miniguide

                int rowcount = 0, curpos = 0;
                String offset = "", chnum = "";
                curpos = listView.getSelectedItemPosition();
                chnum =  miniGuideAdapter.getMiniGuideList().get(curpos).chnum;
                MyDebug("up/down start chnum = " + chnum + " position = " + curpos);
                listView.post((new Runnable() {
                    @Override
                    public void run() {
                        // Update the ListView here
                        MyDebug("Starting  up/down listview post");
                        String rowdata = getLazyLoadRowCount();

                        if ( !rowdata.isEmpty()) {
                            lazyLoadOnNow(rowdata);
                        }
                    }
                }));
               return super.dispatchKeyEvent(event);
            }
        }

        // Handle key events to consistently bring up the mini channel guide
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (loginRequired) {
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

           //     spectrumPlayer.evaluateJavascript("toggleGuide('SHOWGUIDE');", null);
                toggleChannelGuide("SHOWGUIDE");
                getGuideFavorites();
//              scroll Guide to current channel playing
                String curchannel = sharedPref.getString("currentChannel", DEFAULTCHANNEL);
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
                  //  spectrumPlayer.evaluateJavascript("toggleGuide('HIDEGUIDE');", null);
                    toggleChannelGuide("HIDEGUIDE");
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
                    String curchannel = sharedPref.getString("currentChannel", DEFAULTCHANNEL);
                    String tsmid = guideManager.getTsmid(curchannel);
                   // scrollToMiniGuideChannel(curchannel, tsmid);
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
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER ||
                    event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
                String chNumText = (String) chNumTextView.getText();
                if (!chNumText.equals("")) {
                    chNumTextView.setText("");
                    handleChNumEvent(chNumText);
                    return true;
                } else if (miniGuideIsShowing) {
                    // user hits enter on channel to play, miniguide listener will handle
                    return super.dispatchKeyEvent(event);
                }
            }
            if ((event.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_UP ||
                    event.getKeyCode() == KeyEvent.KEYCODE_PLUS ||
                    event.getKeyCode() == KeyEvent.KEYCODE_PAGE_UP) &&
                    spectrumPlayer.getVisibility() == View.VISIBLE) {
                String curchannel = sharedPref.getString("currentChannel", DEFAULTCHANNEL);
                EpgMapData epg = guideManager.getChannelUp(curchannel);
                String chnum = epg.chnum;
                miniGuidePlayChannel(chnum, epg.tsmid);
                return true;
            }

            if ((event.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_DOWN ||
                    event.getKeyCode() == KeyEvent.KEYCODE_MINUS ||
                    event.getKeyCode() == KeyEvent.KEYCODE_PAGE_DOWN) &&
                    spectrumPlayer.getVisibility() == View.VISIBLE) {
                String curchannel = sharedPref.getString("currentChannel", DEFAULTCHANNEL);
                EpgMapData epg = guideManager.getChannelDown(curchannel);
                String chnum = epg.chnum;
                miniGuidePlayChannel(chnum, epg.tsmid);
                return true;
            }

            if ((event.getKeyCode() == KeyEvent.KEYCODE_LAST_CHANNEL ||
                    event.getKeyCode() == KeyEvent.KEYCODE_DEL ||
                    event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) &&
                    spectrumPlayer.getVisibility() == View.VISIBLE &&
                    !miniGuideIsShowing && spectrumGuide.getVisibility() != View.VISIBLE) {
                String newchannel = sharedPref.getString("prevChannel", DEFAULTCHANNEL);
                EpgMapData epg = guideManager.getGuideEntry(newchannel);
                miniGuidePlayChannel(epg.chnum, epg.tsmid);
                return true;
            }

            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT &&  miniGuideIsShowing) {
              // toggle miniguide filter, 0, 1 or 2
                if ( selectedMiniGuideFilter + 1 > 2) {
                    selectedMiniGuideFilter = 0;
                } else {
                    selectedMiniGuideFilter++;
                }
                sharedPrefEdit.putInt("selectedMiniGuideFilter",selectedMiniGuideFilter);
                sharedPrefEdit.apply();
                miniGuideAdapter.getFilter().filter(getMiniGuideFilter());
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
                String chnum = (String) chNumTextView.getText();
                if (!chnum.equals("")) {
                    chNumTextView.setText("");
                    handleChNumEvent(chnum);
                }
            }
        }.start();
    }


    private void scrollToMiniGuideChannel(String chnum, String tsmid) {
        MyDebug("Starting  scrollToMiniGuideChannel " + chnum);

        // use miniguide adapter to find scroll location
        int pos = miniGuideAdapter.getBestChannelPosition(chnum);
        MyDebug("Starting  scrollToMiniGuideChannel chnum =" + chnum + " position = " + pos);
        if (pos >= 0) {
            listView.setSelection(pos);
            listView.requestFocus();
            listView.post((new Runnable() {
                @Override
                public void run() {
                    // Update the ListView here
                    MyDebug("Starting  scrollToMiniGuideChannel post");
                    // reload of data onnow
                    prevFirstVisiblePosition = 1000;
                    String rowdata = getLazyLoadRowCount();
                    if (!rowdata.isEmpty()) {
                        lazyLoadOnNow(rowdata);
                    }
                }
            }));
        } else {
            MyDebug("scrollToMiniGuideChannel chnum =" + chnum + " not found");
        }
    }

    private void scrollToGuideChannel(String chnumber) {
        String chnum = chnumber;
        MyDebug("Starting  scrollToGuideChannel " + chnum);
        EpgMapData mapdata = guideManager.getGuideEntry(chnum);
        if (mapdata == null) {
            //  add function to find a nearby channel
            chnum = guideManager.getAvailableChnum(chnum);
            MyDebug("Starting  scrollToGuideChannel nearby channel  " + chnum);
        }
        String scrollToGuideChannelJS2 =
                "var chnum = '" + chnum + "' ;" +
                        """
                                    var element;
                                
                                    // Accept initial prompts, out of home network
                                
                                // document.querySelector("#kite-modal-container > div > div.kite-modal-dialog.kite-card > div.kite-modal-body > kite-alert > div > button")?.click();
                                // set guide filter to all channels
                                // $('button.filter.ng-binding.ng-scope').eq(0).click();
                                    function setfocus() {
                                    // get timeline offset to position right px
                                       var nowMil = new Date().getTime();
                                       var roundedMil = Math.floor(nowMil/1000/60/30) * 30 * 60 * 1000;
                                       var timeline = $('[time="' + roundedMil + '"]')[0].getBoundingClientRect();
                                    // get the row offset px from top viewport
                                       var domrect = element[0].getBoundingClientRect();
                                       document.elementFromPoint(timeline.left, domrect.top).focus();
                                       // close away from home window
                                       $('#modal-close').click();
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

        MyDebug("scrollToGuideChannel chnum = " + chnum);
        spectrumGuide.evaluateJavascript(scrollToGuideChannelJS2, null);
    }

    public void initMiniGuideAdaptor() {
        miniGuideAdapter = new Adapter(this, R.layout.list_item, guideManager.getMiniGuideList());
        listView.setAdapter(miniGuideAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EpgMapData epgdata = miniGuideAdapter.getMiniGuideList().get(position);
                MyDebug("setOnItemClickListener() play channel = " + epgdata.chnum );
                miniGuidePlayChannel(epgdata.chnum, epgdata.tsmid);
                toggleMiniGuideWindow("CLOSE");
            }
        });
        miniGuideAdapter.setOnDataSetChangedListener(new Adapter.OnDataSetChangedListener() {
            @Override
            public void onDataSetChanged() {
                // Handle the data set change here
                MyDebug("setOnDataSetChangedListener() viewlist size = " + miniGuideAdapter.getMiniGuideList().size());
                String chnum = sharedPref.getString("currentChannel", DEFAULTCHANNEL);
                scrollToMiniGuideChannel(chnum, "");
            }
        });
    }

    public String getMiniGuideFilter() {
        String filter = "";
        if ( selectedMiniGuideFilter == 1 ) {
            listViewHeader.setText("Filter: Favorites");
            if (favoritesFilter.isEmpty()) {
                filter = "-1";  // show create empty list
            } else {
                filter = favoritesFilter;
            }
        } else if (selectedMiniGuideFilter == 2) {
            listViewHeader.setText("Filter: Recent Channels");
            if (recentFilter.isEmpty()) {
                filter = "-1";  // show create empty list
            } else {
                filter = recentFilter;
            }
        } else {
            listViewHeader.setText("Filter: All Channels");
            filter = "";
        }
        return filter;
    }
    public void toggleMiniGuideWindow(String state) {
        MyDebug("In toggleMiniGuideWindow state = " + state);
        if (state.equals("CLOSE")) {
        //    ArrayList<EpgMapData> test = miniGuideAdapter.getMiniGuideList();
        //    MyDebug("toggleMiniGuideWindow CLOSE filterList size = " + test.size() );
        //    MyDebug("toggleMiniGuideWindow CLOSE guidemanager size = " + guideManager.getMiniGuideList().size() );
            miniGuideLayout.setVisibility(View.INVISIBLE);
            miniGuideIsShowing = false;
            return;
        }
        if (state.equals("OPEN")) {
            miniGuideLayout.setVisibility(View.VISIBLE);
            listView.requestFocus();
            miniGuideIsShowing = true;
            // initial install , need to reload data
            if (miniGuideAdapter == null ) {
                initMiniGuideAdaptor();
            }
            miniGuideAdapter.getFilter().filter(getMiniGuideFilter());
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
    public boolean getLoginRequired() {
        return loginRequired;
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
                        miniGuidePlayChannel(queue, mappedid);
                    }
                }
            });
        } catch (Exception e) {
            Log.e("SPECERROR", "reloadStartupChannel() " +  e.toString());
        }
    }


    @JavascriptInterface
    public void toggleChannelGuide(String action) {
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
                    Log.e("SPECERROR","ERROR in showing guide " + e.toString());
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
                    Log.e("SPACERROR","ERROR in hiding guide" +  e.toString());
                }
                break;
        }
    }


    public String getLazyLoadRowCount() {
        int rowcount = 0, diff = 0;
        int firstpos = listView.getFirstVisiblePosition();
        int lastpos = listView.getLastVisiblePosition();
        String spectOffset = "";
        MyDebug("getLazyLoadRowCount start firstpos = " + firstpos + " lastpos = " + lastpos + " prevoffset = " + prevFirstVisiblePosition);
        if ( prevFirstVisiblePosition == firstpos) {
            return "";
        }
        // create json array of channels to update
        prevFirstVisiblePosition = firstpos;
        ArrayList<EpgMapData> listViewData = miniGuideAdapter.getMiniGuideList();
        JSONArray jsonArray = new JSONArray();
        try {
            for (int i = firstpos; i <= lastpos; i++) {
                EpgMapData epg = listViewData.get(i);
                JSONObject obj = new JSONObject();
                obj.put("chnum", epg.chnum);
                obj.put("offset", epg.offset);
                jsonArray.put(obj);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        MyDebug("getLazyLoadRowCount json = " + jsonArray.toString());
        MyDebug("getLazyLoadRowCount firstpos = " + firstpos + " lastpos = " + lastpos + " rows = " + rowcount);
        return jsonArray.toString();
    }

    public void lazyLoadOnNow(String channels) {
        MyDebug("in lazyLoadOnNow channels = " + channels);
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String onnow2 = """
                            var requiredrows = 0;
                            var rowcnt = 0;
                            var buffer = {};
                            var startoffset = 0;
                            var channelListJson = "";
                            
                            function endOfData() {
                                var size = $('#channel-browser li').size();
                                var words = $('#channel-browser li').eq(size - 1).attr('style');
                                // width: 100%; min-height: 0px; - end of scroll
                                var n = words.split(" ");
                                var hpx = n[n.length - 1];
                                return hpx === "0px;";
                            }
                            
                            function readListItems() {
                                // loop over list items get chnum, onnow, episode
                                // get offset to next set of rows and scroll, unless we are at the end
                                var READDONE = false;
                                var size = $('ul#channel-browser li').size();
                            
                                $('#channel-browser li').each(function (index, e) {
                                    //    if (e.offsetTop < startoffset) {
                                    //        return; // iterate loop
                                    //    }
                                    if (READDONE) {
                                        return true;
                                    };
                                    let css = $(e).attr('id');
                                    if (css !== undefined) {
                                        var chnum = $(e).find('div.channel-number.ng-binding.ng-scope').text();
                                        // Check if the value is included in the array
                                        let isIncluded = channelListJson.some(item => item.chnum === chnum);
                            
                                        if (isIncluded && !(chnum in buffer)) {
                            
                                            let onnow = $(e).find('div.truncate.title.ng-binding').text();
                                            let episode = $(e).find('div.truncate.episode.ng-binding.ng-scope').text();
                                            //   Spectv.MyDebug("getOnNowData adding chnum = " + chnum);
                                            buffer[chnum] = {
                                                onnow: onnow,
                                                episode: episode
                                            };
                                            rowcnt++;
                                        }
                            
                                    }
                                    // if we have all the required row , callback to update onnow
                                    // if we need more rows scroll to next block
                                    // index == size - 1 means we have read all the rows in this block
                                    if ((index == size - 1) || (rowcnt == requiredrows)) {
                                        //  Spectv.MyDebug("getOnNowData rowcnt = " + rowcnt + " reqrows = " + requiredrows);
                                        if (rowcnt == requiredrows || endOfData()) {
                                            if ('onscrollend' in window) {
                                                $('ul#channel-browser').get(0).onscrollend = "";
                                            } else {
                                                $('ul#channel-browser').get(0).onscroll = "";
                                            }
                                            json = JSON.stringify(buffer);
                                            console.log("row cnt = " + rowcnt + " json = " + json);
                                            Spectv.setOnNowView("", json);
                                            READDONE = true;
                                        } else {
                                            // need more row, scroll to next block
                                            //  var endpx = $('#channel-browser li').get(size - 2).offsetTop;
                                            // var rowpx = $('#channel-browser li').eq(1).height();
                                            //   console.log("getOnNowData - scrolling - get more rows");
                                            // var nextoffset = endpx + rowpx + rowpx + rowpx // last row+footer+header
                                            var nextoffset = channelListJson[rowcnt].offset;
                                            $('ul#channel-browser.channel-list.ng-scope').scrollTop(nextoffset);
                                            READDONE = true; // break out of loop
                                        }
                            
                                    };
                            
                                });
                            }
                            
                            function getOnNowData(requestedrowsJson) {
                                ////   Start The SCAN
                                //  Spectv.MyDebug("start getOnNowData requset = " + requestedrowsJson);
                                channelListJson = JSON.parse(requestedrowsJson);
                            
                                startoffset = channelListJson[0].offset;
                                buffer = {};
                                rowcnt = 0;
                                requiredrows = channelListJson.length;
                                //  Spectv.MyDebug("start getOnNowData requset rquired row count = " + requiredrows);
                                var toppx = $('#channel-browser li').get(1).offsetTop;
                                var size = $('#channel-browser li').size();
                                var endpx = $('#channel-browser li').get(size - 2).offsetTop;
                                var rowpx = $('#channel-browser li').eq(1).height();
                                // check if startoffset is in range of miniguide
                                if (startoffset > endpx && endOfData()) {
                                    //  Spectv.MyDebug("getOnNowData - offset out of range = " + startoffset);
                                    return;
                                }
                                // check if we need to scroll the miniguide or just read current offset before scroll
                                //    Spectv.MyDebug("getOnNowData - top = " + toppx + " end = " + endpx + "offset = " + offset);
                                // setup onscrollend listener, readListItems will cancel it
                                if ('onscrollend' in window) {
                                    $('ul#channel-browser').get(0).onscrollend = (event) => {
                                        readListItems();
                                    };
                                } else {
                                    let scrollTimer = null;
                                    $('ul#channel-browser').get(0).onscroll = (event) => {
                                        // console.log(" scroll event ");
                                        // Clear any existing timer
                                        if (scrollTimer) {
                                            clearTimeout(scrollTimer);
                                        }
                                        // Start a new timer
                                        scrollTimer = setTimeout(function () {
                                            //    console.log("Scrolling has stopped for 50ms");
                                            //	console.log("calling readChannels()");
                                            readListItems();
                                        }, 50);
                                    };
                                }
                                if (startoffset >= toppx && startoffset <= endpx) {
                                    //  Spectv.MyDebug("getOnNowData - no initial scroll");
                                    readListItems();
                            
                                } else {
                                    // need to scroll to offset
                                    //       Spectv.MyDebug("getOnNowData - scrolling - to get first rows");
                                    console.log("getOnNowData - scrolling - get first rows");
                                    $('ul#channel-browser.channel-list.ng-scope').scrollTop(startoffset);
                                }
                            
                            }
                            
                            """;

                    onnow2 = onnow2 + " getOnNowData( '" + channels +"' );";
                    spectrumPlayer.evaluateJavascript(onnow2, null);
                }
            });
        } catch (Exception e) {
            MyDebug("ERROR in lazyLoadOnNow" + e.toString());
        }

    }

    @JavascriptInterface
    public void setOnNowView(String position, String onnow) {
        MyDebug("in setOnNowView onnow = " + onnow);
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MyDebug("Start -- setOnNowView pos = " + position + " onnow = " + onnow);
                    guideManager.updateMiniGuideList(onnow);
                    // refresh the miniguide view
                    listView.invalidateViews();
                }
            });
        } catch (Exception e) {
            MyDebug("ERROR in miniGuidePlayChannel" + e.toString());
        }
    }
   // @JavascriptInterface
    public void getGuideFavorites() {
        MyDebug("in getGuideFavorites");
        if (spectrumGuide.getVisibility() != View.VISIBLE) {
            MyDebug("getGuideFavorites() Guide not VISIBLE");
            return;
        }
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String favoritiesJS =
                            """
                                    function getfavorites() {
                                    var retrycnt = 0;
                                    var loopVar = setInterval(
                                            function () {
                                            try {
                                                //
                                                if (retrycnt > 10) {
                                                    // no favorites found
                                                    clearInterval(loopVar);
                                                    Spectv.setGuideFavorites("");
                                                //    return;
                                                }
                                                var rows = $('ul li.channel-header-row').length;
                                                if (rows > 0) {
                                                    var buffer = "";
                                                    $('.favorite.favon').each(function (index, e) {
                                                        var chnum = $(e).parent().find('.channel-number').text();
                                                        buffer = buffer + " " + chnum;
                                    
                                                    });
                                                    clearInterval(loopVar);
                                                //    console.log("buffer = " + buffer);
                                                     Spectv.MyDebug('favoritiesJS buffer = ' + buffer);
                                                     Spectv.setGuideFavorites(buffer);
                                               //      $('.filter.ng-binding.ng-scope').eq(0).click();
                                                //     return;
                                                } else {
                                                    retrycnt++;
                                                 //   console.log("not ready");
                                                    Spectv.MyDebug('favoritiesJS retrycnt = ' + retrycnt);
                                                }
                                            } catch (error) {
                                               // console.log('ERROR in guide', error);
                                               Spectv.MyDebug('ERROR in  favoritiesJS ' + error.message);
                                               clearInterval(loopVar);
                                               Spectv.setGuideFavorites("");
                                    
                                            }
                                        }, 100);
                                        }
                                        getfavorites();
                                    """;
                    spectrumGuide.evaluateJavascript(favoritiesJS, null);

                }
            });
        } catch (Exception e) {
            MyDebug("ERROR in getGuideFavorites()" + e.toString());
        }
    }

    @JavascriptInterface
    public void setGuideFavorites(String favorites) {
        MyDebug("setGuideFavorites fav = " + favorites);
        favoritesFilter = " " + favorites + " ";
        // save to pref
        sharedPrefEdit.putString("favoritesFilter", favoritesFilter);
        sharedPrefEdit.apply();
        // spectrumPlayer.evaluateJavascript("toggleGuide('HIDEGUIDE');", null);
        if ( scanningMiniGuide) {
            scanningMiniGuide = false;
            toggleChannelGuide("HIDEGUIDE");
        }
    }

    public void updateRecentChannels(String chnum) {
        int MAXFILTERSIZE = 15;
        if ( recentFilter.isEmpty() ){
            recentFilter = " " + chnum + " ";
            return;
        }
            // need to manage adding chnum to filter, reset the filter based on size and last accessed
            // split the filter
        String[] trimAndSplit = recentFilter.trim().split(" ");
        ArrayList<String> wordsList = new ArrayList<>(Arrays.asList(trimAndSplit));
        MyDebug("updateRecentChannels() queue size = " + wordsList.size());
        if ( recentFilter.contains(" " + chnum + " ")) {
            for (int i = 0; i < wordsList.size(); i++) {
                if (wordsList.get(i).equals(chnum)) {
                    MyDebug("updateRecentChannels() found chnum = " + chnum);
                    // add the channel
                    wordsList.remove(i);
                    wordsList.add(chnum);
                    break;
                }
            }
        } else {
            // just add the new channel
            wordsList.add(chnum);
            if ( wordsList.size() > MAXFILTERSIZE) {
                wordsList.remove(0);
                MyDebug("updateRecentChannels() removed index 0");
            }
        }
        // rebuild the recentFilter string
        recentFilter = " ";
        for (int i = 0; i < wordsList.size(); i++) {
            recentFilter = recentFilter + wordsList.get(i) + " ";
        }
    }

    @JavascriptInterface
    public void miniGuidePlayChannel(String chnum, String tsmid) {
        // build javascript to select channel
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EpgMapData mapdata = guideManager.getGuideEntry(chnum);
                    String offset = mapdata.offset;
                    String cssid = mapdata.cssid;
                    MyDebug("Starting  miniGuidePlayChannel chnum = " + chnum + " offset = " +
                            offset + "cssid " + cssid);

                    // update recent channels list, saveLastChannel() will update saved pref
                    updateRecentChannels(chnum);
                    // save the channel tsmid
                    saveLastChannel(tsmid, chnum);


                    MyDebug("Starting  miniGuidePlayChannel millisec = " + System.currentTimeMillis());

                    String playChannelJS2 = """
                            function playTheChannel(cssid) {
                                if ('onscrollend' in window) {
                                    $('ul#channel-browser').get(0).onscrollend = "";
                                } else {
                                    $('ul#channel-browser').get(0).onscroll = "";
                                }
                                $(cssid).focus();
                                $(cssid).click();
                                $('mini-guide').last().removeClass('mini-guide-open');
                                $('video').eq(0).focus();
                                $('video').eq(0).play();
                            }
                            
                            function getChannelData(cssid, offset, tsmid, chnum) {
                                // Max volume, test fix for alert causing mute issue
                                // need to check aria-pressed=true , that denotes muted
                                var specmute = $('#volume-control-icon').attr('aria-pressed');
                                var videomute = $('video')[0].muted;
                                var videoVolume = $('video')[0].volume;
                                if (specmute === 'true' || videomute) {
                                    // force reload of URL
                                    console.log("video was in mute mode");
                                    Spectv.navToChannel(" + tsmid + ", " + chnum + ", true);
                                    return;
                                }
                                $('video')[0].volume = 1.0;
                                ////   Start The SCAN
                                var toppx = $('#channel-browser li').get(1).offsetTop;
                                var size = $('#channel-browser li').length;
                                var endpx = $('#channel-browser li').get(size - 2).offsetTop;
                                // check if we need to scroll the miniguide or just current offset
                                if (offset >= toppx && offset < endpx) {
                                    playTheChannel(cssid);
                                } else {
                                    // need to scroll to offset
                                    if ('onscrollend' in window) {
                                        $('ul#channel-browser').get(0).onscrollend = (event) => {
                                            // scroll done play the channel
                                            playTheChannel(cssid);
                                        };
                                    } else {
                                        let scrollTimer = null;
                                        $('ul#channel-browser').get(0).onscroll = (event) => {
                                            //        console.log(" scroll event ");
                                            // Clear any existing timer
                                            if (scrollTimer) {
                                                clearTimeout(scrollTimer);
                                            }
                                            // Start a new timer
                                            scrollTimer = setTimeout(function () {
                                                //    console.log("Scrolling has stopped for 50ms");
                                                //	console.log("calling readChannels()");
                                                playTheChannel(cssid);
                                            }, 50);
                                        }
                                    };
                                }
                            
                                $('ul#channel-browser.channel-list.ng-scope').scrollTop(offset);
                                }
                            """;
                    playChannelJS2 = playChannelJS2 + "getChannelData('" + cssid + "'," + offset + ", " + tsmid + ", " + chnum + ");";
                    spectrumPlayer.evaluateJavascript(playChannelJS2, null);
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
                    miniGuidePlayChannel(chNum, channelId);
                }
            });
        } catch (Exception e) {
            Log.e("SPECERROR","guidePlayChannel()" +  e.toString());
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
                    updateRecentChannels(chNum);
                    saveLastChannel(channelId, chNum);
                    spectrumPlayer.loadUrl(baseLiveChannelURL + channelId);
                    spectrumGuide.setVisibility(View.GONE);
                    if (goback) {
                        spectrumGuide.evaluateJavascript("history.back();", null);
                    }
                }
            });
        } catch (Exception e) {
            Log.e("SPECERROR","ERROR in navToChannel() " + e.toString());
        }
    }

    public void saveLastChannel(String channelId, String channelNum) {

        String lastCurrentChannel = sharedPref.getString("currentChannel", DEFAULTCHANNEL);
        sharedPrefEdit.putString("lastChannel", baseLiveChannelURL + channelId);
        if (!lastCurrentChannel.equals(channelNum)) {
            sharedPrefEdit.putString("currentChannel", channelNum);
            sharedPrefEdit.putString("prevChannel", lastCurrentChannel);
        }
        // save recent viewed channels, assume recent filter already updated
        sharedPrefEdit.putString("recentChannels", recentFilter);
        sharedPrefEdit.apply();
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
            Log.e("SPECERROR","ERROR in preloadGuide() " + e.toString());
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
                MyDebug("onPageFinishedloaded url = " + url);
               // if ( loginRequired ) {
               //     return;
               // }
                if (url.contains("/login/auto") ) {
                    loginRequired = false;
                    spectrumPlayer.evaluateJavascript(playerInitJS, null);
                } else if (url.contains("/livetv")) {
                    loginRequired = false;
                    spectrumPlayer.evaluateJavascript(playerInitJS, null);
                }
                else if (url.contains("/login") && !url.contains("/login/auto")) {
                    loginRequired = true;
                } else {
                    // UNKNOW URL
                    loginRequired = false;
                    spectrumPlayer.evaluateJavascript(playerInitJS, null);
                    MyDebug("onPageFinishedloaded unknown url = " + url);
                }

            }

 //           @Override
//            public void onPageStarted(WebView view, String url,Bitmap favicon) {
//                super.onPageStarted(view, url, favicon);
//                if (url.contains("/login") && !url.contains("/login/auto")) {
//                    loginRequired = true;
//                }
//                MyDebug("onPageStarted about to loadUrl " + url);
//            }

        });



        spectrumPlayer.setVerticalScrollBarEnabled(false);
        MyDebug("about to loadUrl " + lastChannelURL);
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
                    spectrumGuide.setVisibility(View.GONE);
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

    /// //////////////////////////////////////////////////////

    @JavascriptInterface
    public void sortMiniGuide() {
        if ( guideManager.guideCacheIsReady()) {
            return;
        }
        scanningMiniGuide = true;
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
            Log.e("SPECERROR", "SortMiniGuide " + e.toString());
        }
    }

    @JavascriptInterface
    public void scanMiniGuide() {
        if (guideManager.guideCacheIsReady()) {
            return;
        }
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MyDebug("Starting  scanMiniGuide");
                    // start scan miniguide

                    String scanMiniGuideJS4 = """
                            function endOfData() {
                                var size = $('#channel-browser li').size();
                                var words = $('#channel-browser li').eq(size - 1).attr('style');
                                // width: 100%; min-height: 0px; - end of scroll
                                var n = words.split(" ");
                                var hpx = n[n.length - 1];
                                return hpx === "0px;";
                            }
                            
                            var SCANDONE = false;
                            var buffer = {};
                            var awayChnum = 7000;
                            function readChannels() {
                                // start processing the channels
                                Spectv.MyDebug('starting readChannels()');
                                var rowpx = $('#channel-browser li').eq(1).height();
                                var size = $('#channel-browser li').length;
                                var offsettop = 0;
                            
                                $('#channel-browser li').each(function (index, e) {
                                    let css = $(e).attr('id');
                                    if (css !== undefined) {
                                        let chname = $(e).find('div.callsign.ng-binding').text();
                                        let chnum = $(e).find('div.channel-number.ng-binding.ng-scope').text();
                                        let onnow = $(e).find('div.truncate.title.ng-binding').text();
                                        let icon = $(e).find('.logo img').attr('src');
                                        //     let offsettop = e.offsetTop - centeroffset;
                                        if (!chnum) {
                                            // hack to handle away from home
                                            awayChnum++;
                                            chnum = awayChnum.toString();
                                        }
                                        if (icon !== undefined) {
                                            //     let offsettop = e.offsetTop - centeroffset;
                                            offsettop = e.offsetTop;
                                            buffer[chnum] = {
                                                chname: chname,
                                                css: css,
                                                offset: offsettop.toString(),
                                                imgurl: icon
                                            };
                                        } else {
                                            //  away from home, end of available channels hit
                                            SCANDONE = true;
                                        }
                                    }
                                    // index == size - 1 means we have read all the rows in this block
                                    if ((index == size - 1) && endOfData() || SCANDONE) {
                                        SCANDONE = true;
                                        // convert buffer to json and update guideManager, kill the scroll listener
                                        if ('onscrollend' in window) {
                                            $('ul#channel-browser').get(0).onscrollend = "";
                                        } else {
                                            $('ul#channel-browser').get(0).onscroll = "";
                                        }
                                        //  console.log("buffer size = " + Object.keys(buffer).length);
                                        var json = JSON.stringify(buffer);
                                        Spectv.setMiniGuideLoaded(json);
                                        //    console.log("miniguide data = " + json);
                                        // JAY close miniguide
                                        $('mini-guide').last().removeClass('mini-guide-open');
                                        $('video').eq(0).focus();
                                        return false; // Breaks the loop
                                    }
                            
                                });
                                if (!SCANDONE) {
                                    // move past header , footers and last entry
                                    // keep miniguide open, spectrum will time it out
                                    $('mini-guide').last().addClass('mini-guide-open');
                                    var nextpos = offsettop + rowpx + rowpx + rowpx;
                                    Spectv.MyDebug('start reading next block offset = ' + nextpos);
                                    // $('ul#channel-browser.channel-list.ng-scope').scrollTop(nextpos.toString());
                                    $('ul#channel-browser.channel-list.ng-scope').animate({ scrollTop: nextpos.toString() }, 50);
                                }
                            };
                            // JAY     open miniguide
                            $('mini-guide').last().addClass('mini-guide-open');
                            // check if we are at offset 0, if it is start reading, else scroll to 0
                            var toppx = $('#channel-browser li').get(1).offsetTop;
                                                       
                            // setup onscroll end listener
                            if ('onscrollend' in window) {
                                $('ul#channel-browser').get(0).onscrollend = (event) => {
                                    // scroll done play the channel
                                    readChannels();
                                };
                            } else {
                                let scrollTimer = null;
                                $('ul#channel-browser').get(0).onscroll = (event) => {
                                    //        console.log(" scroll event ");
                                    // Clear any existing timer
                                    if (scrollTimer) {
                                        clearTimeout(scrollTimer);
                                    }
                                    // Start a new timer
                                    scrollTimer = setTimeout(function () {
                                        //    console.log("Scrolling has stopped for 50ms");
                                        //	console.log("calling readChannels()");
                                        readChannels();
                                    }, 50);
                                }
                            };
                            
                            if (toppx == 0) {
                                readChannels(); // read the current channels
                            } else {
                               // $('ul#channel-browser.channel-list.ng-scope').scrollTop(0);
                               $('ul#channel-browser.channel-list.ng-scope').animate({ scrollTop: 0 }, 50);
                            }
                            
                            """;
                    // open the miniguide
                    MyDebug("  ScanMiniGuide - about to Show miniguide ");
                    //  toggleMiniGuideWindow("OPEN");
                    // run javascript to scan miniguide
                    MyDebug("  ScanMiniGuide - about to Scan  miniguide ");
                    spectrumPlayer.evaluateJavascript(scanMiniGuideJS4, null);
                    MyDebug("End  ScanMiniGuide()");
                }
            });
        } catch (Exception e) {
            Log.e("SPECERROR","ERROR in live channel nav " + e.toString());
        }
    }

    @JavascriptInterface
    // wrapper function to handle JS call to guideManager
    public void setMiniGuideLoaded(String json) {
        MyDebug("in setMiniGuideLoaded()");
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   //chNumTextView.setText("Saving MiniGuide");
                    guideManager.initMiniGuideData(json);
                //    spectrumPlayer.evaluateJavascript("toggleGuide('SHOWGUIDE');", null);
                    // get favorites from the guide
                 //   spectrumGuide.setVisibility(View.VISIBLE);
                    toggleChannelGuide("SHOWGUIDE");
                    getGuideFavorites();
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

    /// ////////////////////////////////////////////////////////


    @JavascriptInterface
    public void MyDebug(String s1) {
        if (DEBUGON) {
            Log.d("SPECDEBUG", s1);
        }
    }

}

