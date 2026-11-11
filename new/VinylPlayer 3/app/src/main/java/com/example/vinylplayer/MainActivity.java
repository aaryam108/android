package com.example.vinylplayer;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private PlaybackService service;
    private boolean bound=false, dragging=false;
    private VinylView vinyl;
    private TextView title, artist, elapsed, duration, status, queueCount;
    private SeekBar seek, volume;
    private Button play;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean repeat=false, shuffle=false;
    private final ArrayList<Uri> tracks=new ArrayList<>();
    private final Runnable ticker=new Runnable(){@Override public void run(){updateUi();handler.postDelayed(this,350);}};

    private final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName n,IBinder b){service=((PlaybackService.LocalBinder)b).getService();bound=true; if(!tracks.isEmpty()&&service.getQueueSize()==0)service.setQueue(tracks); updateUi();}
        public void onServiceDisconnected(ComponentName n){bound=false;service=null;}
    };

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(16,17,20));getWindow().setNavigationBarColor(Color.rgb(16,17,20));buildUi();
        Intent s=new Intent(this,PlaybackService.class);startService(s);bindService(s,connection,BIND_AUTO_CREATE);
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},8);
        handler.post(ticker);
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER);return l;}
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(16,17,20));setContentView(scroll);
        LinearLayout page=column();page.setPadding(dp(22),dp(20),dp(22),dp(24));scroll.addView(page);
        LinearLayout top=row();top.setGravity(Gravity.CENTER_VERTICAL);page.addView(top);
        TextView brand=text("◉  NEEDLE",13,0xFFE8A45B);brand.setTypeface(null,Typeface.BOLD);top.addView(brand,new LinearLayout.LayoutParams(0,dp(34),1));
        Button add=button("+  ADD MUSIC",0xFF292A2F);top.addView(add);add.setOnClickListener(v->pickMusic());
        TextView heading=text("Now spinning",27,Color.WHITE);heading.setTypeface(null,Typeface.BOLD);heading.setPadding(0,dp(22),0,dp(3));page.addView(heading);
        status=text("YOUR PERSONAL RECORD COLLECTION",10,0xFF92939A);page.addView(status);
        FrameLayout recordFrame=new FrameLayout(this);LinearLayout.LayoutParams rf=new LinearLayout.LayoutParams(-1,dp(300));rf.topMargin=dp(16);rf.bottomMargin=dp(14);page.addView(recordFrame,rf);
        vinyl=new VinylView(this);recordFrame.addView(vinyl,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout info=column();info.setGravity(Gravity.CENTER);page.addView(info);
        title=text("Choose a track",21,Color.WHITE);title.setTypeface(null,Typeface.BOLD);title.setGravity(Gravity.CENTER);info.addView(title);
        artist=text("Add music from your device to begin",13,0xFF9B9CA3);artist.setGravity(Gravity.CENTER);artist.setPadding(0,dp(5),0,dp(12));info.addView(artist);
        seek=new SeekBar(this);seek.setMax(1000);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFE8A45B));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFFE8A45B));page.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){dragging=true;}public void onStopTrackingTouch(SeekBar s){dragging=false;if(service!=null&&service.getDuration()>0)service.seekTo((int)(s.getProgress()/1000f*service.getDuration()));}public void onProgressChanged(SeekBar s,int p,boolean u){}});
        LinearLayout times=row();times.setGravity(Gravity.CENTER_VERTICAL);elapsed=text("0:00",11,0xFF9B9CA3);duration=text("0:00",11,0xFF9B9CA3);times.addView(elapsed,new LinearLayout.LayoutParams(0,dp(22),1));times.addView(duration);page.addView(times);
        LinearLayout controls=row();controls.setPadding(0,dp(15),0,dp(16));page.addView(controls);
        Button sh=button("⇄",0xFF24252A);controls.addView(sh,weight());sh.setOnClickListener(v->{shuffle=!shuffle;if(service!=null)service.setShuffle(shuffle);sh.setTextColor(shuffle?0xFFE8A45B:Color.WHITE);});
        Button prev=button("|◀",0xFF24252A);controls.addView(prev,weight());prev.setOnClickListener(v->{if(service!=null)service.previous();});
        play=button("▶",0xFFE8A45B);play.setTextColor(0xFF171311);play.setTextSize(23);controls.addView(play,new LinearLayout.LayoutParams(dp(72),dp(58)));play.setOnClickListener(v->{if(service!=null){if(service.getQueueSize()==0)pickMusic();else service.toggle();}});
        Button next=button("▶|",0xFF24252A);controls.addView(next,weight());next.setOnClickListener(v->{if(service!=null)service.next();});
        Button rep=button("↻",0xFF24252A);controls.addView(rep,weight());rep.setOnClickListener(v->{repeat=!repeat;if(service!=null)service.setRepeat(repeat);rep.setTextColor(repeat?0xFFE8A45B:Color.WHITE);});
        LinearLayout volrow=row();volrow.setGravity(Gravity.CENTER_VERTICAL);TextView volLabel=text("VOL",10,0xFF9B9CA3);volrow.addView(volLabel);volume=new SeekBar(this);volume.setMax(100);volume.setProgress(75);volume.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFE8A45B));LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(0,dp(30),1);vp.leftMargin=dp(12);volrow.addView(volume,vp);page.addView(volrow);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){if(service!=null)service.setVolume(p/100f);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        LinearLayout library=row();library.setPadding(0,dp(24),0,dp(8));library.setGravity(Gravity.CENTER_VERTICAL);TextView lib=text("YOUR QUEUE",12,0xFFE8A45B);lib.setTypeface(null,Typeface.BOLD);library.addView(lib,new LinearLayout.LayoutParams(0,dp(30),1));queueCount=text("0 TRACKS",10,0xFF92939A);library.addView(queueCount);page.addView(library);
        Button open=button("＋  Browse audio files",0xFF25262B);open.setTextColor(Color.WHITE);page.addView(open,new LinearLayout.LayoutParams(-1,dp(48)));open.setOnClickListener(v->pickMusic());
        TextView foot=text("Tip: Select multiple audio files to build your listening queue.",11,0xFF777981);foot.setPadding(0,dp(12),0,0);page.addView(foot);
    }
    private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(54),1);p.setMargins(dp(3),0,dp(3),0);return p;}
    private Button button(String label,int bg){Button b=new Button(this);b.setText(label);b.setTextSize(13);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bg));return b;}
    private void pickMusic(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("audio/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,42);}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==42&&c==RESULT_OK&&d!=null){ArrayList<Uri> chosen=new ArrayList<>();if(d.getClipData()!=null){for(int x=0;x<d.getClipData().getItemCount();x++)chosen.add(d.getClipData().getItemAt(x).getUri());}else if(d.getData()!=null)chosen.add(d.getData());for(Uri u:chosen){try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}if(!tracks.contains(u))tracks.add(u);}if(service!=null)service.setQueue(tracks);updateUi();}}
    private String fmt(int ms){int s=Math.max(0,ms)/1000;return String.format(Locale.getDefault(),"%d:%02d",s/60,s%60);}
    private void updateUi(){
        if(!bound||service==null)return;
        boolean playing=service.isPlaying();vinyl.setSpinning(playing);vinyl.advance();play.setText(playing?"Ⅱ":"▶");
        title.setText(service.getTitle());artist.setText("TRACK "+(service.getQueueSize()==0?0:service.getIndex()+1)+"  •  LOCAL LIBRARY");
        int dur=service.getDuration(),pos=service.getPosition();if(!dragging){seek.setProgress(dur>0?(int)(pos*1000f/dur):0);}
        elapsed.setText(fmt(pos));duration.setText(fmt(dur));queueCount.setText(service.getQueueSize()+" TRACKS");
        status.setText(playing?"NOW PLAYING • 33⅓ RPM":"READY WHEN YOU ARE • 33⅓ RPM");
    }
    @Override protected void onDestroy(){handler.removeCallbacks(ticker);if(bound){unbindService(connection);bound=false;}super.onDestroy();}
}
