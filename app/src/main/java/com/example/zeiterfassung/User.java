package com.example.zeiterfassung;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String pinHash; // Gespeicherter Hash der PIN

    public User(String name, String pinHash) {
        this.name = name;
        this.pinHash = pinHash;
    }
}
