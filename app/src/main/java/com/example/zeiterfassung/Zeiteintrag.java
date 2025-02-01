package com.example.zeiterfassung;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "zeiteintraege")
public class Zeiteintrag {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String mitarbeiterId;
    public long timestamp;
    public String typ;

    public Zeiteintrag(String mitarbeiterId, long timestamp, String typ) {
        this.mitarbeiterId = mitarbeiterId;
        this.timestamp = timestamp;
        this.typ = typ;
    }
}
