package com.example.geetasaar;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.*;
import java.util.*;
import java.net.*;
import java.io.*;
import org.json.*;

public class MainActivity extends Activity {
    private final int BG=0xFFF7F1E6, INK=0xFF20243F, MUTED=0xFF777487, GOLD=0xFFD68A36, CARD=0xFFFFFCF6;
    private LinearLayout root, content;
    private String page="home";
    private int selectedChapter=1;
    private final Set<String> saved=new HashSet<>();
    private final ArrayList<Verse> verses=new ArrayList<>();
    private final String[] chapterNames={
        "Arjuna Vishada Yoga","Sankhya Yoga","Karma Yoga","Jnana Karma Sannyasa Yoga",
        "Karma Sannyasa Yoga","Dhyana Yoga","Jnana Vijnana Yoga","Akshara Brahma Yoga",
        "Raja Vidya Raja Guhya Yoga","Vibhuti Yoga","Vishvarupa Darshana Yoga","Bhakti Yoga",
        "Kshetra Kshetrajna Vibhaga Yoga","Gunatraya Vibhaga Yoga","Purushottama Yoga",
        "Daivasura Sampad Vibhaga Yoga","Shraddhatraya Vibhaga Yoga","Moksha Sannyasa Yoga"
    };
    private final String[] summaries={
        "Arjuna is overcome by sorrow on the battlefield and seeks guidance.",
        "Krishna explains the eternal nature of the Self and the wisdom of equanimity.",
        "The path of selfless action: act with dedication, without attachment to results.",
        "Wisdom, action and the importance of a living spiritual tradition.",
        "Renunciation and disciplined action lead toward the same freedom.",
        "The practice of meditation, balance and mastery of the mind.",
        "Knowing the Divine in both its manifest and unmanifest forms.",
        "Remembering the eternal truth at the final moment and beyond.",
        "The royal knowledge of devotion, offered with love and sincerity.",
        "Seeing the Divine presence in the finest and most powerful expressions of life.",
        "Arjuna beholds the vast universal form of the Divine.",
        "The path of loving devotion and the qualities of a dear devotee.",
        "Understanding the field of experience and the knower of the field.",
        "How the three gunas shape thought, action and understanding.",
        "The imperishable source that sustains all existence.",
        "The contrast between qualities that liberate and those that bind.",
        "Faith expressed through food, worship, discipline and giving.",
        "Freedom through discernment, surrender and wholehearted action."
    };

