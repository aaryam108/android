package com.example.dukaanbilling;

import android.app.*;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.InputType;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final int GREEN = Color.rgb(28, 112, 78);
    private final int BG = Color.rgb(246, 248, 247);
    private DB db;
    private LinearLayout page, content, nav;
    private final LinkedHashMap<Integer, Double> cart = new LinkedHashMap<>();
    private String currentPage = "Billing";
    private TextView totalView;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new DB(this);
        showShell();
        showBilling();
    }

    private void showShell() {
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);
        setContentView(page);

        LinearLayout header = new LinearLayout(this);
        header.setPadding(20, 22, 20, 18);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(GREEN);
        header.setOrientation(LinearLayout.VERTICAL);
        TextView brand = new TextView(this);
        brand.setText("DUKAAN");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(23);
        brand.setTypeface(null, Typeface.BOLD);
        TextView sub = new TextView(this);
        sub.setText("Karyana Store Manager");
        sub.setTextColor(Color.rgb(220, 240, 230));
        sub.setTextSize(13);
        header.addView(brand);
        header.addView(sub);
        page.addView(header);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(16, 14, 16, 12);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        nav = new LinearLayout(this);
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(6, 8, 6, 8);
        page.addView(nav);
        addNav("Billing");
        addNav("Products");
        addNav("Sales");
    }

    private void addNav(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTextColor(GREEN);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        nav.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
        b.setOnClickListener(v -> {
            currentPage = label;
            if (label.equals("Billing")) showBilling();
            else if (label.equals("Products")) showProducts();
            else showSales();
        });
    }

    private void heading(String title, String subtitle) {
        content.removeAllViews();
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(22);
        t.setTextColor(Color.rgb(35, 45, 40));
        t.setTypeface(null, Typeface.BOLD);
        content.addView(t);
        TextView s = new TextView(this);
        s.setText(subtitle);
        s.setTextSize(13);
        s.setTextColor(Color.GRAY);
        s.setPadding(0, 3, 0, 14);
        content.addView(s);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(45, 52, 48));
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(14, 12, 14, 12);
        c.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 10;
        c.setLayoutParams(lp);
        return c;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN));
        return b;
    }

    private void showBilling() {
        currentPage = "Billing";
        heading("New bill", "Add products to the cart and checkout");
        EditText customer = new EditText(this);
        customer.setSingleLine(true);
        customer.setHint("Customer name (optional)");
        content.addView(customer);

        TextView productsTitle = text("Available products", 17, true);
        productsTitle.setPadding(0, 12, 0, 8);
        content.addView(productsTitle);

        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id,name,price,stock FROM products ORDER BY name", null);
        if (c.getCount() == 0) {
            content.addView(text("No products yet. Add products from the Products tab.", 14, false));
        }
        while (c.moveToNext()) {
            final int id = c.getInt(0);
            String name = c.getString(1);
            double price = c.getDouble(2);
            double stock = c.getDouble(3);
            LinearLayout row = card();
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.addView(text(name, 16, true));
            info.addView(text("₹" + money(price) + "  •  Stock: " + quantityFormat(stock), 13, false));
            line.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            Button add = button("ADD");
            add.setEnabled(stock > 0);
            add.setOnClickListener(v -> {
                EditText quantityInput = field("Quantity (e.g. 0.5)",
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                quantityInput.setText("1");
                new AlertDialog.Builder(this)
                    .setTitle("Add " + name)
                    .setView(quantityInput)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Add", (dialog, which) -> {
                        try {
                            double quantity = Double.parseDouble(
                                quantityInput.getText().toString().trim());
                            double already = cart.containsKey(id) ? cart.get(id) : 0.0;
                            if (!Double.isFinite(quantity) || quantity <= 0) {
                                toast("Enter a quantity greater than zero");
                            } else if (already + quantity > stock + 0.000001) {
                                toast("Not enough stock");
                            } else {
                                cart.put(id, already + quantity);
                                showBilling();
                            }
                        } catch (Exception ex) {
                            toast("Enter a valid quantity, e.g. 0.5");
                        }
                    }).show();
            });
            line.addView(add);
            row.addView(line);
            content.addView(row);
        }
        c.close();

        TextView cartTitle = text("Current cart", 17, true);
        cartTitle.setPadding(0, 8, 0, 8);
        content.addView(cartTitle);
        double total = 0;
        for (Map.Entry<Integer,Double> e : cart.entrySet()) {
            Cursor p = db.getReadableDatabase().rawQuery(
                "SELECT name,price FROM products WHERE id=?", new String[]{String.valueOf(e.getKey())});
            if (p.moveToFirst()) {
                String name = p.getString(0);
                double price = p.getDouble(1);
                total += price * e.getValue();
                LinearLayout row = card();
                LinearLayout line = new LinearLayout(this);
                line.setGravity(Gravity.CENTER_VERTICAL);
                line.addView(text(name + "  × " + quantityFormat(e.getValue()) + "\n₹" + money(price * e.getValue()), 14, true),
                    new LinearLayout.LayoutParams(0, -2, 1));
                Button minus = button("−");
                minus.setOnClickListener(v -> {
                    cart.remove(e.getKey());
                    showBilling();
                });
                line.addView(minus);
                row.addView(line);
                content.addView(row);
            }
            p.close();
        }
        LinearLayout summary = card();
        totalView = text("TOTAL  ₹" + money(total), 20, true);
        totalView.setTextColor(GREEN);
        summary.addView(totalView);
        Button checkout = button("Complete sale");
        checkout.setOnClickListener(v -> checkout(customer.getText().toString().trim()));
        summary.addView(checkout);
        Button clear = new Button(this);
        clear.setText("Clear cart");
        clear.setAllCaps(false);
        clear.setOnClickListener(v -> { cart.clear(); showBilling(); });
        summary.addView(clear);
        content.addView(summary);
    }

    private void checkout(String customer) {
        if (cart.isEmpty()) { toast("Cart is empty"); return; }
        SQLiteDatabase w = db.getWritableDatabase();
        double total = 0;
        for (Map.Entry<Integer,Double> e : cart.entrySet()) {
            Cursor p = w.rawQuery("SELECT price,stock,name FROM products WHERE id=?",
                new String[]{String.valueOf(e.getKey())});
            if (p.moveToFirst()) {
                if (p.getDouble(1) + 0.000001 < e.getValue()) {
                    toast("Insufficient stock for " + p.getString(2));
                    p.close(); return;
                }
                total += p.getDouble(0) * e.getValue();
            }
            p.close();
        }
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        android.content.ContentValues bill = new android.content.ContentValues();
        bill.put("customer", customer);
        bill.put("date", date);
        bill.put("total", total);
        long billId = w.insert("bills", null, bill);

        for (Map.Entry<Integer,Double> e : cart.entrySet()) {
            Cursor p = w.rawQuery("SELECT price FROM products WHERE id=?",
                new String[]{String.valueOf(e.getKey())});
            if (p.moveToFirst()) {
                double price = p.getDouble(0);
                android.content.ContentValues item = new android.content.ContentValues();
                item.put("bill_id", billId);
                item.put("product_id", e.getKey());
                item.put("quantity", e.getValue());
                item.put("price", price);
                w.insert("bill_items", null, item);
                w.execSQL("UPDATE products SET stock=stock-? WHERE id=?",
                    new Object[]{e.getValue(), e.getKey()});
            }
            p.close();
        }
        cart.clear();
        new AlertDialog.Builder(this)
            .setTitle("Sale completed")
            .setMessage("Bill #" + billId + "\nCustomer: " +
                (customer.isEmpty() ? "Walk-in customer" : customer) +
                "\nDate: " + date + "\n\nTOTAL: ₹" + money(total))
            .setPositiveButton("Done", (d, which) -> showBilling())
            .setNeutralButton("View sales", (d, which) -> showSales())
            .show();
    }

    private void showProducts() {
        currentPage = "Products";
        heading("Products & stock", "Manage the items available in your store");
        Button add = button("+ Add new product");
        add.setOnClickListener(v -> productDialog());
        content.addView(add);
        content.addView(new View(this), new LinearLayout.LayoutParams(1, 12));
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id,name,price,stock FROM products ORDER BY name", null);
        if (c.getCount() == 0) content.addView(text("Your product list is empty.", 14, false));
        while (c.moveToNext()) {
            int id = c.getInt(0);
            String name = c.getString(1);
            double price = c.getDouble(2);
            double stock = c.getDouble(3);
            LinearLayout row = card();
            row.addView(text(name, 16, true));
            row.addView(text("Selling price: ₹" + money(price) + "   |   Stock: " + quantityFormat(stock), 13, false));
            Button edit = button("Edit");
            edit.setOnClickListener(v -> productDialog(id, name, price, stock));
            row.addView(edit);
            content.addView(row);
        }
        c.close();
    }

    private void productDialog() { productDialog(-1, "", 0, 0); }

    private void productDialog(int id, String oldName, double oldPrice, double oldStock) {
        LinearLayout form = new LinearLayout(this);
        form.setPadding(20, 4, 20, 4);
        form.setOrientation(LinearLayout.VERTICAL);
        EditText name = field("Product name", InputType.TYPE_CLASS_TEXT);
        name.setText(oldName);
        EditText price = field("Selling price (₹)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (id != -1) price.setText(String.valueOf(oldPrice));
        EditText stock = field("Stock quantity (e.g. 12.5 kg)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (id != -1) stock.setText(String.valueOf(oldStock));
        form.addView(name); form.addView(price); form.addView(stock);
        new AlertDialog.Builder(this).setTitle(id == -1 ? "Add product" : "Edit product")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                try {
                    String n = name.getText().toString().trim();
                    double p = Double.parseDouble(price.getText().toString());
                    double q = Double.parseDouble(stock.getText().toString().trim());
                    if (n.isEmpty() || !Double.isFinite(p) || !Double.isFinite(q) || p < 0 || q < 0) throw new Exception();
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("name", n); values.put("price", p); values.put("stock", q);
                    if (id == -1) db.getWritableDatabase().insert("products", null, values);
                    else db.getWritableDatabase().update("products", values, "id=?", new String[]{String.valueOf(id)});
                    showProducts();
                } catch (Exception ex) { toast("Enter a valid name, price and stock"); }
            }).show();
    }

    private EditText field(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(type);
        return e;
    }

    private void showSales() {
        currentPage = "Sales";
        heading("Sales history", "Completed bills saved on this device");
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id,customer,date,total FROM bills ORDER BY id DESC", null);
        double sum = 0;
        int count = c.getCount();
        Cursor totals = db.getReadableDatabase().rawQuery("SELECT COALESCE(SUM(total),0) FROM bills", null);
        if (totals.moveToFirst()) sum = totals.getDouble(0);
        totals.close();
        LinearLayout stats = card();
        stats.addView(text("Total sales: ₹" + money(sum), 19, true));
        stats.addView(text("Bills generated: " + count, 14, false));
        content.addView(stats);
        if (count == 0) content.addView(text("No sales recorded yet.", 14, false));
        while (c.moveToNext()) {
            LinearLayout row = card();
            String customer = c.getString(1);
            row.addView(text("Bill #" + c.getInt(0) + "  •  ₹" + money(c.getDouble(3)), 16, true));
            row.addView(text((customer == null || customer.isEmpty() ? "Walk-in customer" : customer) +
                "\n" + c.getString(2), 13, false));
            content.addView(row);
        }
        c.close();
    }

    private String quantityFormat(double value) {
        return new java.text.DecimalFormat(
            "0.##",
            java.text.DecimalFormatSymbols.getInstance(Locale.US)
        ).format(value);
    }

    private String money(double value) {
        return String.format(Locale.getDefault(), "%.2f", value);
    }
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    @Override public void onBackPressed() {
        if (!cart.isEmpty()) {
            new AlertDialog.Builder(this).setMessage("Leave billing and clear the current cart?")
                .setNegativeButton("Stay", null)
                .setPositiveButton("Clear", (d,w) -> { cart.clear(); showBilling(); })
                .show();
        } else super.onBackPressed();
    }

    static class DB extends SQLiteOpenHelper {
        DB(Context c) { super(c, "dukaan.db", null, 1); }
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,price REAL NOT NULL,stock REAL NOT NULL)");
            db.execSQL("CREATE TABLE bills(id INTEGER PRIMARY KEY AUTOINCREMENT,customer TEXT,date TEXT,total REAL)");
            db.execSQL("CREATE TABLE bill_items(id INTEGER PRIMARY KEY AUTOINCREMENT,bill_id INTEGER,product_id INTEGER,quantity REAL,price REAL)");
            db.execSQL("INSERT INTO products(name,price,stock) VALUES('Rice (1 kg)',60,40)");
            db.execSQL("INSERT INTO products(name,price,stock) VALUES('Wheat flour (1 kg)',42,35)");
            db.execSQL("INSERT INTO products(name,price,stock) VALUES('Sugar (1 kg)',45,25)");
            db.execSQL("INSERT INTO products(name,price,stock) VALUES('Milk (1 litre)',60,20)");
            db.execSQL("INSERT INTO products(name,price,stock) VALUES('Cooking oil (1 litre)',145,15)");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
    }
}
