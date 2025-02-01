package com.example.zeiterfassung;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WorkTimeEntryDao {
    @Insert
    void insert(WorkTimeEntry entry);

    @Update
    void update(WorkTimeEntry entry);

    @Query("SELECT * FROM work_time_entries WHERE userId = :userId")
    List<WorkTimeEntry> getEntriesForUser(int userId);

    @Delete
    void delete(WorkTimeEntry entry);
    @Query("SELECT * FROM work_time_entries WHERE userId = :userId AND endTime IS NULL LIMIT 1")
    WorkTimeEntry getActiveEntryForUser(int userId);

}
