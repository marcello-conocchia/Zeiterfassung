package com.example.zeiterfassung;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {User.class, WorkTimeEntry.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract WorkTimeEntryDao workTimeEntryDao();
}
