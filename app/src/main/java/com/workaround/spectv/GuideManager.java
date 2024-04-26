package com.workaround.spectv;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class GuideManager {

    protected Context context;
    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEdit;
    final String DBVERSION = "V1.1";
    final String ROOTNODE = "-1";  // add a root entry "-1" that has head/nextChnum tail/prevChNum
    final Boolean DEBUGON = false;
    final String GuideCacheFile = "GuideCache.dat";
    boolean guideCacheIsReady = false;
    boolean miniGuideScanDone = false;
    LinkedHashMap<String, EpgMapData> guideHashMap = new LinkedHashMap<String, EpgMapData>();
    LinkedHashMap<String, String> chNameToChNum = new LinkedHashMap<String, String>();
    LinkedHashMap<String, String> tsmIDToChNum = new LinkedHashMap<String, String>();
    LinkedHashMap<String, EpgMapData> miniGuideBuffer = new LinkedHashMap<String, EpgMapData>();

    public GuideManager(Context mycontext){
        context = mycontext.getApplicationContext();
        sharedPref = context.getSharedPreferences("com.workaround.spectv.pref", MODE_PRIVATE);
        sharedPrefEdit = sharedPref.edit();
    }

    public void readMiniGuideCache() {
        // read the cached mappings from disk
        // check if db is correct version
        String dbversion = sharedPref.getString("DBVERSION", "");
        if (dbversion.equals(DBVERSION)) {
            try {
                ObjectInputStream is = new ObjectInputStream(context.openFileInput(GuideCacheFile));
                guideHashMap = (LinkedHashMap<String, EpgMapData>) is.readObject();
                // merge done, create channel up, down list also tsmid to chnum map
                guideHashMap.forEach((key, value) -> {
                    tsmIDToChNum.put(value.tsmid,key);
                });
                guideCacheIsReady = true;
                miniGuideScanDone = true;
                myDebugDump();
            } catch (Exception e) {
                MyDebug("read hashmap failed " + e);
            }

        }
    }

    public void addMiniGuideEntry(String chnum, String chname, String css, String offsetpx) {

        String[] parts = css.split("-");
        int pos = parts.length - 1;
        String tsmid = parts[pos];
        String cssid = "#" + css;
        miniGuideBuffer.put(chnum,new EpgMapData(offsetpx,chnum,tsmid,chname,cssid));
        chNameToChNum.put(chname,chnum);
    }

    public String getTsmid(String chnum) {
        EpgMapData chinfo = guideHashMap.get(chnum);
        String tsm = "";
        if ( chinfo != null) { tsm = chinfo.tsmid; }
        return tsm;
    }

    public String getChnum(String tsmid) {
        return tsmIDToChNum.get(tsmid);
    }

    public int numberOfChannels() {
        if (guideHashMap.size() > 0)
          return guideHashMap.size() - 1;
        else  return 0;
    }

    public EpgMapData getGuideEntry(String chnum) {
        return guideHashMap.get(chnum);
    }

    public EpgMapData getChannelUp(String chnum) {
        MyDebug("UP old ch = " + chnum);
        EpgMapData chinfo = guideHashMap.get(chnum);
        if (chinfo != null) {
            return guideHashMap.get(chinfo.nextChNum);
        } else {
            return chinfo;
        }
    }

    public EpgMapData getChannelDown(String chnum) {
        EpgMapData chinfo = guideHashMap.get(chnum);
        if (chinfo != null) {
            return guideHashMap.get(chinfo.prevChNum);
        } else {
            return chinfo;
        }
    }

    public String getDefaultChannel() {
        EpgMapData node = guideHashMap.get(guideHashMap.get(ROOTNODE).nextChNum);
        return node.chnum;
    }

    public String getAvailableChnum(String chnum) {
        EpgMapData node = guideHashMap.get(guideHashMap.get(ROOTNODE).nextChNum);
        // walk the channels and find a substitute channel
        String retval = "";
        int chint = Integer.parseInt(chnum);
        boolean found = false;
        while (!found) {
            if (Integer.parseInt(node.nextChNum) > chint ) {
                found = true;
                retval = node.chnum;
            }
            if ( node.chnum.equals(node.nextChNum)) {
                // end of list
                retval = node.chnum;
                found = true;
            } else {
                node = guideHashMap.get(node.nextChNum);
            }
        }
        return retval;
    }


    public boolean guideCacheIsReady() {
        return guideCacheIsReady;
    }

    public boolean miniGuideScanDone() {
        return miniGuideScanDone;
    }
    static class CompareChnum implements java.util.Comparator<String> {
        @Override
        public int compare( String a, String b) {
            // compare chnum as integers
            return Integer.parseInt(a) -Integer.parseInt(b) ;
        }
    }

    private void saveGuideData() {
        ArrayList<String> chUpDownList = new ArrayList<>();
        MyDebug("In saveGuideData() ");
        MyDebug("mini buffer size = " + miniGuideBuffer.size());

        try {
            // create channel up, down list also tsmid to chnum map
            miniGuideBuffer.forEach((key, value) -> {
                chUpDownList.add(key);
                tsmIDToChNum.put(value.tsmid,key);
            });

            // sort the buffer by channel number
            Collections.sort(chUpDownList, new CompareChnum());
            // add position back pointer to sorted channel list
            for (int i = 0; i < chUpDownList.size(); i++) {
                EpgMapData d = miniGuideBuffer.get(chUpDownList.get(i));
                d.chindex = i;
                if ( i == 0) {
                    d.nextChNum = chUpDownList.get(i+1);
                    d.prevChNum = chUpDownList.get(i);
                } else if ( i == (chUpDownList.size() - 1) ) {
                    d.nextChNum = chUpDownList.get(i);
                    d.prevChNum = chUpDownList.get(i-1);
                } else {
                    d.nextChNum = chUpDownList.get(i + 1);
                    d.prevChNum = chUpDownList.get(i - 1);
                }
            }
            // add a root entry "-1" that has head/nextChnum tail/prevChNum
            EpgMapData root = new EpgMapData("0",ROOTNODE,"","","");
            root.nextChNum = chUpDownList.get(0);
            root.prevChNum = chUpDownList.get(chUpDownList.size()-1);
            miniGuideBuffer.put(ROOTNODE,root);
            // set guide hashmap to miniguide buffer all done
            guideHashMap = miniGuideBuffer;

            // set default channel to rootnode , nextChnum
            sharedPrefEdit.putString("currentChannel", root.nextChNum);
            sharedPrefEdit.putString("prevChannel", root.nextChNum);
            sharedPrefEdit.apply();

            guideCacheIsReady = true;
            MyDebug("merged mini buffer size =" + String.valueOf(miniGuideBuffer.size()));
            myDebugDump();
            Toast.makeText(context, "Scan Guide Data Completed "+
                            "\n Found " +  numberOfChannels() + " channels",
                    Toast.LENGTH_LONG).show();

            // write hashmap to disk

            FileOutputStream fileout = context.openFileOutput(GuideCacheFile, MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fileout);
            oos.writeObject(guideHashMap);
            sharedPrefEdit.putString("DBVERSION", DBVERSION);
            sharedPrefEdit.apply();
        } catch (Exception e) {
            MyDebug("save hashmap failed" + e);
            Log.e("HASHERROR", Log.getStackTraceString(e));
        }
    }

    public void setMiniGuideLoaded() {
        MyDebug("In setMiniGuideLoaded() ");
        miniGuideScanDone = true;
        saveGuideData();
    }

    public void myDebugDump() {
        if (!DEBUGON) { return; }
        String s = String.format("%7s %7s %7s %7s %7s %7s %7s %9s","row","chnum","prevch","nextch","index","name","tsmid","offset");
        MyDebug(s);
        boolean done = false;
        String key = guideHashMap.get(ROOTNODE).nextChNum;
        int i = 0;
        while (!done) {
            EpgMapData d =  guideHashMap.get(key);
            s = String.format("%7s %7s %7s %7s %7s %7s %7s %9s",
                    String.valueOf(i),d.chnum,d.prevChNum,d.nextChNum,
                         String.valueOf(d.chindex),d.name,d.tsmid,d.offset);
            MyDebug(s);
            if (d.chnum.equals(d.nextChNum)) {
                done = true;
            } else {
                key = guideHashMap.get(d.nextChNum).chnum;
                i++;
            }
        }
        // Dump Keys

        // print keys using getKey() method
        i = 0;
        for (Map.Entry<String, EpgMapData> ite :
                guideHashMap.entrySet()) {
            MyDebug("row " + i + " key =" + ite.getKey() + ", ");
            i++;
        }

    }


    @JavascriptInterface
    public void MyDebug(String s1) {
        if (DEBUGON) {
            Log.d("SPECDEBUG", s1 );
        }
    }
}
