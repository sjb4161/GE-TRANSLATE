package com.mini.patternshortcut;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS = "mini_shortcut";
    private static final String KEY_URL = "chat_url";
    private static final String HOME = "https://chatgpt.com/";
    private static final String ACTION_SETTINGS = "com.mini.patternshortcut.SETTINGS";
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        String action = getIntent() != null ? getIntent().getAction() : null;
        String saved = prefs.getString(KEY_URL, "").trim();

        if (!ACTION_SETTINGS.equals(action) && !saved.isEmpty()) {
            openChat(saved);
            finish();
            return;
        }
        showSetup(saved);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(Color.rgb(31, 41, 55));
        if (bold) v.setTypeface(null, 1);
        return v;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setMinHeight(dp(52));
        return b;
    }

    private void showSetup(String saved) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(28));
        root.setBackgroundColor(Color.rgb(248, 250, 252));
        scroll.addView(root);

        TextView title = text("미니 패턴번역", 26, true);
        root.addView(title);

        TextView sub = text("공식 ChatGPT Plus 바로가기", 15, true);
        sub.setTextColor(Color.rgb(16, 185, 129));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(6);
        root.addView(sub, subLp);

        TextView desc = text(
            "API Key나 별도 API 결제를 사용하지 않습니다.\n" +
            "서실장님의 공식 ChatGPT 앱을 열어 Plus 계정으로 그대로 사용합니다.\n\n" +
            "처음 한 번만 원하는 ChatGPT 대화 주소를 붙여넣어 주세요. " +
            "그 다음부터는 홈 화면의 ‘미니 패턴번역’ 아이콘만 누르면 바로 이동합니다.",
            15, false
        );
        desc.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, -2);
        dlp.topMargin = dp(22);
        root.addView(desc, dlp);

        EditText url = new EditText(this);
        url.setHint("https://chatgpt.com/c/... 대화 주소");
        url.setSingleLine(false);
        url.setMinHeight(dp(92));
        url.setText(saved);
        url.setTextSize(14);
        url.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(-1, -2);
        ulp.topMargin = dp(20);
        root.addView(url, ulp);

        Button saveOpen = button("저장하고 ChatGPT 열기");
        saveOpen.setTextColor(Color.WHITE);
        saveOpen.setBackgroundColor(Color.rgb(37, 99, 235));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(58));
        blp.topMargin = dp(16);
        root.addView(saveOpen, blp);

        Button home = button("ChatGPT 홈만 열기");
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, dp(54));
        hlp.topMargin = dp(10);
        root.addView(home, hlp);

        Button help = button("대화 주소 복사 방법");
        LinearLayout.LayoutParams helpLp = new LinearLayout.LayoutParams(-1, dp(54));
        helpLp.topMargin = dp(10);
        root.addView(help, helpLp);

        TextView tip = text(
            "나중에 주소를 바꾸려면 홈 화면에서 ‘미니 패턴번역’ 아이콘을 길게 누른 뒤 " +
            "‘대화 링크 변경’을 선택하세요.",
            13, false
        );
        tip.setTextColor(Color.GRAY);
        LinearLayout.LayoutParams tipLp = new LinearLayout.LayoutParams(-1, -2);
        tipLp.topMargin = dp(22);
        root.addView(tip, tipLp);

        TextView version = text("Shortcut v1.0 · ChatGPT 공식 앱 연결", 12, false);
        version.setTextColor(Color.LTGRAY);
        version.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(-1, -2);
        vlp.topMargin = dp(28);
        root.addView(version, vlp);

        saveOpen.setOnClickListener(v -> {
            String target = normalize(url.getText().toString());
            if (target == null) {
                Toast.makeText(this, "chatgpt.com 대화 주소를 확인해주세요.", Toast.LENGTH_LONG).show();
                return;
            }
            prefs.edit().putString(KEY_URL, target).apply();
            Toast.makeText(this, "저장했습니다. 다음부터 아이콘만 누르면 됩니다.", Toast.LENGTH_SHORT).show();
            openChat(target);
            finish();
        });

        home.setOnClickListener(v -> {
            openChat(HOME);
            finish();
        });

        help.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("대화 주소 복사 방법")
            .setMessage(
                "PC에서 지금 패턴번역 대화를 열고 브라우저 주소창의 chatgpt.com 주소를 복사해서 " +
                "휴대폰으로 보내거나, 휴대폰 ChatGPT/브라우저에서 해당 대화를 연 뒤 공유 또는 주소 복사를 이용하세요.\n\n" +
                "주소 예: https://chatgpt.com/c/..."
            )
            .setPositiveButton("확인", null)
            .show()
        );

        setContentView(scroll);
    }

    private String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "https://" + s;
        }
        try {
            Uri u = Uri.parse(s);
            String host = u.getHost();
            if (host == null) return null;
            host = host.toLowerCase();
            if (!host.equals("chatgpt.com") && !host.endsWith(".chatgpt.com")) return null;
            return s.replaceFirst("^http://", "https://");
        } catch (Exception e) {
            return null;
        }
    }

    private void openChat(String url) {
        Uri uri = Uri.parse(url);

        Intent appIntent = new Intent(Intent.ACTION_VIEW, uri);
        appIntent.setPackage("com.openai.chatgpt");
        appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(appIntent);
            return;
        } catch (ActivityNotFoundException ignored) {
        }

        Intent normal = new Intent(Intent.ACTION_VIEW, uri);
        normal.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(normal);
        } catch (ActivityNotFoundException e) {
            Intent play = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.openai.chatgpt"));
            startActivity(play);
        }
    }
}
