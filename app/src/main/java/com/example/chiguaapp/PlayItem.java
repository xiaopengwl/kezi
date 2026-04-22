package com.example.chiguaapp;

public class PlayItem {
    public String source = "默认线路";
    public String name = "";
    public String input = "";

    public PlayItem(String n, String i) {
        this("默认线路", n, i);
    }

    public PlayItem(String s, String n, String i) {
        source = s == null || s.length() == 0 ? "默认线路" : s;
        name = n == null ? "" : n;
        input = i == null ? "" : i;
    }
}
