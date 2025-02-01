package com.example.zeiterfassung;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDao {
    @Insert
    void insert(User user);

    @Query("SELECT * FROM users WHERE name = :name LIMIT 1")
    User getUserByName(String name);
}
