package com.example.booktracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int BG=Color.rgb(247,246,252), WHITE=Color.WHITE, INK=Color.rgb(35,34,49),
        MUTED=Color.rgb(126,124,143), PURPLE=Color.rgb(109,92,231),
        LAVENDER=Color.rgb(237,234,255), GREEN=Color.rgb(44,151,112),
        ORANGE=Color.rgb(220,139,65), BORDER=Color.rgb(231,229,239);
    private BookDb db;
    private LinearLayout root, list;
    private EditText searchBox;
    private String activeFilter="All";
    private String query="";
    private int page=0;

    static class Book {
        long id; String title, author, status, notes; int pages, current, rating;
        Book(long i,String t,String a,String s,int p,int c,int r,String n) {
            id=i;title=t;author=a;status=s;pages=p;current=c;rating=r;notes=n;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        db=new BookDb(this);
        showLibrary();
    }

    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color,int stroke,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke);return d;
    }
    private TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}

    private void setup(String title,String subtitle){
        root=vertical();root.setBackgroundColor(BG);
        LinearLayout header=vertical();header.setPadding(dp(22),dp(22),dp(22),dp(14));
        TextView brand=label("PAPERTRAIL  /  YOUR READING LIFE",10,PURPLE,true);
        TextView h=label(title,27,INK,true);h.setPadding(0,dp(6),0,dp(2));
        TextView sub=label(subtitle,13,MUTED,false);
        header.addView(brand);header.addView(h);header.addView(sub);
        root.addView(header,lp(-1,-2));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        list=vertical();list.setPadding(dp(18),dp(6),dp(18),dp(24));
        scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=horizontal();nav.setPadding(dp(14),dp(10),dp(14),dp(10));
        nav.setBackgroundColor(WHITE);
        Button library=navButton("▤  Library",page==0), stats=navButton("◷  Insights",page==1);
        library.setOnClickListener(v->{page=0;showLibrary();});
        stats.setOnClickListener(v->{page=1;showStats();});
        nav.addView(library,new LinearLayout.LayoutParams(0,dp(48),1));
        nav.addView(stats,new LinearLayout.LayoutParams(0,dp(48),1));
        root.addView(nav,lp(-1,68));setContentView(root);
    }

    private Button navButton(String s,boolean selected){
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(13);
        b.setTextColor(selected?PURPLE:MUTED);b.setBackground(bg(selected?LAVENDER:WHITE,0,14));return b;
    }
    private Button action(String s){
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(13);
        b.setTextColor(WHITE);b.setBackground(bg(PURPLE,0,14));return b;
    }
    private void showLibrary(){
        page=0;setup("My library","A little progress, one page at a time.");
        ArrayList<Book> all=db.getAll();
        int reading=0,done=0,want=0;
        for(Book b:all){if(b.status.equals("Reading"))reading++;else if(b.status.equals("Finished"))done++;else want++;}
        LinearLayout summary=horizontal();summary.setPadding(dp(16),dp(15),dp(16),dp(15));summary.setBackground(bg(INK,0,18));
        summary.addView(statCell("TOTAL",String.valueOf(all.size()),WHITE),new LinearLayout.LayoutParams(0,-2,1));
        summary.addView(statCell("READING",String.valueOf(reading),Color.rgb(202,190,255)),new LinearLayout.LayoutParams(0,-2,1));
        summary.addView(statCell("FINISHED",String.valueOf(done),Color.rgb(154,226,195)),new LinearLayout.LayoutParams(0,-2,1));
        list.addView(summary,lp(-1,-2));
        LinearLayout addRow=horizontal();addRow.setPadding(0,dp(15),0,dp(10));
        TextView heading=label("Your books",18,INK,true);addRow.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        Button add=action("+ Add book");add.setOnClickListener(v->bookDialog(null));addRow.addView(add,lp(118,46));
        list.addView(addRow);
        searchBox=new EditText(this);searchBox.setSingleLine(true);searchBox.setTextSize(14);
        searchBox.setHint("Search title or author");searchBox.setPadding(dp(15),0,dp(15),0);
        searchBox.setBackground(bg(WHITE,BORDER,14));searchBox.setText(query);
        list.addView(searchBox,lp(-1,48));
        searchBox.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();renderBooks();}
            public void afterTextChanged(Editable e){}
        });
        LinearLayout filters=horizontal();filters.setPadding(0,dp(12),0,dp(8));
        String[] names={"All","Reading","Want to read","Finished"};
        for(String f:names){Button b=new Button(this);b.setText(f);b.setAllCaps(false);b.setTextSize(11);
            boolean on=activeFilter.equals(f);b.setTextColor(on?WHITE:MUTED);b.setBackground(bg(on?PURPLE:WHITE,on?0:BORDER,30));
            LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(0,dp(40),1);fp.setMargins(dp(2),0,dp(2),0);
            filters.addView(b,fp);b.setOnClickListener(v->{activeFilter=f;showLibrary();});}
        list.addView(filters);
        LinearLayout bookList=vertical();bookList.setTag("bookList");list.addView(bookList);
        renderBooks();
    }
    private View statCell(String title,String value,int color){
        LinearLayout l=vertical();TextView a=label(title,9,Color.rgb(178,177,193),true);
        TextView b=label(value,23,color,true);b.setPadding(0,dp(3),0,0);l.addView(a);l.addView(b);return l;
    }
    private void renderBooks(){
        if(list==null)return;
        LinearLayout holder=(LinearLayout)list.findViewWithTag("bookList");if(holder==null)return;
        holder.removeAllViews();
        ArrayList<Book> books=db.getAll();int count=0;
        for(Book b:books){
            boolean filter=activeFilter.equals("All")||b.status.equals(activeFilter);
            String q=query.toLowerCase(Locale.ROOT);
            boolean match=b.title.toLowerCase(Locale.ROOT).contains(q)||b.author.toLowerCase(Locale.ROOT).contains(q);
            if(filter&&match){holder.addView(bookCard(b),lp(-1,-2));count++;}
        }
        if(count==0){
            LinearLayout empty=vertical();empty.setGravity(Gravity.CENTER);empty.setPadding(dp(20),dp(34),dp(20),dp(34));
            empty.setBackground(bg(WHITE,BORDER,18));
            TextView icon=label("✦",30,PURPLE,true);icon.setGravity(Gravity.CENTER);
            TextView msg=label(books.isEmpty()?"Your next favourite starts here.":"No books match this search.",14,MUTED,false);msg.setGravity(Gravity.CENTER);
            empty.addView(icon);empty.addView(msg);holder.addView(empty,lp(-1,-2));
        }
    }
    private View bookCard(Book b){
        LinearLayout card=vertical();card.setPadding(dp(16),dp(15),dp(16),dp(14));
        card.setBackground(bg(WHITE,BORDER,18));
        LinearLayout.LayoutParams cp=lp(-1,-2);cp.bottomMargin=dp(10);card.setLayoutParams(cp);
        LinearLayout top=horizontal();
        TextView cover=label("▤",23,PURPLE,true);cover.setGravity(Gravity.CENTER);cover.setBackground(bg(LAVENDER,0,12));
        top.addView(cover,lp(48,58));
        LinearLayout meta=vertical();meta.setPadding(dp(12),0,0,0);
        TextView title=label(b.title,16,INK,true);
        TextView author=label(b.author.isEmpty()?"Unknown author":b.author,12,MUTED,false);
        meta.addView(title);meta.addView(author);
        top.addView(meta,new LinearLayout.LayoutParams(0,-2,1));
        TextView more=label("•••",17,MUTED,true);more.setPadding(dp(8),dp(8),0,dp(8));
        more.setOnClickListener(v->options(b));top.addView(more);
        card.addView(top);
        LinearLayout row=horizontal();row.setPadding(0,dp(14),0,dp(8));
        TextView status=label("  "+b.status+"  ",10,statusColor(b.status),true);
        status.setBackground(bg(statusBg(b.status),0,20));row.addView(status);
        TextView pages=label(b.pages>0?(b.current+" / "+b.pages+" pages"):"No page goal",11,MUTED,false);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,-2,1);pp.leftMargin=dp(8);row.addView(pages,pp);
        if(b.rating>0)row.addView(label("★ "+b.rating+"/5",11,ORANGE,true));
        card.addView(row);
        if(b.pages>0){
            ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100);progress.setProgress(Math.min(100,(int)(100L*b.current/b.pages)));
            progress.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
            progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(LAVENDER));
            card.addView(progress,lp(-1,5));
        }
        card.setOnClickListener(v->bookDialog(b));return card;
    }
    private int statusColor(String s){return s.equals("Finished")?GREEN:s.equals("Reading")?PURPLE:ORANGE;}
    private int statusBg(String s){return s.equals("Finished")?Color.rgb(230,248,240):s.equals("Reading")?LAVENDER:Color.rgb(255,243,228);}

    private void options(Book b){
        String[] opts={"Edit book","Delete book"};
        new AlertDialog.Builder(this).setTitle(b.title).setItems(opts,(d,w)->{
            if(w==0)bookDialog(b);
            else new AlertDialog.Builder(this).setMessage("Delete “"+b.title+"” from your library?")
                .setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,y)->{db.delete(b.id);showLibrary();}).show();
        }).show();
    }
    private void bookDialog(Book old){
        LinearLayout form=vertical();form.setPadding(dp(20),dp(8),dp(20),dp(4));
        EditText title=field("Book title",old==null?"":old.title);
        EditText author=field("Author",old==null?"":old.author);
        EditText pages=field("Total pages",old==null?"":String.valueOf(old.pages));pages.setInputType(2);
        EditText current=field("Current page",old==null?"":String.valueOf(old.current));current.setInputType(2);
        EditText notes=field("Notes / favourite quote",old==null?"":old.notes);notes.setMinLines(2);notes.setGravity(Gravity.TOP);
        Spinner status=new Spinner(this);
        String[] states={"Want to read","Reading","Finished"};
        status.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,states));
        if(old!=null){for(int i=0;i<states.length;i++)if(states[i].equals(old.status))status.setSelection(i);}
        Spinner rating=new Spinner(this);
        String[] ratings={"Not rated","1 star","2 stars","3 stars","4 stars","5 stars"};
        rating.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,ratings));
        if(old!=null)rating.setSelection(old.rating);
        addField(form,"TITLE",title);addField(form,"AUTHOR",author);
        addField(form,"STATUS",status);addField(form,"TOTAL PAGES",pages);
        addField(form,"CURRENT PAGE",current);addField(form,"RATING",rating);addField(form,"NOTES",notes);
        ScrollView sc=new ScrollView(this);sc.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(old==null?"Add a book":"Edit book")
            .setView(sc).setNegativeButton("Cancel",null).setPositiveButton(old==null?"Add book":"Save",null).create();
        dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{
            String t=title.getText().toString().trim();
            if(t.isEmpty()){title.setError("Please enter a book title");return;}
            int p=parseInt(pages.getText().toString()),c=parseInt(current.getText().toString());
            if(p<0||c<0|| (p>0&&c>p)){current.setError("Enter a valid page number");return;}
            String st=states[status.getSelectedItemPosition()];
            if(st.equals("Finished")&&p>0)c=p;
            int r=rating.getSelectedItemPosition();
            Book b=new Book(old==null?-1:old.id,t,author.getText().toString().trim(),st,p,c,r,notes.getText().toString().trim());
            if(old==null)db.insert(b);else db.update(b);
            dialog.dismiss();showLibrary();
        }));
        dialog.show();
    }
    private EditText field(String hint,String value){
        EditText e=new EditText(this);e.setSingleLine(false);e.setTextSize(14);e.setHint(hint);e.setText(value);
        e.setPadding(dp(12),dp(8),dp(12),dp(8));e.setBackground(bg(BG,BORDER,12));return e;
    }
    private void addField(LinearLayout parent,String name,View v){
        TextView l=label(name,9,MUTED,true);l.setPadding(dp(2),dp(9),0,dp(4));parent.addView(l);
        parent.addView(v,lp(-1,v instanceof EditText?48:48));
    }
    private int parseInt(String s){try{return Integer.parseInt(s.trim());}catch(Exception e){return 0;}}

    private void showStats(){
        page=1;setup("Reading insights","Small steps add up to big stories.");
        ArrayList<Book> all=db.getAll();int total=all.size(),finished=0,reading=0,want=0,pagesRead=0;
        for(Book b:all){pagesRead+=b.current;if(b.status.equals("Finished"))finished++;else if(b.status.equals("Reading"))reading++;else want++;}
        LinearLayout hero=vertical();hero.setPadding(dp(20),dp(20),dp(20),dp(20));hero.setBackground(bg(INK,0,18));
        hero.addView(label("PAGES READ",10,Color.rgb(190,183,220),true));
        hero.addView(label(String.valueOf(pagesRead),40,WHITE,true));
        hero.addView(label("Every page is progress.",13,Color.rgb(190,183,220),false));
        list.addView(hero,lp(-1,-2));
        addInsight("Books in your library",String.valueOf(total),"All the stories you’re collecting");
        addInsight("Finished books",String.valueOf(finished),"Stories you’ve reached the end of");
        addInsight("Currently reading",String.valueOf(reading),"Books on your bedside table");
        addInsight("On your wishlist",String.valueOf(want),"Good reads for another day");
        Button back=action("← Back to my library");back.setOnClickListener(v->{page=0;showLibrary();});
        LinearLayout.LayoutParams bp=lp(-1,50);bp.topMargin=dp(12);list.addView(back,bp);
    }
    private void addInsight(String title,String value,String desc){
        LinearLayout row=horizontal();row.setPadding(dp(16),dp(14),dp(16),dp(14));row.setBackground(bg(WHITE,BORDER,16));
        LinearLayout text=vertical();text.addView(label(title,14,INK,true));text.addView(label(desc,11,MUTED,false));
        row.addView(text,new LinearLayout.LayoutParams(0,-2,1));row.addView(label(value,23,PURPLE,true));
        LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(10);list.addView(row,p);
    }

    static class BookDb extends SQLiteOpenHelper{
        BookDb(Context c){super(c,"papertrail.db",null,1);}
        public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE books(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,author TEXT,status TEXT,pages INTEGER,current_page INTEGER,rating INTEGER,notes TEXT)");}
        public void onUpgrade(SQLiteDatabase d,int a,int b){}
        void insert(Book b){getWritableDatabase().insert("books",null,values(b));}
        void update(Book b){getWritableDatabase().update("books",values(b),"id=?",new String[]{String.valueOf(b.id)});}
        void delete(long id){getWritableDatabase().delete("books","id=?",new String[]{String.valueOf(id)});}
        ContentValues values(Book b){ContentValues v=new ContentValues();v.put("title",b.title);v.put("author",b.author);v.put("status",b.status);v.put("pages",b.pages);v.put("current_page",b.current);v.put("rating",b.rating);v.put("notes",b.notes);return v;}
        ArrayList<Book> getAll(){
            ArrayList<Book> a=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT * FROM books ORDER BY id DESC",null);
            try{while(c.moveToNext())a.add(new Book(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4),c.getInt(5),c.getInt(6),c.getString(7)));}finally{c.close();}
            return a;
        }
    }
}
