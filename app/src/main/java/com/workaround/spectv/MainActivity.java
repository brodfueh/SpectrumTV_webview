package com.workaround.spectv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.fragment.app.FragmentActivity;

/*
 * Main Activity class that loads {@link MainFragment}.
 */
public class MainActivity extends FragmentActivity {

    final String uaString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
    final String guideUrl = "https://watch.spectrum.net/guide";
    final String baseLiveChannelURL = "https://watch.spectrum.net/livetv?tmsid=";
    final String newSessionURL = "https://watch.spectrum.net/?sessionOverride=true";

    WebView spectrumPlayer;
    WebView spectrumGuide;

    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEdit;
    String lastChannelURL;

    String cookies;
    boolean guideLoaded = false;

    boolean miniGuideIsShowing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPref = this.getSharedPreferences("com.workaround.spectv.pref", Context.MODE_PRIVATE);
        sharedPrefEdit = sharedPref.edit();
        lastChannelURL = sharedPref.getString("lastChannel", "");

        initPlayer();
        initGuide();

        spectrumPlayer.addJavascriptInterface(this, "Spectv");
        spectrumGuide.addJavascriptInterface(this, "Spectv");
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

                spectrumPlayer.evaluateJavascript("toggleGuide('SHOWGUIDE');", null);

                return true;
            }

            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && spectrumGuide.getVisibility() != View.GONE) {

                if (spectrumGuide.canGoBack()) {
                    spectrumGuide.evaluateJavascript("history.back();", null);
                } else {
                    spectrumPlayer.evaluateJavascript("toggleGuide('HIDEGUIDE');", null);
                }

                return true;
            }

            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT && spectrumGuide.getVisibility() == View.GONE) {
                if(miniGuideIsShowing) {
                    spectrumPlayer.evaluateJavascript("$('mini-guide').last().removeClass('mini-guide-open')", null);
                    miniGuideIsShowing = false;
                }
                else {
                    spectrumPlayer.evaluateJavascript("$('mini-guide').last().addClass('mini-guide-open')", null);
                    miniGuideIsShowing = true;
                }

                return true;
            }
        }

        return super.dispatchKeyEvent(event);
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
    public void navToChannel(String channelId) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sharedPrefEdit.putString("lastChannel", baseLiveChannelURL + channelId);
                    sharedPrefEdit.apply();
                    spectrumPlayer.loadUrl(baseLiveChannelURL + channelId);
                    spectrumGuide.setVisibility(View.GONE);
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
                    if(!guideLoaded) {
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
        wv.setMixedContentMode(wv.MIXED_CONTENT_ALWAYS_ALLOW);
        wv.setUserAgentString(uaString);
    }

    private void initPlayer() {
        spectrumPlayer = (WebView) findViewById(R.id.spectv);

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
                spectrumPlayer.evaluateJavascript("var loopVar = setInterval(function() {" +
                                "try{" +
                                // Accept initial prompts
                                "document.querySelector('[aria-label=\"Continue and accept terms and conditions to go to Spectrum TV\"]')?.click();" +
                                "[...document.querySelectorAll(\"button\")]?.find(btn => btn.textContent.includes(\"Got It\"))?.click();" +
                                // Max volume
                                "$('video')[0].volume = 1.0;" +
                                "if($('video')[0]) {" +
                                "Spectv.preloadGuide();" +
                                "}" +
                                // Hide html elements except video player
                                "$('.site-header').attr('style', 'display: none');" +
                                "$('#video-controls').attr('style', 'display: none');" +
                                "$('.nav-triangle-pattern').attr('style', 'display: none');" +
                                "$('channels-filter').attr('style', 'display: none');" +
                                "$('.transparent-header').attr('style', 'display: none');" +
                                // Style mini channel guide
                                "$('#channel-browser').attr('style', 'height: 100%');" +
                                "$('.mini-guide').attr('style', 'height: 100%');" +
                                // To help with navigation with remote. Doesn't seem to do anything though
                                "$('#channel-browser').attr('style', 'tabindex: 0');" +
                                "$('#spectrum-player').attr('style', 'tabindex: 0');" +
                                "$('.site-footer').attr('style', 'display: none');" +
                                "}" +
                                "catch(e){" +
                                "console.log(e)" +
                                "}" +
                                "}, 2000);" +
                                "function toggleGuide(s) {Spectv.channelGuide(s)}" +
                                "function toggleMiniGuide(s) {Spectv.channelGuide(s)};"
                        , null);

            }

        });
        spectrumPlayer.setVerticalScrollBarEnabled(false);
        spectrumPlayer.loadUrl(lastChannelURL.isEmpty() ? newSessionURL : lastChannelURL);
    }

    private void initGuide() {
        spectrumGuide = (WebView) findViewById(R.id.spectv_guide);
        spectrumGuide.setVisibility(View.GONE);

        spectrumGuide.setBackgroundColor(Color.TRANSPARENT);
        spectrumGuide.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

        spectrumGuide.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("**************GUIDE************", consoleMessage.message() + " -- From line " +
                        consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }

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
                if(!guideLoaded) {
                    guideLoaded = true;
                    CookieManager.getInstance().setCookie(guideUrl, cookies, null);
                }
                super.onPageFinished(view, url);
                spectrumGuide.evaluateJavascript(
                        "var loopVar = setInterval(function() {" +
                                "try {" +
                                "var focusOnGuide = true;" +
                                "$('.top-level-nav').attr('style', 'display: none');" +
                                "$('.navbar').attr('style', 'display: none');" +
                                "$('.time-nav').attr('style', 'display: none');" +
                                "$('.guide').attr('style', 'width: 100%');" +
                                "$('.site-footer').attr('style', 'display: none');" +
                                "$('.filter-section').attr('style', 'display: none');" +
                                "$(\"[role='tablist']\").attr('style', 'display: none');" +

                                "var currentURL = new URL(window.location.href);" +
                                "$('button:contains(\\'Watch Live\\')').unbind('click');" +
                                "$('button:contains(\\'Watch Live\\')').on('click', function(event) {event.preventDefault(); event.stopImmediatePropagation(); Spectv.navToChannel(currentURL.searchParams.get('tmsGuideServiceId'))});" +

                                "if($('.channel-content').length > 0 && focusOnGuide) {" +
                                "$('.channel-content-list-container').attr('style', 'tabindex: 1');" +
                                "$('.channel-content-list-container').focus();" +
                                "clearInterval(loopVar);" +
                                "focusOnGuide = false;" +
                                "}" +

//                                "if($('#episode-container').length > 0) {" +
                                "$('masthead').attr('style', 'display: none');" +
                                "$('.nav-tab-bar').attr('style', 'display: none');" +
                                "$('#button-watchLiveIP-0')[0].scrollIntoView();" +
                                "$('#episode-container').attr('style', 'tabindex: 1');" +
                                "$('#button-watchLiveIP-0').attr('style', 'tabindex: 2');" +
                                "$('#button-watchLiveIP-0').focus();" +
                                "alert('loaded')" +

//                                "}" +
                                "}" +
                                "catch (error) {" +
                                "console.log(error);" +
                                "}" +
                                "}" +
                                ", 2000 " +
                                ");"
                        , null);
            }
        });
    }


}