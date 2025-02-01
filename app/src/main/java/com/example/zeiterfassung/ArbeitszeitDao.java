package com.example.zeiterfassung;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ArbeitszeitDao {
    @Insert
    void insert(ArbeitszeitEntry entry);

    @Update
    void update(ArbeitszeitEntry entry);

    @Query("SELECT * FROM arbeitszeiten")
    List<ArbeitszeitEntry> getAll();

    @Delete
    void delete(ArbeitszeitEntry entry);
}
