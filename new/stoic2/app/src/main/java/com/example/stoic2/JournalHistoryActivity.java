
package com.example.stoic2;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class JournalHistoryActivity extends Activity {

    private SharedPreferences preferences;

    private ArrayList<String> dates;
    private ArrayList<String> displayDates;

    private ListView journalList;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_journal_history);

        preferences = getSharedPreferences(
                "stoic_companion", MODE_PRIVATE
        );

        journalList = findViewById(R.id.journalList);
        emptyText = findViewById(R.id.emptyText);

        loadJournalHistory();
    }

    // LOAD ALL SAVED JOURNAL ENTRIES

    private void loadJournalHistory() {

        Map<String, ?> entries = preferences.getAll();

        dates = new ArrayList<>();
        displayDates = new ArrayList<>();

        SimpleDateFormat storageFormat = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()
        );

        SimpleDateFormat displayFormat = new SimpleDateFormat(
                "EEEE, d MMMM yyyy", Locale.getDefault()
        );

        for (Map.Entry<String, ?> entry : entries.entrySet()) {

            if (entry.getValue() instanceof String) {

                String date = entry.getKey();

                try {

                    Date parsedDate = storageFormat.parse(date);

                    if (parsedDate != null) {

                        dates.add(date);

                        displayDates.add(
                                displayFormat.format(parsedDate)
                        );
                    }

                } catch (ParseException ignored) {
                    // Ignore unrelated preference keys.
                }
            }
        }

        // SORT NEWEST FIRST

        ArrayList<Integer> order = new ArrayList<>();

        for (int i = 0; i < dates.size(); i++) {
            order.add(i);
        }

        Collections.sort(order, (a, b) ->
                dates.get(b).compareTo(dates.get(a))
        );

        ArrayList<String> sortedDates = new ArrayList<>();
        ArrayList<String> sortedDisplayDates = new ArrayList<>();

        for (int index : order) {

            sortedDates.add(dates.get(index));
            sortedDisplayDates.add(displayDates.get(index));

        }

        dates = sortedDates;
        displayDates = sortedDisplayDates;

        // EMPTY STATE

        if (dates.isEmpty()) {

            journalList.setVisibility(ListView.GONE);
            emptyText.setVisibility(TextView.VISIBLE);

            return;
        }

        emptyText.setVisibility(TextView.GONE);
        journalList.setVisibility(ListView.VISIBLE);

        // DISPLAY JOURNAL DATES

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.item_journal_date,
                R.id.journalDateText,
                displayDates
        );

        journalList.setAdapter(adapter);

        // OPEN SELECTED ENTRY

        journalList.setOnItemClickListener(
                (parent, view, position, id) -> {

                    String selectedDate = dates.get(position);

                    String note = preferences.getString(
                            selectedDate, ""
                    );

                    new AlertDialog.Builder(this)
                            .setTitle(displayDates.get(position))
                            .setMessage(note)
                            .setPositiveButton("Close", null)
                            .show();
                }
        );
    }
}
