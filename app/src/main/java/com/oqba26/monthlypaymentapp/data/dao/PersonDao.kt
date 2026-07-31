package com.oqba26.monthlypaymentapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oqba26.monthlypaymentapp.data.model.Person
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query("SELECT * FROM persons ORDER BY displayOrder ASC")
    fun getAllPersonsFlow(): Flow<List<Person>>

    @Query("SELECT * FROM persons ORDER BY displayOrder ASC")
    suspend fun getAllPersons(): List<Person>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    fun getPersonByIdFlow(id: String): Flow<Person?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(persons: List<Person>)

    @Query("UPDATE persons SET isArchived = :isArchived WHERE id = :personId")
    suspend fun updateArchivedStatus(personId: String, isArchived: Boolean)

    @Query("UPDATE persons SET displayOrder = :displayOrder WHERE id = :personId")
    suspend fun updateDisplayOrder(personId: String, displayOrder: Int)

    @Query("DELETE FROM persons")
    suspend fun deleteAll()
}