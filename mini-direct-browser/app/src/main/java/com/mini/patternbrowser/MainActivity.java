package com.mini.patternbrowser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class MainActivity extends Activity {
  private static final String URL="https://chatgpt.com/g/g-p-69a629a06ff08191b8043b552d9484c7-haeoee-eobcewa-sotonghagi-wihan-bongjeeeoneo-caetingbang/c/69a84478-4bc4-8323-993b-9e7d1a12331a";
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(i);
    finish();
  }
}
