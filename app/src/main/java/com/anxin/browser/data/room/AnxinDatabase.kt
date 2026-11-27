package com.anxin.browser.data.room

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["url"], unique = true)],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** 分组名，默认"默认"。用户可在保存页新建分组后归类书签。 */
    val groupName: String = "默认",
)

@Entity(
    tableName = "history",
    indices = [Index("time"), Index(value = ["url"])],
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val time: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "downloads",
    indices = [Index("createdAt")],
)
data class DownloadEntity(
    @PrimaryKey val id: Long,
    val fileName: String,
    val url: String,
    val mimeType: String?,
    val totalBytes: Long,
    val downloadedBytes: Long,
    /** [com.anxin.browser.core.model.DownloadStatus] 的名字，避免 Room 与 core 模块互相依赖。 */
    val status: String,
    val localUri: String?,
    val createdAt: Long,
    val fromIncognito: Boolean,
    val userAgent: String?,
    val cookie: String?,
    val referer: String?,
)

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt ASC")
    suspend fun loadAll(): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM downloads WHERE status IN ('SUCCESS', 'FAILED')")
    suspend fun clearFinished()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY id DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM bookmarks")
    suspend fun clear()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY time DESC LIMIT 300")
    fun observeRecent(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entity: HistoryEntity)

    @Update
    suspend fun update(entity: HistoryEntity)

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): HistoryEntity?

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class, DownloadEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AnxinDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        /**
         * v1 -> v2：新增 downloads 表。
         *
         * 必须显式写出来。此前用的是 `fallbackToDestructiveMigration()`，
         * 只要忘记写迁移，用户的书签和历史就会被静默清空——
         * 静默删数据比崩溃更糟，所以迁移写全，兜底也一并去掉。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `downloads` (
                        `id` INTEGER NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `mimeType` TEXT,
                        `totalBytes` INTEGER NOT NULL,
                        `downloadedBytes` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `localUri` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `fromIncognito` INTEGER NOT NULL,
                        `userAgent` TEXT,
                        `cookie` TEXT,
                        `referer` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_createdAt` ON `downloads` (`createdAt`)")
            }
        }

        /**
         * v2 -> v3：书签支持分组，补 groupName 列，存量书签归入"默认"。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `bookmarks` ADD COLUMN `groupName` TEXT NOT NULL DEFAULT '默认'"
                )
            }
        }
    }
}
