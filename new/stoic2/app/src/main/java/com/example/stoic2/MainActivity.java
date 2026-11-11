
package com.example.stoic2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private EditText journalInput;
    private SharedPreferences preferences;
    private String todayKey;

    // Quotes loaded from stoicQuotes.txt
    private final List<String[]> stoicQuotes = new ArrayList<>();

    // Daily reflection prompts
    private final String[] prompts = {
            "What is within my control today?",
            "Where can I practise patience?",
            "What can I improve about myself?",
            "Did I act with wisdom today?",
            "What should I let go of?",
            "How can I become stronger?",
            "Did I live according to my values?"
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(
                "stoic_companion",
                MODE_PRIVATE
        );

        TextView dateText = findViewById(R.id.dateText);
        TextView quoteText = findViewById(R.id.quoteText);
        TextView authorText = findViewById(R.id.authorText);
        TextView promptText = findViewById(R.id.promptText);

        journalInput = findViewById(R.id.journalInput);

        Button saveButton = findViewById(R.id.saveButton);
        Button historyButton = findViewById(R.id.historyButton);

        SimpleDateFormat displayFormat = new SimpleDateFormat(
                "EEEE, d MMMM yyyy",
                Locale.getDefault()
        );

        SimpleDateFormat storageFormat = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        );

        Date currentDate = new Date();

        dateText.setText(displayFormat.format(currentDate));
        todayKey = storageFormat.format(currentDate);

        // Load quotes from TXT file
        loadStoicQuotes();

        // Display daily quote
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);


        if (!stoicQuotes.isEmpty()) {

            int index = (day - 1) % stoicQuotes.size();

            String[] quote = stoicQuotes.get(index);

            if (quote != null && quote.length >= 2) {

                quoteText.setText(quote[0].trim());
                authorText.setText("| " + quote[1].trim());

            } else {

                quoteText.setText("No quote available.");
                authorText.setText("");
            }

        }

        // Display daily reflection prompt
        promptText.setText(
                prompts[(day - 1) % prompts.length]
        );

        // Migrate old journal entries
        migrateOldEntries(displayFormat, storageFormat);

        // Load today's saved journal entry
        journalInput.setText(
                preferences.getString(todayKey, "")
        );

        // SAVE REFLECTION
        saveButton.setOnClickListener(v -> {

            String reflection = journalInput.getText()
                    .toString()
                    .trim();

            if (reflection.isEmpty()) {

                Toast.makeText(
                        MainActivity.this,
                        "Write a reflection first.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            preferences.edit()
                    .putString(todayKey, reflection)
                    .apply();

            Toast.makeText(
                    MainActivity.this,
                    "Reflection saved successfully!",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // OPEN JOURNAL HISTORY
        historyButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    JournalHistoryActivity.class
            );

            startActivity(intent);
        });
    }

    // LOAD QUOTES FROM assets/stoicQuotes.txt
    private void loadStoicQuotes() {

        stoicQuotes.clear();

        try (
                InputStream inputStream =
                        getAssets().open("stoicQuotes.txt");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                inputStream,
                                StandardCharsets.UTF_8
                        )
                )
        ) {

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\|", 2);

                if (parts.length == 2) {

                    String quote = parts[0].trim();
                    String author = parts[1].trim();

                    if (!quote.isEmpty() && !author.isEmpty()) {

                        stoicQuotes.add(
                                new String[]{quote, author}
                        );
                    }
                }
            }

        } catch (IOException e) {

            Toast.makeText(
                    this,
                    "Unable to load stoicQuotes.txt",
                    Toast.LENGTH_LONG
            ).show();

            e.printStackTrace();
        }
    }

    // MIGRATE PREVIOUSLY SAVED JOURNAL ENTRIES
    private void migrateOldEntries(
            SimpleDateFormat displayFormat,
            SimpleDateFormat storageFormat
    ) {

        displayFormat.setLenient(false);

        Map<String, ?> entries = preferences.getAll();

        SharedPreferences.Editor editor = preferences.edit();
        boolean changed = false;

        for (Map.Entry<String, ?> entry : entries.entrySet()) {

            String key = entry.getKey();
            Object value = entry.getValue();

            if (!(value instanceof String)) {
                continue;
            }

            try {

                Date parsedDate = displayFormat.parse(key);

                if (parsedDate != null) {

                    String newKey = storageFormat.format(parsedDate);

                    if (!preferences.contains(newKey)) {
                        editor.putString(newKey, (String) value);
                    }

                    editor.remove(key);
                    changed = true;
                }

            } catch (ParseException ignored) {
                // Entry already uses the new date format.
            }
        }

        if (changed) {
            editor.apply();
        }
    }
}
