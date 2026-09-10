package com.workaround.spectv;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class GuideManager {

    protected Context context;
    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEdit;
    final String DBVERSION = "V1.1";
    final String ROOTNODE = "-1";  // add a root entry "-1" that has head/nextChnum tail/prevChNum
    final Boolean DEBUGON = true;
    final String GuideCacheFile = "GuideCache.dat";
    boolean guideCacheIsReady = false;
    LinkedHashMap<String, EpgMapData> guideHashMap = new LinkedHashMap<String, EpgMapData>();
    LinkedHashMap<String, String> chNameToChNum = new LinkedHashMap<String, String>();
    LinkedHashMap<String, String> tsmIDToChNum = new LinkedHashMap<String, String>();
    LinkedHashMap<String, EpgMapData> miniGuideBuffer = new LinkedHashMap<String, EpgMapData>();
    ArrayList <EpgMapData> miniGuideList = new ArrayList<>();

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
                for (Map.Entry<String, EpgMapData> entry : guideHashMap.entrySet()) {
                 //   System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
                    tsmIDToChNum.put(entry.getValue().tsmid,entry.getKey());
                }
                /*
                guideHashMap.forEach((key, value) -> {
                    tsmIDToChNum.put(value.tsmid,key);
                });
                 */
                initMiniGuideList();
                guideCacheIsReady = true;
       //         miniGuideScanDone = true;
         //       myDebugDump();
            } catch (Exception e) {
                MyDebug("read hashmap failed " + e);
            }

        }
    }

    private void addMiniGuideEntry(String chnum, String chname, String css, String offsetpx, String imgurl) {

        String[] parts = css.split("-");
        if ( miniGuideBuffer.containsKey(chnum)) {
            return;
        }
        int pos = parts.length - 1;
        String tsmid = parts[pos];
        String cssid = "#" + css;
        miniGuideBuffer.put(chnum,new EpgMapData(offsetpx,chnum,tsmid,chname,cssid,imgurl));
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

    public void initMiniGuideList() {
        miniGuideList = new ArrayList<>();
        boolean done = false;
        String key = guideHashMap.get(ROOTNODE).nextChNum;
        int i = 0;
        while (!done) {
            EpgMapData d = guideHashMap.get(key);
            miniGuideList.add(d);
            if (d.chnum.equals(d.nextChNum)) {
                done = true;
            } else {
                key = guideHashMap.get(d.nextChNum).chnum;
                i++;
            }
        }
    }

    public void updateMiniGuideList(String jsondata) {
        try {
            //    MyDebug("Runnable --  updateMiniGuideList miniGuideList size =  " + miniGuideList.size());
            JSONObject json = new JSONObject(jsondata);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    if (guideHashMap.containsKey(key)) {
                        EpgMapData epg = guideHashMap.get(key);
                        epg.onnow = json.getJSONObject(key).getString("onnow");
                        epg.episode = json.getJSONObject(key).getString("episode");
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
      /*      json.keys().forEachRemaining(key -> {
                try {
                    if (guideHashMap.containsKey(key)) {
                        EpgMapData epg = guideHashMap.get(key);
                        epg.onnow = json.getJSONObject(key).getString("onnow");
                        epg.episode = json.getJSONObject(key).getString("episode");
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

            });*/
        } catch (JSONException e) {
            MyDebug("JSON --  updateMiniGuideList exception " + e.toString());
            throw new RuntimeException(e);
        }
    }

    public ArrayList<EpgMapData> getMiniGuideList() {
        return miniGuideList;
    }

    public boolean guideCacheIsReady() {
        return guideCacheIsReady;
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
            for (Map.Entry<String, EpgMapData> entry : miniGuideBuffer.entrySet()) {
              //  System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
                chUpDownList.add(entry.getKey());
                tsmIDToChNum.put(entry.getValue().tsmid,entry.getKey());
            }

/*
            miniGuideBuffer.forEach((key, value) -> {
                chUpDownList.add(key);
                tsmIDToChNum.put(value.tsmid,key);
            });

 */

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
            EpgMapData root = new EpgMapData("0",ROOTNODE,"","","","");
            root.nextChNum = chUpDownList.get(0);
            root.prevChNum = chUpDownList.get(chUpDownList.size()-1);
            miniGuideBuffer.put(ROOTNODE,root);
            // set guide hashmap to miniguide buffer all done
            guideHashMap = miniGuideBuffer;

            // set default channel to rootnode , nextChnum, unless already set ie. doing rescan
            String curchan = sharedPref.getString("currentChannel","");
            if ( curchan.isEmpty()) {
                sharedPrefEdit.putString("currentChannel", root.nextChNum);
                sharedPrefEdit.putString("prevChannel", root.nextChNum);
                sharedPrefEdit.apply();
            }
            initMiniGuideList();

            guideCacheIsReady = true;
            MyDebug("merged mini buffer size =" + String.valueOf(miniGuideBuffer.size()));
          //  myDebugDump();
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


    public void initMiniGuideData(String jsonData) {
        // add all the miniguide data to the buffer, jsonData contains scan of miniguide
        MyDebug("In initMiniGuideData() ");
        // create a new buffer for the data
        miniGuideBuffer = new LinkedHashMap<>();
        chNameToChNum = new LinkedHashMap<>();
        tsmIDToChNum = new LinkedHashMap<>();
        try {
            //    MyDebug("Runnable --  updateMiniGuideList miniGuideList size =  " + miniGuideList.size());
            JSONObject json = new JSONObject(jsonData);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String chnum = keys.next();
                try {
                    String chname, css, offsetpx, imgurl;
                    chname = json.getJSONObject(chnum).getString("chname");
                    css = json.getJSONObject(chnum).getString("css");
                    offsetpx = json.getJSONObject(chnum).getString("offset");
                    imgurl = json.getJSONObject(chnum).getString("imgurl");
                    addMiniGuideEntry(chnum, chname, css, offsetpx, imgurl);
                } catch (JSONException e) {
                    MyDebug("JSON --  updateMiniGuideList exception in chnum " + chnum);
                    throw new RuntimeException(e);
                }
            }

      /*      json.keys().forEachRemaining(chnum -> {
                try {
                    String chname, css, offsetpx, imgurl;
                    chname = json.getJSONObject(chnum).getString("chname");
                    css = json.getJSONObject(chnum).getString("css");
                    offsetpx = json.getJSONObject(chnum).getString("offset");
                    imgurl = json.getJSONObject(chnum).getString("imgurl");
                    addMiniGuideEntry(chnum, chname, css, offsetpx, imgurl);
                } catch (JSONException e) {
                    MyDebug("JSON --  updateMiniGuideList exception in chnum " + chnum);
                    throw new RuntimeException(e);
                }

            });
       */
        } catch (JSONException e) {
            MyDebug("JSON --  updateMiniGuideList exception " + e.toString());
            throw new RuntimeException(e);
        }
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
            s = String.format("%7s %7s %7s %7s %7s %7s %7s %9s %20s",
                    String.valueOf(i),d.chnum,d.prevChNum,d.nextChNum,
                    String.valueOf(d.chindex),d.name,d.tsmid,d.offset,d.imgsrc);
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
    public void MyDebug(String s1) {
        if (DEBUGON) {
            Log.d("SPECDEBUG", s1 );
        }
    }
}
