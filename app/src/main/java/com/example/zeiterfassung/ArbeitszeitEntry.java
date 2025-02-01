package com.example.zeiterfassung;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "arbeitszeiten")
public class ArbeitszeitEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long startTime;
    public Long stopTime; // Null, solange die Arbeitszeit läuft
    public long pauseDuration; // Gesamtdauer der manuellen Pause in Millisekunden

    public ArbeitszeitEntry(long startTime) {
        this.startTime = startTime;
        this.stopTime = null;
        this.pauseDuration = 0;
    }
}
