package com.example.chiguaapp;
import android.app.*;import android.os.*;import android.content.*;import android.net.*;import android.widget.*;
public class PlayerActivity extends Activity{
    public void onCreate(android.os.Bundle b){super.onCreate(b); String url=getIntent().getStringExtra("url"); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); TextView t=new TextView(this); t.setText(url); root.addView(t); VideoView vv=new VideoView(this); root.addView(vv,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root); MediaController mc=new MediaController(this); vv.setMediaController(mc); vv.setVideoURI(Uri.parse(url)); vv.setOnErrorListener((mp,what,extra)->{try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));}catch(Exception e){} return true;}); vv.start();}
}
