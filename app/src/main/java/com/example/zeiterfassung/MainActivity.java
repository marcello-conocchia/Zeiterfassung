package com.example.zeiterfassung;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer; // korrekter Import
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private int loggedInUserId;
    private String loggedInUserName;
    private WorkTimeEntry currentEntry = null;
    private Button btnStart, btnPause, btnStop, btnShowEntries, btnGeneratePdf, btnLogout;
    private ListView lvEntries;
    private NfcAdapter nfcAdapter;
    private Chronometer chronometer;
    private TextView tvHeader;
    private boolean isPaused = false;
    private long pauseStartTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hole Benutzerdaten aus dem Intent
        loggedInUserId = getIntent().getIntExtra("userId", -1);
        loggedInUserName = getIntent().getStringExtra("userName");

        if (loggedInUserId == -1) {
            Toast.makeText(this, "Benutzer nicht gefunden!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Datenbank instanziieren (nur für Entwicklungszwecke synchron)
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "worktime-db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();

        // UI-Elemente initialisieren
        tvHeader = findViewById(R.id.tvHeader);
        btnStart = findViewById(R.id.btnStart);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btnShowEntries = findViewById(R.id.btnShowEntries);
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf);
        btnLogout = findViewById(R.id.btnLogout);
        lvEntries = findViewById(R.id.lvEntries);
        chronometer = findViewById(R.id.chronometerTimer);

        // Setze den Banner-Text: "Willkommen, [Benutzername]"
        tvHeader.setText("Willkommen, " + loggedInUserName);

        // NFC-Adapter initialisieren (optional)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "Dieses Gerät unterstützt kein NFC.", Toast.LENGTH_LONG).show();
        }

        // Prüfe, ob bereits ein aktiver Eintrag existiert und setze den Chronometer entsprechend
        WorkTimeEntry activeEntry = db.workTimeEntryDao().getActiveEntryForUser(loggedInUserId);
        if (activeEntry != null) {
            currentEntry = activeEntry;
            long offset = System.currentTimeMillis() - (currentEntry.startTime + currentEntry.pauseDuration);
            chronometer.setBase(SystemClock.elapsedRealtime() - offset);
            chronometer.start();
        }

        // Start-Button: Arbeitszeiterfassung starten
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentEntry == null) {
                    long startTime = System.currentTimeMillis();
                    currentEntry = new WorkTimeEntry(loggedInUserId, startTime);
                    db.workTimeEntryDao().insert(currentEntry);
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
                    isPaused = false;
                    Toast.makeText(MainActivity.this, "Arbeitszeit gestartet: " + formatTime(startTime), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Arbeitszeit läuft bereits. Nutze den Pause-Button.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Pause-Button: Arbeitszeiterfassung pausieren und fortsetzen
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentEntry != null && !isPaused) {
                    pauseStartTime = System.currentTimeMillis();
                    chronometer.stop();
                    isPaused = true;
                    Toast.makeText(MainActivity.this, "Arbeitszeit pausiert", Toast.LENGTH_SHORT).show();
                } else if (currentEntry != null && isPaused) {
                    long pauseDuration = System.currentTimeMillis() - pauseStartTime;
                    currentEntry.pauseDuration += pauseDuration;
                    chronometer.setBase(chronometer.getBase() + pauseDuration);
                    chronometer.start();
                    isPaused = false;
                    Toast.makeText(MainActivity.this, "Arbeitszeit fortgesetzt", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Keine laufende Arbeitszeit!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Stop-Button: Arbeitszeiterfassung beenden
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentEntry != null && currentEntry.endTime == null) {
                    long stopTime = System.currentTimeMillis();
                    currentEntry.endTime = stopTime;
                    db.workTimeEntryDao().update(currentEntry);
                    chronometer.stop();
                    long calculatedDuration = calculateWorkingTime(currentEntry.startTime, currentEntry.endTime, currentEntry.pauseDuration);
                    currentEntry.totalTime = calculatedDuration;
                    db.workTimeEntryDao().update(currentEntry);
                    String durationStr = formatDuration(calculatedDuration);
                    Toast.makeText(MainActivity.this, "Arbeitszeit gestoppt. Dauer: " + durationStr, Toast.LENGTH_LONG).show();
                    // Setze currentEntry auf null, sodass ein neuer Eintrag gestartet werden kann.
                    currentEntry = null;
                    chronometer.setBase(SystemClock.elapsedRealtime());
                } else {
                    Toast.makeText(MainActivity.this, "Keine laufende Arbeitszeit gefunden.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Button "Alle Einträge anzeigen": Zeigt alle Einträge des eingeloggten Benutzers an
        btnShowEntries.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<WorkTimeEntry> entries = db.workTimeEntryDao().getEntriesForUser(loggedInUserId);
                List<String> entryStrings = new ArrayList<>();
                DateFormat dateFormat = DateFormat.getDateTimeInstance();
                for (WorkTimeEntry entry : entries) {
                    String startStr = dateFormat.format(new Date(entry.startTime));
                    String stopStr = entry.endTime != null ? dateFormat.format(new Date(entry.endTime)) : "laufend";
                    String durationStr = entry.endTime != null ? formatDuration(entry.totalTime) : "-";
                    String text = "Start: " + startStr + "\nStop: " + stopStr + "\nDauer: " + durationStr;
                    entryStrings.add(text);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_list_item_1, entryStrings);
                lvEntries.setAdapter(adapter);
            }
        });

        // Button "PDF-Bericht generieren": Erzeugt ein PDF in einem Hintergrundthread
        btnGeneratePdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        List<WorkTimeEntry> entries = db.workTimeEntryDao().getEntriesForUser(loggedInUserId);
                        generatePdfReport(entries);
                    }
                }).start();
            }
        });

        // Logout-Button: Meldet den Benutzer ab, ohne den aktiven Eintrag zu löschen (aktiven Eintrag bleibt in der DB)
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Falls ein aktiver Eintrag läuft, Warnung anzeigen
                if (currentEntry != null && currentEntry.endTime == null) {
                    new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle("Aktive Arbeitszeit")
                            .setMessage("Eine aktive Arbeitszeiterfassung läuft noch. Beim Logout wird diese fortgesetzt. Trotzdem ausloggen?")
                            .setPositiveButton("Ausloggen", (dialog, which) -> logout())
                            .setNegativeButton("Abbrechen", null)
                            .show();
                } else {
                    logout();
                }
            }
        });
    }

    // Hilfsmethode zum Logout: Startet LoginActivity neu und leert den Back-Stack
    private void logout() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Hilfsmethode: Formatiert einen Zeitstempel in lesbares Datum/Uhrzeit-Format
    private String formatTime(long timeMillis) {
        DateFormat df = DateFormat.getDateTimeInstance();
        return df.format(new Date(timeMillis));
    }

    // Hilfsmethode: Formatiert eine Dauer (in Millisekunden) in "X Std Y Min"
    private String formatDuration(long durationMillis) {
        long totalMinutes = durationMillis / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + " Std " + minutes + " Min";
    }

    // Hilfsmethode: Rundet eine Dauer (in ms) auf den nächsten 5-Minuten-Takt
    private long roundDurationTo5Minutes(long duration) {
        long interval = 300000; // 5 Minuten in ms
        if (duration % interval == 0) return duration;
        return ((duration / interval) + 1) * interval;
    }

    // Hilfsmethode: Berechnet die effektive Arbeitszeit (endTime - startTime - pauseDuration),
    // wendet automatische Pausenzeiten an (30 Min nach 6h, 60 Min nach 9h) und rundet auf 5 Minuten
    private long calculateWorkingTime(long start, long end, long pauseDuration) {
        long rawDuration = end - start - pauseDuration;
        long minutes = rawDuration / 60000;
        if (minutes >= 540) { // ≥ 9 Stunden
            minutes -= 60;
        } else if (minutes >= 360) { // ≥ 6 Stunden
            minutes -= 30;
        }
        long adjusted = minutes * 60000;
        return roundDurationTo5Minutes(adjusted);

    }

    // Erstellt einen PDF-Bericht und speichert ihn im app-eigenen Dokumenten-Ordner
    // Der Bericht enthält einen Header (Benutzername, aktueller Monat/Jahr, "Trattoria Volare"),
    // eine Tabelle (Spalten: Datum, Start, Ende, Pause, Dauer) und eine Gesamtsumme der Arbeitszeit.