    static class Verse {
        int chapter, number; String sanskrit, transliteration, meaning, reflection;
        Verse(int c,int n,String s,String t,String m,String r){chapter=c;number=n;sanskrit=s;transliteration=t;meaning=m;reflection=r;}
        String id(){return chapter+"."+number;}
    }

    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(0xFF171B36); getWindow().setNavigationBarColor(0xFF171B36);
        loadVerses(); saved.addAll(getPreferences(0).getStringSet("saved",new HashSet<>()));
        showHome();
        loadCompleteGitaOnline();
    }


    // Loads the complete 700-verse text and English translations, then caches both JSON files.
    // First launch needs internet; later launches use the cached files when available.
    private void loadCompleteGitaOnline() {
        new Thread(() -> {
            try {
                File baseFile = new File(getFilesDir(), "gita_base.json");
                File enFile = new File(getFilesDir(), "gita_en.json");
                if (!baseFile.exists() || !enFile.exists()) {
                    String baseJson = downloadText("https://cdn.jsdelivr.net/gh/AdiyogiArts/gita-json@main/base.json");
                    String enJson = downloadText("https://cdn.jsdelivr.net/gh/AdiyogiArts/gita-json@main/en.json");
                    writeText(baseFile, baseJson);
                    writeText(enFile, enJson);
                }
                JSONObject baseData = new JSONObject(readText(baseFile));
                JSONObject enData = new JSONObject(readText(enFile));
                ArrayList<Verse> complete = new ArrayList<>();
                for (int chapter = 1; chapter <= 18; chapter++) {
                    int count = 0;
                    for (Iterator<String> it = baseData.keys(); it.hasNext();) {
                        String key = it.next();
                        JSONObject item = baseData.optJSONObject(key);
                        if (item == null || item.optInt("chapter", -1) != chapter) continue;
                        int number = item.optInt("verse", 0);
                        JSONObject translation = enData.optJSONObject(key);
                        String meaning = translation == null ? "English translation unavailable." :
                                translation.optString("translation", "English translation unavailable.");
                        String sanskrit = item.optString("sanskrit", "");
                        String transliteration = item.optString("transliteration", "");
                        if (!sanskrit.trim().isEmpty()) {
                            complete.add(new Verse(chapter, number, sanskrit, transliteration, meaning,
                                    translation == null ? "" : translation.optString("explanation", "")));
                            count++;
                        }
                    }
                }
                Collections.sort(complete, (a,b) -> {
                    if (a.chapter != b.chapter) return a.chapter - b.chapter;
                    return a.number - b.number;
                });
                if (complete.size() >= 690) {
                    runOnUiThread(() -> {
                        verses.clear();
                        verses.addAll(complete);
                        if ("chapter".equals(page)) showChapter(selectedChapter);
                        else if ("saved".equals(page)) showSaved();
                        else if ("chapters".equals(page)) showChapters();
                        else showHome();
                        Toast.makeText(MainActivity.this, "Complete Bhagavad Gita loaded: " + complete.size() + " verses", Toast.LENGTH_LONG).show();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Could not verify the complete verse collection. Please check your internet connection.", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connect to the internet once to load all 700 shlokas.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String downloadText(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "GeetaSaar-Android/1.0");
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("HTTP " + code);
            InputStream in = connection.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            in.close();
            return out.toString("UTF-8");
        } finally { connection.disconnect(); }
    }

    private void writeText(File file, String value) throws Exception {
        FileOutputStream out = new FileOutputStream(file);
        out.write(value.getBytes("UTF-8")); out.close();
    }

    private String readText(File file) throws Exception {
        FileInputStream in = new FileInputStream(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int n;
        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        in.close(); return out.toString("UTF-8");
    }

    private void loadVerses(){
        verses.add(new Verse(2,47,"कर्मण्येवाधिकारस्ते मा फलेषु कदाचन।\\nमा कर्मफलहेतुर्भूर्मा ते सङ्गोऽस्त्वकर्मणि॥","Karmaṇy-evādhikāras te mā phaleṣhu kadāchana","You have a right to perform your prescribed duty, but you are not entitled to the fruits of action. Do not consider yourself the cause of the results, nor be attached to inaction.","Give your best to the work in front of you. Let sincerity guide the effort, not anxiety about the outcome."));
        verses.add(new Verse(2,14,"मात्रास्पर्शास्तु कौन्तेय शीतोष्णसुखदुःखदाः।\\nआगमापायिनोऽनित्यास्तांस्तितिक्षस्व भारत॥","Mātrā-sparśās tu kaunteya śītoṣhṇa-sukha-duḥkha-dāḥ","Pleasure and pain arise from contact with the senses. They come and go; they are temporary. Learn to meet them with steadiness.","A difficult moment is not your whole story. Pause, breathe, and allow it to pass."));
        verses.add(new Verse(3,19,"तस्मादसक्तः सततं कार्यं कर्म समाचर।","Tasmād asaktaḥ satataṁ kāryaṁ karma samāchara","Therefore, without attachment, constantly perform the work that is your duty.","Let action become an offering. Do what is right, with a clear and generous heart."));
        verses.add(new Verse(4,7,"यदा यदा हि धर्मस्य ग्लानिर्भवति भारत।\\nअभ्युत्थानमधर्मस्य तदात्मानं सृजाम्यहम्॥","Yadā yadā hi dharmasya glānir bhavati Bhārata","Whenever righteousness declines and unrighteousness rises, I manifest Myself.","Hope returns when we choose courage, compassion and what is right."));
        verses.add(new Verse(6,5,"उद्धरेदात्मनात्मानं नात्मानमवसादयेत्।","Uddhared ātmanātmānaṁ nātmānam avasādayet","Elevate yourself through your own mind; do not let yourself fall.","Be a friend to your own mind. Small, kind choices can lift you forward."));
        verses.add(new Verse(6,26,"यतो यतो निश्चरति मनश्चञ्चलमस्थिरम्।\\nततस्ततो नियम्यैतदात्मन्येव वशं नयेत्॥","Yato yato niścharati manaś chañchalam asthiram","Whenever the restless mind wanders, gently bring it back under the guidance of the Self.","Meditation is not never wandering; it is the gentle practice of returning."));
        verses.add(new Verse(9,26,"पत्रं पुष्पं फलं तोयं यो मे भक्त्या प्रयच्छति।","Patraṁ puṣhpaṁ phalaṁ toyaṁ yo me bhaktyā prayachchhati","Whoever offers Me a leaf, a flower, a fruit or water with devotion, I accept it.","A small offering made with love can carry the greatest meaning."));
        verses.add(new Verse(12,13,"अद्वेष्टा सर्वभूतानां मैत्रः करुण एव च।","Adveṣhṭā sarva-bhūtānāṁ maitraḥ karuṇa eva cha","One who is free from malice toward all beings, friendly and compassionate, is dear to the Divine.","Let kindness be your strength. Make room for another person's humanity."));
        verses.add(new Verse(18,66,"सर्वधर्मान्परित्यज्य मामेकं शरणं व्रज।\\nअहं त्वां सर्वपापेभ्यो मोक्षयिष्यामि मा शुचः॥","Sarva-dharmān parityajya mām ekaṁ śharaṇaṁ vraja","Take refuge in the Divine alone. I shall free you from all bondage; do not grieve.","When you feel overwhelmed, return to trust. You do not have to carry everything alone."));
        verses.add(new Verse(2,20,"न जायते म्रियते वा कदाचिन्","Na jāyate mriyate vā kadāchin","The Self is never born, nor does it ever die.","The Gita invites us to look beyond what changes and discover the stillness within."));
        verses.add(new Verse(10,20,"अहमात्मा गुडाकेश सर्वभूताशयस्थितः।","Aham ātmā guḍākeśa sarva-bhūtāśaya-sthitaḥ","I am the Self dwelling in the hearts of all beings.","See the sacred in every life, including your own."));
        verses.add(new Verse(5,18,"विद्याविनयसम्पन्ने ब्राह्मणे गवि हस्तिनि।","Vidya-vinaya-sampanne brāhmaṇe gavi hastini","The wise see with equal vision a learned person, an animal and all beings.","True wisdom softens the ego and widens our sense of belonging."));
    }

    private void setup(String title, String subtitle){
        root=new LinearLayout(this); root.setOrientation(1); root.setBackgroundColor(BG);
        setContentView(root);
        LinearLayout header=new LinearLayout(this); header.setOrientation(1); header.setPadding(dp(22),dp(22),dp(22),dp(18)); header.setBackground(gradient(0xFF171B36,0xFF34345A,28));
        TextView brand=txt("✦  GEETA SAAR",11,0xFFE7B56E,true); header.addView(brand);
        TextView h=txt(title,27,0xFFFFF8EC,true); h.setPadding(0,dp(10),0,dp(3)); header.addView(h);
        TextView sub=txt(subtitle,13,0xFFD5D1D9,false); header.addView(sub); root.addView(header);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); content=new LinearLayout(this); content.setOrientation(1); content.setPadding(dp(18),dp(18),dp(18),dp(22)); scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this); nav.setPadding(dp(8),dp(8),dp(8),dp(8)); nav.setBackgroundColor(0xFFFFFCF6);
        addNav(nav,"⌂","Home","home"); addNav(nav,"☼","Chapters","chapters"); addNav(nav,"♡","Saved","saved"); root.addView(nav);
    }
    private void addNav(LinearLayout nav,String icon,String label,String target){
        LinearLayout item=new LinearLayout(this); item.setGravity(Gravity.CENTER); item.setOrientation(1); item.setPadding(0,dp(4),0,dp(3));
        TextView i=txt(icon,20,page.equals(target)?GOLD:MUTED,true); i.setGravity(Gravity.CENTER); item.addView(i);
        TextView t=txt(label,10,page.equals(target)?INK:MUTED,true); t.setGravity(Gravity.CENTER); item.addView(t);
        item.setOnClickListener(v->{if(target.equals("home"))showHome();else if(target.equals("chapters"))showChapters();else showSaved();});
        nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));
    }
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable gradient(int a,int b,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));return d;}
    private void gap(int h){View v=new View(this);content.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private void section(String s){TextView t=txt(s,18,INK,true);t.setPadding(0,dp(8),0,dp(10));content.addView(t);}
    private TextView button(String label,Runnable run){TextView b=txt(label,14,INK,true);b.setGravity(Gravity.CENTER);b.setPadding(dp(15),dp(12),dp(15),dp(12));b.setBackground(gradient(0xFFFFE9C7,0xFFFFF3DF,16));b.setOnClickListener(v->run.run());return b;}
    private void card(View v){LinearLayout wrap=new LinearLayout(this);wrap.setPadding(dp(16),dp(15),dp(16),dp(15));wrap.setBackground(gradient(CARD,CARD,18));wrap.addView(v);content.addView(wrap,new LinearLayout.LayoutParams(-1,-2));gap(12);}
    private void showHome(){page="home";setup("A little wisdom, every day","A timeless guide to a more mindful life");
        TextView intro=txt("The Bhagavad Gita is a conversation about duty, courage, devotion and the nature of the Self.",15,MUTED,false);intro.setLineSpacing(dp(3),1);card(intro);
        section("TODAY'S VERSE");
        Verse daily=verses.get((int)(System.currentTimeMillis()/86400000L%verses.size()));
        verseCard(daily,true);
        section("EXPLORE THE GITA");
        LinearLayout row=new LinearLayout(this);row.addView(button("18 Chapters",()->showChapters()),new LinearLayout.LayoutParams(0,-2,1));View space=new View(this);row.addView(space,new LinearLayout.LayoutParams(dp(10),1));row.addView(button("Saved verses",()->showSaved()),new LinearLayout.LayoutParams(0,-2,1));content.addView(row);
        gap(20);TextView note=txt("“When meditation is mastered, the mind is unwavering like the flame of a lamp in a windless place.”",16,INK,true);note.setLineSpacing(dp(5),1);card(note);
        TextView foot=txt("Made for reflection • Read slowly, live gently",11,MUTED,false);foot.setGravity(Gravity.CENTER);content.addView(foot);
    }
    private void showChapters(){page="chapters";setup("The 18 chapters","Choose a chapter and explore its central teaching");
        for(int i=0;i<18;i++){final int n=i+1;LinearLayout box=new LinearLayout(this);box.setOrientation(1);
            int[] verseCounts={47,72,43,42,29,47,30,28,34,42,55,20,34,27,20,24,28,78};
            TextView num=txt(String.format(Locale.getDefault(),"CHAPTER %02d  •  %d SHLOKAS",n,verseCounts[n-1]),10,GOLD,true);box.addView(num);
            TextView name=txt(chapterNames[i],17,INK,true);name.setPadding(0,dp(5),0,dp(5));box.addView(name);
            TextView desc=txt(summaries[i],13,MUTED,false);desc.setLineSpacing(dp(2),1);box.addView(desc);
            card(box); box.setOnClickListener(v->showChapter(n));
        }
    }
    private void showChapter(int n){page="chapter";selectedChapter=n;setup("Chapter "+n,chapterNames[n-1]);
        TextView desc=txt(summaries[n-1],15,MUTED,false);desc.setLineSpacing(dp(3),1);card(desc);
        section("ALL SHLOKAS IN THIS CHAPTER  •  "+versesForChapter(n)+" AVAILABLE");
        boolean found=false;for(Verse v:verses)if(v.chapter==n){verseCard(v,false);found=true;}
        if(!found){TextView empty=txt("This chapter's full verse collection can be added in a future content update. Explore the chapter summary above and browse the selected teachings below.",14,MUTED,false);card(empty);}
        section("A MOMENT TO REFLECT");TextView reflect=txt("What can this teaching change in the way you think, speak or act today?",16,INK,true);card(reflect);
    }
    private int versesForChapter(int chapter) {
        int total=0; for(Verse v:verses) if(v.chapter==chapter) total++; return total;
    }
    private void showSaved(){page="saved";setup("Your saved verses","Keep the teachings you want to return to");
        ArrayList<Verse> list=new ArrayList<>();for(Verse v:verses)if(saved.contains(v.id()))list.add(v);
        if(list.isEmpty()){TextView e=txt("No saved verses yet. Tap the ♡ on any verse to keep it close.",15,MUTED,false);card(e);}
        else for(Verse v:list)verseCard(v,true);
    }
    private void verseCard(Verse v,boolean showMeaning){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView ref=txt("BHAGAVAD GITA  •  "+v.chapter+"."+v.number,10,GOLD,true);top.addView(ref,new LinearLayout.LayoutParams(0,-2,1));
        TextView save=txt(saved.contains(v.id())?"♥":"♡",24,saved.contains(v.id())?GOLD:MUTED,true);top.addView(save);
        save.setOnClickListener(w->{if(saved.contains(v.id()))saved.remove(v.id());else saved.add(v.id());getPreferences(0).edit().putStringSet("saved",new HashSet<>(saved)).apply();save.setText(saved.contains(v.id())?"♥":"♡");save.setTextColor(saved.contains(v.id())?GOLD:MUTED);});
        box.addView(top);
        TextView sans=txt(v.sanskrit,21,INK,true);sans.setGravity(Gravity.CENTER);sans.setPadding(0,dp(15),0,dp(12));sans.setLineSpacing(dp(5),1);box.addView(sans);
        TextView trans=txt(v.transliteration,12,MUTED,false);trans.setGravity(Gravity.CENTER);box.addView(trans);
        View line=new View(this);line.setBackgroundColor(0xFFEADCC7);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.setMargins(0,dp(14),0,dp(13));box.addView(line,lp);
        TextView meaning=txt(v.meaning,15,INK,false);meaning.setLineSpacing(dp(4),1);box.addView(meaning);
        if(showMeaning){TextView reflection=txt("REFLECTION",10,GOLD,true);reflection.setPadding(0,dp(14),0,dp(5));box.addView(reflection);TextView r=txt(v.reflection,13,MUTED,false);r.setLineSpacing(dp(3),1);box.addView(r);}
        card(box);
    }
}
