package com.workaround.spectv;

import java.io.Serializable;


public class EpgMapData implements Serializable {
    public String offset ="0";
    public String chnum = "";
    public String tsmid = "";
    public String name = "";
    public String cssid = "";
    public String prevChNum = "";
    public String nextChNum = "";
    public int chindex = 0;

    public EpgMapData(String o, String c, String t, String n, String css) {
        offset = o;
        chnum = c;
        tsmid = t;
        name = n;
        cssid = css;
        chindex = 0;
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
    }
}

