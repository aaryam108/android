package com.example.vinylplayer;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import java.util.*;

public class PlaybackService extends Service {
    public static final String ACTION_PLAY="com.example.vinylplayer.PLAY";
    public static final String ACTION_PAUSE="com.example.vinylplayer.PAUSE";
    public static final String ACTION_NEXT="com.example.vinylplayer.NEXT";
    public static final String ACTION_PREV="com.example.vinylplayer.PREV";
    private static final String CHANNEL="vinyl_playback";
    private final IBinder binder = new LocalBinder();
    private MediaPlayer player;
    private ArrayList<Uri> queue = new ArrayList<>();
    private int index=0;
    private boolean prepared=false;
    private boolean repeat=false;
    private boolean shuffle=false;
    private final Random random=new Random();
    private String title="Nothing playing";

    public class LocalBinder extends Binder { PlaybackService getService(){ return PlaybackService.this; } }
    @Override public IBinder onBind(Intent intent){ return binder; }

    @Override public void onCreate(){ super.onCreate(); createChannel(); }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null){
            String a=intent.getAction();
            if("SET_QUEUE".equals(a)){
                ArrayList<Uri> list=intent.getParcelableArrayListExtra("queue");
                if(list!=null && !list.isEmpty()){ queue=list; index=0; loadCurrent(true); }
            } else if(ACTION_PLAY.equals(a)) play();
            else if(ACTION_PAUSE.equals(a)) pause();
            else if(ACTION_NEXT.equals(a)) next();
            else if(ACTION_PREV.equals(a)) previous();
        }
        return START_NOT_STICKY;
    }

    public void setQueue(ArrayList<Uri> list){ if(list!=null&&!list.isEmpty()){queue=new ArrayList<>(list);index=0;loadCurrent(true);} }
    public int getDuration(){ return prepared&&player!=null?player.getDuration():0; }
    public int getPosition(){ return prepared&&player!=null?player.getCurrentPosition():0; }
    public boolean isPlaying(){ return player!=null&&prepared&&player.isPlaying(); }
    public String getTitle(){ return title; }
    public int getIndex(){ return index; }
    public int getQueueSize(){ return queue.size(); }
    public Uri getCurrentUri(){ return queue.isEmpty()?null:queue.get(index); }
    public void seekTo(int ms){ if(prepared&&player!=null) player.seekTo(ms); }
    public void setRepeat(boolean v){repeat=v;}
    public boolean isRepeat(){return repeat;}
    public void setShuffle(boolean v){shuffle=v;}
    public boolean isShuffle(){return shuffle;}
    public void setVolume(float v){if(player!=null)player.setVolume(v,v);}
    public void toggle(){if(isPlaying())pause();else play();}
    public void play(){if(prepared&&player!=null){player.start();startForeground(1,notification());}}
    public void pause(){if(player!=null&&prepared&&player.isPlaying())player.pause(); if(player!=null) { if(Build.VERSION.SDK_INT>=24) stopForeground(STOP_FOREGROUND_DETACH); else stopForeground(false); NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE); nm.notify(1,notification());}}
    public void next(){if(queue.size()<1)return; if(shuffle&&queue.size()>1){int n=index;while(n==index)n=random.nextInt(queue.size());index=n;}else index=(index+1)%queue.size();loadCurrent(true);}
    public void previous(){if(queue.size()<1)return; if(getPosition()>3000){seekTo(0);return;} index=(index-1+queue.size())%queue.size();loadCurrent(true);}

    private void loadCurrent(boolean autoPlay){
        releasePlayer(); if(queue.isEmpty())return;
        try{
            title=queryTitle(queue.get(index));
            player=new MediaPlayer(); player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setDataSource(this,queue.get(index));
            player.setOnPreparedListener(mp->{prepared=true; if(autoPlay)mp.start(); startForeground(1,notification());});
            player.setOnCompletionListener(mp->{if(repeat){seekTo(0);play();}else if(index<queue.size()-1||shuffle)next();else pause();});
            player.setOnErrorListener((mp,w,e)->{prepared=false;return true;});
            player.prepareAsync();
        }catch(Exception e){stopForeground(true);}
    }
    private String queryTitle(Uri uri){
        android.database.Cursor c=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null);
        if(c!=null){try{if(c.moveToFirst()){String n=c.getString(0);if(n!=null)return n.replaceFirst("\\.[^.]+$","");}}finally{c.close();}}
        return "Unknown track";
    }
    private void releasePlayer(){prepared=false;if(player!=null){try{player.release();}catch(Exception ignored){}player=null;}}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,"Music playback",NotificationManager.IMPORTANCE_LOW);ch.setDescription("Playback controls");getSystemService(NotificationManager.class).createNotificationChannel(ch);}}
    private PendingIntent action(String action,int request){Intent i=new Intent(this,PlaybackService.class).setAction(action);return PendingIntent.getService(this,request,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private Notification notification(){
        Intent open=new Intent(this,MainActivity.class); PendingIntent content=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setContentTitle(title).setContentText("Vinyl Player").setSmallIcon(android.R.drawable.ic_media_play).setContentIntent(content).setOngoing(isPlaying())
        .addAction(android.R.drawable.ic_media_previous,"Previous",action(ACTION_PREV,2))
        .addAction(isPlaying()?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,isPlaying()?"Pause":"Play",action(isPlaying()?ACTION_PAUSE:ACTION_PLAY,3))
        .addAction(android.R.drawable.ic_media_next,"Next",action(ACTION_NEXT,4));
        return b.build();
    }
    @Override public void onDestroy(){releasePlayer();stopForeground(true);super.onDestroy();}
}