// ... weitere Importe

    private void generatePdfReport(List<WorkTimeEntry> entries) {
        // Filtere Einträge für den aktuellen Monat
        Calendar currentCal = Calendar.getInstance();
        int currentMonth = currentCal.get(Calendar.MONTH);
        int currentYear = currentCal.get(Calendar.YEAR);
        List<WorkTimeEntry> monthlyEntries = new ArrayList<>();
        long totalMonthlyDuration = 0;
        for (WorkTimeEntry entry : entries) {
            Calendar entryCal = Calendar.getInstance();
            entryCal.setTimeInMillis(entry.startTime);
            int entryMonth = entryCal.get(Calendar.MONTH);
            int entryYear = entryCal.get(Calendar.YEAR);
            if (entryYear == currentYear && entryMonth == currentMonth) {
                monthlyEntries.add(entry);
                if (entry.endTime != null) {
                    totalMonthlyDuration += entry.totalTime;
                }
            }
        }

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM");
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        String monthName = monthFormat.format(new Date());
        String yearString = yearFormat.format(new Date());

        // Erstelle das PDF-Dokument
        PdfDocument document = new PdfDocument();
        int pageWidth = 595;
        int pageHeight = 842;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // Definiere Ränder und Spaltenbreite
        int leftMargin = 40;
        int rightMargin = 40;
        int availableWidth = pageWidth - leftMargin - rightMargin;  // 595 - 80 = 515
        int numColumns = 5;
        int colWidth = availableWidth / numColumns; // ca. 103 pt pro Spalte

        // Spaltenpositionen
        float col1 = leftMargin;
        float col2 = leftMargin + colWidth;
        float col3 = leftMargin + 2 * colWidth;
        float col4 = leftMargin + 3 * colWidth;
        float col5 = leftMargin + 4 * colWidth;

        // Header
        int y = 50;
        paint.setTextSize(18);
        String header = loggedInUserName + " - " + monthName + " " + yearString + " - Trattoria Volare";
        canvas.drawText(header, leftMargin, y, paint);
        y += 30;

        // Tabellenkopf (Spalten: Datum, Start, Ende, Pause, Dauer)
        paint.setTextSize(12);
        canvas.drawText("Datum", col1, y, paint);
        canvas.drawText("Start", col2, y, paint);
        canvas.drawText("Ende", col3, y, paint);
        canvas.drawText("Pause", col4, y, paint);
        canvas.drawText("Dauer", col5, y, paint);
        y += 15;

        // Zeichne eine Trennlinie über die gesamte Breite
        canvas.drawText("--------------------------------------------------------------", leftMargin, y, paint);
        y += 15;

        // Tabellenzeilen für jeden Eintrag
        SimpleDateFormat dfDate = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat dfTime = new SimpleDateFormat("HH:mm");
        for (WorkTimeEntry entry : monthlyEntries) {
            String dateStr = dfDate.format(new Date(entry.startTime));
            String startStr = dfTime.format(new Date(entry.startTime));
            String endStr = (entry.endTime != null) ? dfTime.format(new Date(entry.endTime)) : "laufend";
            String pauseStr = (entry.endTime != null) ? (entry.pauseDuration / 60000) + " Min" : "-";
            String durationStr = (entry.endTime != null) ? formatDuration(entry.totalTime) : "-";

            canvas.drawText(dateStr, col1, y, paint);
            canvas.drawText(startStr, col2, y, paint);
            canvas.drawText(endStr, col3, y, paint);
            canvas.drawText(pauseStr, col4, y, paint);
            canvas.drawText(durationStr, col5, y, paint);
            y += 15;
            // Zeichne eine Trennlinie
            canvas.drawLine(leftMargin, y, pageWidth - rightMargin, y, paint);
            y += 15;
            if (y > pageHeight - 40) break;
        }
        // Gesamtdauer anzeigen
        y += 20;
        String totalStr = "Gesamtdauer: " + formatDuration(totalMonthlyDuration);
        canvas.drawText(totalStr, leftMargin, y, paint);

        document.finishPage(page);

        // Speichere das PDF über MediaStore (empfohlene Methode ab Android 10+)
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "Arbeitszeitbericht_" + monthName + "_" + yearString + ".pdf");
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

        android.net.Uri pdfUri = getContentResolver().insert(android.provider.MediaStore.Files.getContentUri("external"), values);
        if (pdfUri != null) {
            try {
                try (java.io.OutputStream os = getContentResolver().openOutputStream(pdfUri)) {
                    document.writeTo(os);
                }
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "PDF erstellt: " + pdfUri.toString(), Toast.LENGTH_LONG).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Fehler beim Erstellen des PDFs", Toast.LENGTH_SHORT).show());
            }
        } else {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Fehler: URI ist null", Toast.LENGTH_LONG).show());
        }
        document.close();
    }




    // NFC-Foreground Dispatch aktivieren
    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            IntentFilter[] intentFiltersArray = new IntentFilter[]{};
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null);
        }
    }

    // NFC-Foreground Dispatch deaktivieren
    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    // NFC-Tag verarbeiten (optional)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                byte[] tagId = tag.getId();
                String tagIdStr = bytesToHex(tagId);
                long currentTime = System.currentTimeMillis();
                // Beispiel: Erstelle einen speziellen Eintrag für einen NFC-Scan
                WorkTimeEntry entry = new WorkTimeEntry(loggedInUserId, currentTime);
                db.workTimeEntryDao().insert(entry);
                Toast.makeText(this, "RFID-Tag gescannt: " + tagIdStr, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Hilfsmethode: Konvertiert ein Byte-Array in einen Hex-String
    private String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
