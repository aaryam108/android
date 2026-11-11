
package com.example.todo;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class MainActivity extends Activity {

    LinearLayout taskContainer;
    TextView progressPercent, progressLabel, taskCount;
    ProgressBar progressBar;
    ArrayList<Task> tasks = new ArrayList<>();
    SharedPreferences prefs;
    String filter = "All";

    static class Task {
        String title, category, priority;
        boolean done;

        Task(String t, String c, String p, boolean d) {
            title=t; category=c; priority=p; done=d;
        }
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        taskContainer=findViewById(R.id.taskContainer);
        progressPercent=findViewById(R.id.progressPercent);
        progressLabel=findViewById(R.id.progressLabel);
        taskCount=findViewById(R.id.taskCount);
        progressBar=findViewById(R.id.progressBar);

        prefs=getSharedPreferences("dayflow",MODE_PRIVATE);
        loadTasks();

        findViewById(R.id.addTaskButton)
                .setOnClickListener(v -> addTask());

        int[] ids={
                R.id.allFilter,R.id.personalFilter,
                R.id.workFilter,R.id.studyFilter,
                R.id.healthFilter
        };

        String[] categories={
                "All","Personal","Work","Study","Health"
        };

        for(int i=0;i<ids.length;i++){
            final String category=categories[i];
            findViewById(ids[i]).setOnClickListener(v->{
                filter=category;
                renderTasks();
            });
        }

        renderTasks();
    }

    void addTask(){

        EditText input=new EditText(this);
        input.setHint("Enter your task");

        Spinner category=new Spinner(this);
        String[] categories={
                "Personal","Work","Study","Health"
        };

        category.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
        ));

        Spinner priority=new Spinner(this);
        String[] priorities={"Normal","High","Low"};

        priority.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                priorities
        ));

        LinearLayout layout=new LinearLayout(this);
        layout.setPadding(25,10,25,10);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(input);
        layout.addView(category);
        layout.addView(priority);

        new AlertDialog.Builder(this)
                .setTitle("Create new task")
                .setView(layout)
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Add",(d,w)->{

                    String title=input.getText().toString().trim();

                    if(!title.isEmpty()){
                        tasks.add(0,new Task(
                                title,
                                category.getSelectedItem().toString(),
                                priority.getSelectedItem().toString(),
                                false
                        ));

                        saveTasks();
                        renderTasks();
                    }
                }).show();
    }

    void renderTasks(){

        taskContainer.removeAllViews();

        int completed=0;
        int visible=0;

        for(Task t:tasks){
            if(t.done) completed++;
        }

        for(int i=0;i<tasks.size();i++){

            final int index=i;
            Task t=tasks.get(i);

            if(!filter.equals("All") &&
                    !t.category.equals(filter)) continue;

            visible++;

            LinearLayout card=new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(12,12,12,12);

            GradientDrawable bg=new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(35);
            bg.setStroke(1,Color.LTGRAY);
            card.setBackground(bg);

            LinearLayout.LayoutParams cp=
                    new LinearLayout.LayoutParams(-1,-2);
            cp.bottomMargin=15;

            taskContainer.addView(card,cp);

            CheckBox check=new CheckBox(this);
            check.setChecked(t.done);
            check.setButtonTintList(
                    ColorStateList.valueOf(Color.rgb(113,139,120))
            );

            card.addView(check);

            LinearLayout details=new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.setPadding(10,0,5,0);

            card.addView(details,new LinearLayout.LayoutParams(
                    0,-2,1
            ));

            TextView name=new TextView(this);
            name.setText(t.title);
            name.setTextSize(16);
            name.setTextColor(Color.rgb(38,51,43));
            name.setTypeface(null,Typeface.BOLD);

            if(t.done){
                name.setPaintFlags(
                        name.getPaintFlags() |
                                android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                );
                name.setAlpha(0.5f);
            }

            details.addView(name);

            TextView meta=new TextView(this);
            meta.setText(t.category+" • "+t.priority+" priority");
            meta.setTextSize(12);
            meta.setTextColor(Color.GRAY);
            details.addView(meta);

            TextView more=new TextView(this);
            more.setText("⋮");
            more.setTextSize(25);
            more.setGravity(Gravity.CENTER);
            card.addView(more);

            check.setOnClickListener(v->{
                t.done=check.isChecked();
                saveTasks();
                renderTasks();
            });

            more.setOnClickListener(v->{
                new AlertDialog.Builder(this)
                        .setItems(
                                new String[]{"Edit","Delete"},
                                (dialog,choice)->{
                                    if(choice==0) editTask(index);
                                    else deleteTask(index);
                                }
                        ).show();
            });
        }

        if(visible==0){
            TextView empty=new TextView(this);
            empty.setText("✨\n\nNothing here yet!\nAdd a task to get started.");
            empty.setGravity(Gravity.CENTER);
            empty.setTextSize(16);
            empty.setPadding(10,80,10,80);
            taskContainer.addView(empty);
        }

        int percent=tasks.isEmpty()?0:
                completed*100/tasks.size();

        progressBar.setProgress(percent);
        progressPercent.setText(percent+"%");
        progressLabel.setText(
                completed+" of "+tasks.size()+" tasks completed"
        );

        taskCount.setText(tasks.size()+" tasks");
    }

    void editTask(int index){

        Task t=tasks.get(index);
        EditText input=new EditText(this);
        input.setText(t.title);

        new AlertDialog.Builder(this)
                .setTitle("Edit task")
                .setView(input)
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Save",(d,w)->{
                    String value=input.getText().toString().trim();
                    if(!value.isEmpty()){
                        t.title=value;
                        saveTasks();
                        renderTasks();
                    }
                }).show();
    }

    void deleteTask(int index){

        new AlertDialog.Builder(this)
                .setTitle("Delete task?")
                .setMessage("Are you sure you want to delete this task?")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Delete",(d,w)->{
                    tasks.remove(index);
                    saveTasks();
                    renderTasks();
                }).show();
    }

    void saveTasks(){

        JSONArray array=new JSONArray();

        try{
            for(Task t:tasks){
                JSONObject obj=new JSONObject();

                obj.put("title",t.title);
                obj.put("category",t.category);
                obj.put("priority",t.priority);
                obj.put("done",t.done);

                array.put(obj);
            }

            prefs.edit()
                    .putString("tasks",array.toString())
                    .apply();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    void loadTasks(){

        String data=prefs.getString("tasks",null);
        if(data==null) return;

        try{
            JSONArray array=new JSONArray(data);

            for(int i=0;i<array.length();i++){

                JSONObject obj=array.getJSONObject(i);

                tasks.add(new Task(
                        obj.getString("title"),
                        obj.getString("category"),
                        obj.getString("priority"),
                        obj.getBoolean("done")
                ));
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
