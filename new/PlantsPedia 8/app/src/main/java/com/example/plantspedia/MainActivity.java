package com.example.plantspedia;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Plants Pedia: a Java Android client for Wikipedia's public REST API. */
public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private LinearLayout content;
    private EditText searchBox;
    private ProgressBar progress;
    private String currentQuery = "plants";
    private int green = Color.rgb(35, 105, 62);
    private int darkGreen = Color.rgb(18, 61, 42);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(darkGreen);
        getWindow().setNavigationBarColor(darkGreen);
        buildHome();
        searchPlants("plants");
    }

    private void buildHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(247, 250, 246));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(18), dp(20), dp(18));
        header.setBackgroundColor(darkGreen);
        TextView title = text("Plants Pedia", 27, Color.WHITE, true);
        TextView subtitle = text("Explore the world of plants", 14, Color.rgb(220, 239, 222), false);
        header.addView(title);
        header.addView(subtitle);
        root.addView(header);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(dp(14), dp(12), dp(14), dp(8));
        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(16);
        searchBox.setHint("Search plant name (e.g. Neem, Rose)");
        searchBox.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchBox.setPadding(dp(14), 0, dp(10), 0);
        searchBox.setBackground(roundBg(Color.WHITE, 12, Color.rgb(220, 228, 219)));
        searchRow.addView(searchBox, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button search = new Button(this);
        search.setText("Search");
        search.setTextColor(Color.WHITE);
        search.setBackground(roundBg(green, 12, green));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(94), dp(48));
        bp.leftMargin = dp(8);
        searchRow.addView(search, bp);
        root.addView(searchRow);
        search.setOnClickListener(v -> submitSearch());
        searchBox.setOnEditorActionListener((v, action, event) -> { submitSearch(); return true; });

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(36), dp(36));
        pp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(progress, pp);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(8), dp(14), dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        TextView intro = text("Discover plants", 20, darkGreen, true);
        intro.setPadding(dp(4), dp(8), 0, dp(8));
        content.addView(intro);
        addMessage("Search Wikipedia for plant profiles, photographs, descriptions and scientific details.");
    }

    private void submitSearch() {
        String q = searchBox.getText().toString().trim();
        if (!q.isEmpty()) searchPlants(q);
    }

    private void searchPlants(String query) {
        currentQuery = query;
        progress.setVisibility(View.VISIBLE);
        content.removeAllViews();
        addMessage("Searching Wikipedia for “" + query + "”…");
        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20");
                JSONObject json = getJson("https://en.wikipedia.org/w/rest.php/v1/search/page?q=" + encoded + "&limit=50");
                JSONArray pages = json.optJSONArray("pages");
                mainHandler.post(() -> {
                    progress.setVisibility(View.GONE);
                    content.removeAllViews();
                    TextView heading = text("Results for “" + query + "”", 20, darkGreen, true);
                    heading.setPadding(dp(4), dp(8), 0, dp(10));
                    content.addView(heading);
                    if (pages == null || pages.length() == 0) {
                        addMessage("No plant articles found. Try a plant name such as Neem, Rose, Mango or Fern.");
                        return;
                    }

                    int shown = 0;
                    for (int i = 0; i < pages.length(); i++) {
                        JSONObject p = pages.optJSONObject(i);
                        if (p != null && isPlantRelated(p)) {
                            addPlantCard(p);
                            shown++;
                        }
                    }
                    if (shown == 0) {
                        addMessage("No plant-related articles matched “" + query + "”. Try a botanical name, common plant name, tree, flower or herb.");
                    } else {
                        addAttribution();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    progress.setVisibility(View.GONE);
                    content.removeAllViews();
                    addMessage("Could not load Wikipedia results. Check your internet connection and try again.");
                    addRetryButton(query);
                });
            }
        });
    }

    /**
     * Keep search results focused on botanical subjects. Wikipedia's general
     * search can return people, places, products and other unrelated pages,
     * so each candidate is checked using its article description and excerpt.
     */
    private boolean isPlantRelated(JSONObject page) {
        String title = page.optString("title", "");
        String description = page.optString("description", "");
        String excerpt = android.text.Html.fromHtml(
                page.optString("excerpt", ""),
                android.text.Html.FROM_HTML_MODE_COMPACT).toString();

        String titleText = title.toLowerCase(java.util.Locale.ROOT);
        String descriptionText = description.toLowerCase(java.util.Locale.ROOT);
        String excerptText = excerpt.toLowerCase(java.util.Locale.ROOT);
        String combined = (titleText + " " + descriptionText + " " + excerptText)
                .replace('’', '\'');

        // Avoid non-botanical uses of the word "plant" (factories, power stations, etc.).
        if (combined.matches("(?s).*(power plant|nuclear power|industrial plant|manufacturing plant|plant operator|plant maintenance|plant equipment|plant machinery|plant hire|plant-based business).*")) {
            return false;
        }

        // Require an explicit botanical clue in the article description or excerpt.
        // This is intentionally stricter than checking the title alone, which helps
        // prevent people, places, companies and products from appearing as results.
        String botanicalPattern = "(?s).*(\\b(plant|plants|tree|trees|shrub|shrubs|flowering plant|flowering plants|herb|herbs|grass|grasses|fern|ferns|moss|mosses|alga|algae|succulent|succulents|cactus|cacti|vine|vines|orchid|orchids|crop|crops|cultivar|cultivars|botanical|botany|flora|angiosperm|gymnosperm|bryophyte|pteridophyte)\\b|species of (flowering )?plant|genus of plants|family of plants|member of the plant kingdom|plant species|plant genus).*";
        return descriptionText.matches(botanicalPattern) || excerptText.matches(botanicalPattern);
    }

    private void addPlantCard(JSONObject p) {
        String key = p.optString("key", p.optString("title"));
        String title = p.optString("title", key);
        String description = p.optString("description", "Plant article on Wikipedia");
        String excerpt = android.text.Html.fromHtml(p.optString("excerpt", ""), android.text.Html.FROM_HTML_MODE_COMPACT).toString();
        String thumb = "";
        JSONObject thumbnail = p.optJSONObject("thumbnail");
        if (thumbnail != null) thumb = thumbnail.optString("url", "");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(roundBg(Color.WHITE, 14, Color.rgb(228, 235, 226)));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.bottomMargin = dp(10);
        content.addView(card, cp);

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(roundBg(Color.rgb(230, 239, 229), 10, Color.rgb(230, 239, 229)));
        card.addView(image, new LinearLayout.LayoutParams(dp(94), dp(104)));
        if (!thumb.isEmpty()) loadImage(thumb, image);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        info.addView(text(title, 17, darkGreen, true));
        TextView desc = text(description, 12, green, true);
        desc.setPadding(0, dp(3), 0, dp(4));
        info.addView(desc);
        TextView ex = text(excerpt, 13, Color.rgb(78, 87, 78), false);
        ex.setMaxLines(3);
        ex.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(ex);
        card.setOnClickListener(v -> showPlantDetails(key, title));
    }

    private void showPlantDetails(String key, String title) {
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(key.replace(' ', '_'), "UTF-8").replace("+", "%20");
                JSONObject data = getJson("https://en.wikipedia.org/api/rest_v1/page/summary/" + encoded);
                mainHandler.post(() -> {
                    progress.setVisibility(View.GONE);
                    content.removeAllViews();
                    Button back = new Button(this);
                    back.setText("← Back to results");
                    back.setOnClickListener(v -> searchPlants(currentQuery));
                    content.addView(back);
                    String displayTitle = data.optString("title", title);
                    content.addView(text(displayTitle, 25, darkGreen, true));
                    JSONObject original = data.optJSONObject("originalimage");
                    JSONObject thumb = data.optJSONObject("thumbnail");
                    String imageUrl = original != null ? original.optString("source", "") : (thumb != null ? thumb.optString("source", "") : "");
                    if (!imageUrl.isEmpty()) {
                        ImageView hero = new ImageView(this);
                        hero.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, dp(220));
                        hp.topMargin = dp(12); hp.bottomMargin = dp(12);
                        content.addView(hero, hp);
                        loadImage(imageUrl, hero);
                    }
                    String desc = data.optString("description", "");
                    if (!desc.isEmpty()) content.addView(text(desc, 15, green, true));
                    String extract = data.optString("extract", "No summary is available for this article.");
                    TextView body = text(extract, 15, Color.rgb(54, 62, 54), false);
                    body.setPadding(0, dp(12), 0, dp(16));
                    content.addView(body);
                    JSONObject contentUrls = data.optJSONObject("content_urls");
                    JSONObject desktop = contentUrls == null ? null : contentUrls.optJSONObject("desktop");
                    String pageUrl = desktop == null ? "https://en.wikipedia.org/wiki/" + encoded : desktop.optString("page", "https://en.wikipedia.org/wiki/" + encoded);
                    Button open = new Button(this);
                    open.setText("Read full article on Wikipedia");
                    open.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl))));
                    content.addView(open);
                    addAttribution();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    progress.setVisibility(View.GONE);
                    addMessage("Unable to load this plant article. Please try again.");
                });
            }
        });
    }

    private JSONObject getJson(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(12000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "PlantsPediaAndroid/1.0 (educational plant encyclopedia)");
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        } finally { c.disconnect(); }
    }

    private void loadImage(String address, ImageView target) {
        executor.execute(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(address.startsWith("//") ? "https:" + address : address).openConnection();
                c.setConnectTimeout(10000); c.setReadTimeout(15000);
                Bitmap bitmap = BitmapFactory.decodeStream(c.getInputStream());
                if (bitmap != null) mainHandler.post(() -> target.setImageBitmap(bitmap));
            } catch (Exception ignored) { }
            finally { if (c != null) c.disconnect(); }
        });
    }

    private void addMessage(String message) {
        TextView t = text(message, 14, Color.DKGRAY, false);
        t.setPadding(dp(12), dp(12), dp(12), dp(12));
        t.setBackground(roundBg(Color.WHITE, 12, Color.rgb(230, 236, 228)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(10);
        content.addView(t, lp);
    }

    private void addRetryButton(String query) {
        Button retry = new Button(this); retry.setText("Try again");
        retry.setOnClickListener(v -> searchPlants(query)); content.addView(retry);
    }

    private void addAttribution() {
        TextView t = text("Plant information and images are retrieved from Wikipedia. Article text is available under the Creative Commons Attribution-ShareAlike License; individual images may have separate licenses. Check each file's Wikimedia Commons page for attribution and reuse terms.", 11, Color.GRAY, false);
        t.setPadding(dp(6), dp(18), dp(6), dp(10));
        content.addView(t);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable roundBg(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke); return d;
    }
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onDestroy() { super.onDestroy(); executor.shutdownNow(); }
}
