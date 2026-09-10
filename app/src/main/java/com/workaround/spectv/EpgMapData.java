package com.workaround.spectv;

import java.io.Serializable;


public class EpgMapData implements Serializable {
    public String offset ="0";
    public String chnum = "";
    public String tsmid = "";
    public String name = "";   // channel network name
    public String cssid = "";
    public String prevChNum = "";
    public String nextChNum = "";
    public int chindex = 0;
    public String imgsrc = "";
    public String onnow = "";
    public String episode = "";



    public EpgMapData(String o, String c, String t, String n, String css, String imgurl) {
        offset = o;
        chnum = c;
        tsmid = t;
        name = n;
        cssid = css;
        imgsrc = imgurl;
        chindex = 0;
        onnow = "NA";
        episode = "";
    }

    public EpgMapData(EpgMapData e) {
        offset = e.offset;
        chnum = e.chnum;
        tsmid = e.tsmid;
        name = e.name;
        cssid = e.cssid;
        chindex = 0;
        prevChNum = e.prevChNum;
        nextChNum = e.nextChNum;
        imgsrc = e.imgsrc;
        onnow = e.onnow;
        episode = e.episode;
    }
}

