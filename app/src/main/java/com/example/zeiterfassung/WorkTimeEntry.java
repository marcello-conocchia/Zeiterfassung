package com.example.zeiterfassung;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "work_time_entries")
public class WorkTimeEntry {
    @PrimaryKey(autoGenerate = true)
    public int entryId;
    public int userId; // Fremdschlüssel zu User
    public long startTime;
    public Long endTime; // Null, solange noch aktiv
    public long totalTime; // in Millisekunden (berechnet)
    public long pauseDuration; // Gesamtdauer der Pausen in Millisekunden

    public WorkTimeEntry(int userId, long startTime) {
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = null;
        this.totalTime = 0;
        this.pauseDuration = 0;
    }
}
