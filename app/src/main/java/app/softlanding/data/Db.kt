package app.softlanding.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods WHERE hidden = 0")
    fun all(): Flow<List<Food>>

    @Query("SELECT * FROM foods")
    suspend fun list(): List<Food>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun get(id: Long): Food?

    @Query("SELECT * FROM foods WHERE barcode = :code AND hidden = 0 LIMIT 1")
    suspend fun byBarcode(code: String): Food?

    @Query("SELECT * FROM foods WHERE `key` = :key LIMIT 1")
    suspend fun byKey(key: String): Food?

    @Query("SELECT COUNT(*) FROM foods WHERE source = 'BUILTIN'")
    suspend fun builtinCount(): Int

    @Insert
    suspend fun insert(f: Food): Long

    @Insert
    suspend fun insertAll(f: List<Food>)

    @Update
    suspend fun update(f: Food)

    @Query("UPDATE foods SET uses = uses + 1, lastUsed = :at, lastGrams = :grams WHERE id = :id")
    suspend fun used(id: Long, at: Long, grams: Double)

    @Query("SELECT * FROM ingredients WHERE dish = :dish ORDER BY id")
    suspend fun ingredients(dish: Long): List<Ingredient>

    @Query("SELECT * FROM ingredients ORDER BY id")
    fun allIngredients(): Flow<List<Ingredient>>

    @Insert
    suspend fun addIngredients(xs: List<Ingredient>)

    @Query("DELETE FROM ingredients WHERE dish = :dish")
    suspend fun clearIngredients(dish: Long)
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY day, at")
    fun all(): Flow<List<Entry>>

    @Query("SELECT * FROM entries ORDER BY day, at")
    suspend fun list(): List<Entry>

    @Query("SELECT * FROM entries WHERE day = :day ORDER BY at")
    suspend fun day(day: Long): List<Entry>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: Long): Entry?

    @Insert
    suspend fun insert(e: Entry): Long

    @Insert
    suspend fun insertAll(e: List<Entry>)

    @Update
    suspend fun update(e: Entry)

    @Delete
    suspend fun delete(e: Entry)

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun count(): Int
}

@Dao
interface MiscDao {
    @Query("SELECT * FROM plates ORDER BY grams")
    fun plates(): Flow<List<Plate>>

    @Insert
    suspend fun addPlate(p: Plate): Long

    @Delete
    suspend fun deletePlate(p: Plate)

    @Query("SELECT * FROM weighs ORDER BY day")
    fun weighs(): Flow<List<Weigh>>

    @Query("SELECT * FROM weighs ORDER BY day")
    suspend fun weighList(): List<Weigh>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun weigh(w: Weigh)

    @Delete
    suspend fun unweigh(w: Weigh)

    @Query("SELECT * FROM fairs ORDER BY day")
    fun fairs(): Flow<List<Fair>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun fair(f: Fair)

    @Delete
    suspend fun unfair(f: Fair)
}

@Database(entities = [Food::class, Ingredient::class, Entry::class, Plate::class, Weigh::class, Fair::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class LandingDb : RoomDatabase() {
    abstract fun foods(): FoodDao
    abstract fun entries(): EntryDao
    abstract fun misc(): MiscDao

    companion object {
        fun create(context: Context): LandingDb = Room.databaseBuilder(context, LandingDb::class.java, "softlanding.db").build()
    }
}
