# Dukaan – Karyana Store Billing App

A starter Android billing app built with Java, native Android views and SQLite.

## Features
- Product inventory: add/edit product, selling price and fractional stock quantities
- Billing cart with fractional quantities (e.g. 0.5 kg or 1.25 litres)
- Automatic bill total
- Checkout saves the bill and bill items locally
- Stock decreases automatically after a completed sale
- Sales history and total sales summary
- Sample products are inserted on first launch

## Requirements
- Android Studio
- JDK 17
- Android SDK 35
- Minimum Android version: 6.0 (API 23)

## Open and run
1. Extract the ZIP.
2. In Android Studio, choose **Open** and select the `DukaanBilling` folder.
3. Let Gradle sync complete.
4. Run the `app` configuration on an emulator or Android phone.

## Notes
- All data is stored locally in the app's SQLite database.
- This is a starter version: it does not yet create GST-compliant tax invoices, print to a thermal printer, sync to cloud, or export reports.
- Before using it for real shop accounting, verify tax calculations and invoice requirements with an accountant.

## Fractional quantity support
- Add fractional quantities such as 0.5, 1.25 or 2.75 to a bill.
- Store fractional stock quantities.
- Product selling price is treated as the price per entered unit (for example, ₹60 per kg).
- The cart remove button removes the selected line; add the desired quantity again to change it.
- Existing SQLite databases can store fractional values in old INTEGER-affinity columns too; fresh installs create REAL quantity/stock columns.
