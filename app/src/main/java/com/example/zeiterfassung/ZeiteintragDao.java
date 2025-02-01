package com.example.zeiterfassung;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ZeiteintragDao {
    @Insert
    void insert(Zeiteintrag eintrag);

    @Query("SELECT * FROM zeiteintraege")
    List<Zeiteintrag> getAll();

    @Delete
    void delete(Zeiteintrag eintrag);
}
