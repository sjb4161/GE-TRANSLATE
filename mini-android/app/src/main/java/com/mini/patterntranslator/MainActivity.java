package com.mini.patterntranslator;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int REQ_SPEECH=11, REQ_CAMERA=12, REQ_MIC_PERMISSION=21, REQ_CAM_PERMISSION=22;
    private static final String API="https://api.openai.com/v1/responses";
    private static final String VERSION="7.0.0";

    private EditText input;
    private TextView zhResult,enResult,status;
    private Button translateBtn,micBtn,cameraBtn,settingsBtn,historyBtn;
    private TextToSpeech tts;
    private android.content.SharedPreferences prefs;
    private LinearLayout root;
    private final int blue=Color.rgb(37,99,235);

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("mini_settings",MODE_PRIVATE);
        tts=new TextToSpeech(this,this);
        buildUi();
        if(getKey().isEmpty()) showSettings(true);
    }

    private TextView label(String s,int sp,boolean bold){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(Color.rgb(31,41,55));
        if(bold) v.setTypeface(null,1); return v;
    }
    private Button button(String s){
        Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(15); b.setMinHeight(dp(48)); return b;
    }
    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    private void pad(View v,int n){ int d=dp(n); v.setPadding(d,d,d,d); }

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(244,246,249)); pad(root,16);
        scroll.addView(root,new ScrollView.LayoutParams(-1,-2));

        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("미니 · 패턴실무 AI 번역비서",20,true);
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(0,-2,1); top.addView(title,tlp);
        settingsBtn=button("⚙"); top.addView(settingsBtn,new LinearLayout.LayoutParams(dp(58),dp(50)));
        root.addView(top);

        status=label("● ChatGPT · Android APK v"+VERSION,12,false); status.setTextColor(Color.rgb(16,185,129)); root.addView(status);
        addSpace(12);

        input=new EditText(this); input.setHint("번역할 패턴/봉제 내용을 입력하세요"); input.setTextSize(17); input.setGravity(Gravity.TOP);
        input.setMinHeight(dp(150)); input.setBackgroundColor(Color.WHITE); pad(input,14);
        root.addView(input,new LinearLayout.LayoutParams(-1,-2));
        addSpace(10);

        LinearLayout actions=new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        micBtn=button("🎤 음성"); cameraBtn=button("📷 사진"); historyBtn=button("기록");
        actions.addView(micBtn,new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(cameraBtn,new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(historyBtn,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(actions);
        addSpace(8);

        translateBtn=button("번역하기"); translateBtn.setTextColor(Color.WHITE); translateBtn.setBackgroundColor(blue); translateBtn.setTextSize(17);
        root.addView(translateBtn,new LinearLayout.LayoutParams(-1,dp(56)));
        addSpace(14);

        root.addView(label("중국어(간체)",15,true));
        zhResult=resultBox();
        root.addView(zhResult);
        LinearLayout zhBtns=new LinearLayout(this);
        Button zSpeak=button("🔊 듣기"); Button zCopy=button("복사");
        zhBtns.addView(zSpeak,new LinearLayout.LayoutParams(0,-2,1)); zhBtns.addView(zCopy,new LinearLayout.LayoutParams(0,-2,1)); root.addView(zhBtns);
        addSpace(14);

        root.addView(label("영어",15,true));
        enResult=resultBox();
        root.addView(enResult);
        LinearLayout enBtns=new LinearLayout(this);
        Button eSpeak=button("🔊 듣기"); Button eCopy=button("복사");
        enBtns.addView(eSpeak,new LinearLayout.LayoutParams(0,-2,1)); enBtns.addView(eCopy,new LinearLayout.LayoutParams(0,-2,1)); root.addView(enBtns);
        addSpace(24);

        TextView foot=label("패턴·봉제·CLO3D·공장 실무 표현 우선 · API Key는 이 기기에만 저장",11,false);
        foot.setTextColor(Color.GRAY); root.addView(foot);

        setContentView(scroll);

        translateBtn.setOnClickListener(v->translate());
        settingsBtn.setOnClickListener(v->showSettings(false));
        micBtn.setOnClickListener(v->startSpeech());
        cameraBtn.setOnClickListener(v->startCamera());
        historyBtn.setOnClickListener(v->showHistory());
        zSpeak.setOnClickListener(v->speak(zhResult.getText().toString(),"zh-CN"));
        eSpeak.setOnClickListener(v->speak(enResult.getText().toString(),"en-US"));
        zCopy.setOnClickListener(v->copy(zhResult.getText().toString()));
        eCopy.setOnClickListener(v->copy(enResult.getText().toString()));
    }

    private TextView resultBox(){
        TextView v=label("번역 결과가 여기에 표시됩니다.",16,false);
        v.setTextColor(Color.rgb(55,65,81)); v.setMinHeight(dp(110)); v.setBackgroundColor(Color.WHITE); pad(v,14);
        v.setTextIsSelectable(true); return v;
    }
    private void addSpace(int h){ Space s=new Space(this); root.addView(s,new LinearLayout.LayoutParams(1,dp(h))); }

    private String getKey(){ return prefs.getString("api_key","").trim(); }
    private String getModel(){ return prefs.getString("model","gpt-5.6").trim(); }
    private String getTone(){ return prefs.getString("tone","field"); }

    private void showSettings(boolean first){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); pad(box,4);
        EditText key=new EditText(this); key.setHint("OpenAI API Key (sk-...)"); key.setText(getKey()); key.setSingleLine(true);
        EditText model=new EditText(this); model.setHint("모델"); model.setText(getModel()); model.setSingleLine(true);
        Spinner tone=new Spinner(this);
        String[] tones={"현장 실무 · 친근","일상 대화 · 자연스럽게","QC/작업지시 · 정확하게"};
        ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,tones); tone.setAdapter(ad);
        String t=getTone(); tone.setSelection("daily".equals(t)?1:"qc".equals(t)?2:0);
        TextView info=label("앱 버전 "+VERSION+"\nAPI Key는 APK 안에 포함되지 않으며, 입력한 값은 이 휴대폰 내부 설정에 저장됩니다.",12,false);
        info.setTextColor(Color.GRAY);
        box.addView(key); box.addView(model); box.addView(tone); box.addView(info);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(first?"처음 설정":"시스템 설정").setView(box)
            .setPositiveButton("저장",null).setNegativeButton(first?"나중에":"취소",null)
            .setNeutralButton("앱 정보",(x,w)->showInfo()).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String k=key.getText().toString().trim();
            String m=model.getText().toString().trim();
            if(k.isEmpty()){ Toast.makeText(this,"OpenAI API Key를 입력해주세요.",Toast.LENGTH_SHORT).show(); return; }
            String tv=tone.getSelectedItemPosition()==1?"daily":tone.getSelectedItemPosition()==2?"qc":"field";
            prefs.edit().putString("api_key",k).putString("model",m.isEmpty()?"gpt-5.6":m).putString("tone",tv).apply();
            status.setText("● ChatGPT 연결 준비 · Android APK v"+VERSION); d.dismiss();
        }));
        d.show();
    }

    private void showInfo(){
        new AlertDialog.Builder(this).setTitle("미니 패턴번역")
            .setMessage("Android APK v"+VERSION+"\n\n• 한국어 → 중국어(간체)+영어 동시 번역\n• 봉제/패턴/CLO3D 현장용어 우선\n• 음성 입력 및 TTS\n• 사진 분석\n• 로컬 번역기록\n\n이 APK는 공식 ChatGPT 앱과 별도 앱입니다.")
            .setPositiveButton("확인",null).show();
    }

    private String systemPrompt(){
        String tone=getTone();
        String style="공장에 그대로 보내도 되는 친근하고 짧고 명확한 현장 실무 표현";
        if("daily".equals(tone)) style="일상에서 실제 사람이 말하는 자연스럽고 친근한 표현";
        if("qc".equals(tone)) style="QC 작업지시처럼 정중하지만 단호하고 오해 없는 표현";
        return "당신은 미니, 의류 패턴·봉제·CLO3D·샘플실·중국/해외 공장 전문 통역 비서입니다. "+
            "직역보다 실제 현장 표현을 우선하고 "+style+"을 사용하세요. "+
            "중요 용어: 오바로크=OVERLOCK/OVEREDGE=拷边/锁边, 가이루빠=COVERSTITCH/COVERSEAM=绷缝/坎车, "+
            "오도롬프/오드람쁘=FLATLOCK/FLAT-SEAM COVERSTITCH=平锁缝/四针六线拼缝, 삼봉=3-NEEDLE COVERSTITCH=三针绷缝, "+
            "니혼바리=DOUBLE-NEEDLE CHAINSTITCH=双针链式线迹, 바인딩=BINDING=包边, 랍빠=BINDER=拉筒, "+
            "말아박기=NARROW HEM/ROLLED HEM=卷边/窄折边, 시접=SEAM ALLOWANCE=缝份, 결방향=GRAIN DIRECTION=布纹方向. "+
            "특히 오도롬프를 rolled hem으로 번역하지 마세요. 품번, 치수, 단위, 앞뒤, 좌우는 정확히 보존하세요.";
    }

    private void translate(){
        String text=input.getText().toString().trim();
        if(text.isEmpty()){ Toast.makeText(this,"번역할 내용을 입력해주세요.",Toast.LENGTH_SHORT).show(); return; }
        if(getKey().isEmpty()){ showSettings(true); return; }
        setBusy(true,"번역 중...");
        new Thread(()->{
            try{
                JSONObject schema=new JSONObject()
                    .put("type","object")
                    .put("properties",new JSONObject()
                        .put("zh",new JSONObject().put("type","string"))
                        .put("en",new JSONObject().put("type","string")))
                    .put("required",new JSONArray().put("zh").put("en"))
                    .put("additionalProperties",false);
                JSONArray inputArr=new JSONArray()
                    .put(new JSONObject().put("role","system").put("content",systemPrompt()))
                    .put(new JSONObject().put("role","user").put("content","다음을 중국어(간체)와 영어로 번역하세요. 원문: "+text));
                JSONObject body=new JSONObject()
                    .put("model",getModel())
                    .put("input",inputArr)
                    .put("max_output_tokens",1200)
                    .put("text",new JSONObject().put("format",new JSONObject()
                        .put("type","json_schema").put("name","translation").put("strict",true).put("schema",schema)));
                String raw=post(body);
                String out=extractOutputText(new JSONObject(raw));
                JSONObject r=new JSONObject(out);
                String zh=r.optString("zh"), en=r.optString("en");
                runOnUiThread(()->{
                    zhResult.setText(zh); enResult.setText(en); setBusy(false,"● ChatGPT 연결됨 · v"+VERSION);
                    saveHistory(text,zh,en);
                });
            }catch(Exception e){
                runOnUiThread(()->{ setBusy(false,"● 오류"); showError(e); });
            }
        }).start();
    }

    private String post(JSONObject body) throws Exception{
        URL u=new URL(API);
        HttpURLConnection c=(HttpURLConnection)u.openConnection();
        c.setRequestMethod("POST"); c.setConnectTimeout(30000); c.setReadTimeout(120000); c.setDoOutput(true);
        c.setRequestProperty("Authorization","Bearer "+getKey());
        c.setRequestProperty("Content-Type","application/json");
        byte[] data=body.toString().getBytes(StandardCharsets.UTF_8);
        try(OutputStream os=c.getOutputStream()){ os.write(data); }
        int code=c.getResponseCode();
        InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        String res=readAll(is);
        if(code<200||code>=300) throw new Exception("API "+code+": "+res);
        return res;
    }
    private String readAll(InputStream is)throws Exception{
        if(is==null)return ""; ByteArrayOutputStream b=new ByteArrayOutputStream(); byte[] x=new byte[8192]; int n;
        while((n=is.read(x))!=-1)b.write(x,0,n); return b.toString("UTF-8");
    }
    private String extractOutputText(JSONObject data)throws Exception{
        if(data.has("output_text")) return data.optString("output_text");
        JSONArray out=data.optJSONArray("output");
        if(out!=null) for(int i=0;i<out.length();i++){
            JSONArray content=out.getJSONObject(i).optJSONArray("content");
            if(content!=null) for(int j=0;j<content.length();j++){
                JSONObject c=content.getJSONObject(j);
                if("output_text".equals(c.optString("type"))) return c.optString("text");
            }
        }
        throw new Exception("응답 텍스트를 찾지 못했습니다.");
    }
    private void setBusy(boolean b,String s){
        translateBtn.setEnabled(!b); micBtn.setEnabled(!b); cameraBtn.setEnabled(!b); status.setText(s);
    }
    private void showError(Exception e){
        String m=e.getMessage()==null?"알 수 없는 오류":e.getMessage();
        if(m.contains("401")) m="API Key를 확인해주세요.";
        else if(m.contains("429")) m="OpenAI API 이용량 또는 결제 한도를 확인해주세요.";
        new AlertDialog.Builder(this).setTitle("번역 오류").setMessage(m).setPositiveButton("확인",null).show();
    }

    private void startSpeech(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC_PERMISSION); return;
        }
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT,"한국어로 말씀하세요");
        try{ startActivityForResult(i,REQ_SPEECH); }catch(Exception e){ Toast.makeText(this,"음성 인식을 사용할 수 없습니다.",Toast.LENGTH_SHORT).show(); }
    }

    private void startCamera(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA},REQ_CAM_PERMISSION); return;
        }
        Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try{ startActivityForResult(i,REQ_CAMERA); }catch(Exception e){ Toast.makeText(this,"카메라를 사용할 수 없습니다.",Toast.LENGTH_SHORT).show(); }
    }

    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(result!=RESULT_OK||data==null)return;
        if(req==REQ_SPEECH){
            ArrayList<String> r=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if(r!=null&&!r.isEmpty()){ input.setText(r.get(0)); input.setSelection(input.length()); }
        }else if(req==REQ_CAMERA){
            Object o=data.getExtras()!=null?data.getExtras().get("data"):null;
            if(o instanceof Bitmap) analyzeImage((Bitmap)o);
        }
    }

    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){
        super.onRequestPermissionsResult(r,p,g);
        if(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED){
            if(r==REQ_MIC_PERMISSION)startSpeech(); else if(r==REQ_CAM_PERMISSION)startCamera();
        }
    }

    private void analyzeImage(Bitmap bm){
        if(getKey().isEmpty()){ showSettings(true); return; }
        setBusy(true,"사진 분석 중...");
        new Thread(()->{
            try{
                ByteArrayOutputStream os=new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG,85,os);
                String b64=Base64.encodeToString(os.toByteArray(),Base64.NO_WRAP);
                JSONArray content=new JSONArray()
                    .put(new JSONObject().put("type","input_text").put("text","이 의류/패턴/봉제 사진을 분석하고 보이는 텍스트나 공정 문제를 한국어로 간단히 정리한 뒤, 공장에 전달할 중국어(간체)와 영어 문장을 함께 작성하세요."))
                    .put(new JSONObject().put("type","input_image").put("image_url","data:image/jpeg;base64,"+b64));
                JSONArray arr=new JSONArray()
                    .put(new JSONObject().put("role","system").put("content",systemPrompt()))
                    .put(new JSONObject().put("role","user").put("content",content));
                JSONObject body=new JSONObject().put("model",getModel()).put("input",arr).put("max_output_tokens",1400);
                String out=extractOutputText(new JSONObject(post(body)));
                runOnUiThread(()->{
                    input.setText(out); zhResult.setText("사진 분석 결과를 입력칸에 넣었습니다."); enResult.setText("필요하면 내용을 수정한 뒤 번역하기를 누르세요.");
                    setBusy(false,"● 사진 분석 완료 · v"+VERSION);
                });
            }catch(Exception e){ runOnUiThread(()->{setBusy(false,"● 오류");showError(e);}); }
        }).start();
    }

    private void speak(String text,String lang){
        if(text==null||text.trim().isEmpty())return;
        Locale l=Locale.forLanguageTag(lang); tts.setLanguage(l); tts.setSpeechRate(0.9f); tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"mini");
    }
    @Override public void onInit(int s){}
    @Override protected void onDestroy(){ if(tts!=null){tts.stop();tts.shutdown();} super.onDestroy(); }

    private void copy(String s){
        android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("translation",s)); Toast.makeText(this,"복사되었습니다.",Toast.LENGTH_SHORT).show();
    }

    private void saveHistory(String src,String zh,String en){
        try{
            JSONArray a=new JSONArray(prefs.getString("history","[]"));
            JSONObject o=new JSONObject().put("src",src).put("zh",zh).put("en",en).put("time",System.currentTimeMillis());
            JSONArray n=new JSONArray().put(o);
            for(int i=0;i<Math.min(a.length(),29);i++)n.put(a.getJSONObject(i));
            prefs.edit().putString("history",n.toString()).apply();
        }catch(Exception ignored){}
    }

    private void showHistory(){
        try{
            JSONArray a=new JSONArray(prefs.getString("history","[]"));
            if(a.length()==0){ Toast.makeText(this,"저장된 번역이 없습니다.",Toast.LENGTH_SHORT).show(); return; }
            String[] items=new String[a.length()];
            for(int i=0;i<a.length();i++){ JSONObject o=a.getJSONObject(i); items[i]=o.optString("src"); }
            new AlertDialog.Builder(this).setTitle("최근 번역")
                .setItems(items,(d,which)->{
                    try{
                        JSONObject o=a.getJSONObject(which); input.setText(o.optString("src")); zhResult.setText(o.optString("zh")); enResult.setText(o.optString("en"));
                    }catch(Exception ignored){}
                })
                .setNegativeButton("닫기",null)
                .setNeutralButton("전체 삭제",(d,w)->prefs.edit().remove("history").apply()).show();
        }catch(Exception e){ Toast.makeText(this,"기록을 읽지 못했습니다.",Toast.LENGTH_SHORT).show(); }
    }
}
