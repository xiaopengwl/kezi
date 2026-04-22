package com.example.chiguaapp;

public class RepoSourceCatalog {
    public static class Item {
        public final String title;
        public final String fileName;
        public final String rawUrl;
        public Item(String title, String fileName) {
            this.title = title;
            this.fileName = fileName;
            this.rawUrl = "https://raw.githubusercontent.com/xiaopengwl/xiaomaojs/main/" + fileName;
        }
        @Override public String toString() { return title + "  (" + fileName + ")"; }
    }
    public static final Item[] ITEMS = new Item[]{
        new Item("4k影视", "4Kyingshi-drpy.js"),
        new Item("555电影", "555dy-drpy.js"),
        new Item("dongkadi", "dongkadi-drpy.js"),
        new Item("两个BT", "lianggebt-drpy.js"),
        new Item("可可影视", "kkyingshi-drpy.js"),
        new Item("吃瓜网t3-js-drpy-v32-playurl-fix", "chigua-band-drpy.js"),
        new Item("映像星球", "yxxq-drpy.js"),
        new Item("百思派电影网", "bxpys-drpy.js"),
        new Item("西瓜影院", "xgys-drpy.js"),
        new Item("高清仓库", "hsck-drpy.js"),
        new Item("麻豆视频", "mdsp-drpy.js")
    };
}
