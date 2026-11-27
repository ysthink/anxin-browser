package com.anxin.browser.data.room

import android.content.Context
import androidx.room.Room

/**
 * 进程内唯一的 [AnxinDatabase] 实例。
 *
 * 之前 `BookmarksHistoryRepo` 在自己构造时 `Room.databaseBuilder(...)`，
 * 如果下载存储也各自 build 一个，同一个 `anxin_data.db` 就会有多个 Room 实例，
 * SQLite 层面表现为互相看不到对方的事务、偶发 `database is locked`。
 * 统一走这里取实例。
 */
object AnxinDatabaseProvider {

    @Volatile
    private var instance: AnxinDatabase? = null

    fun get(context: Context): AnxinDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AnxinDatabase::class.java,
                "anxin_data.db",
            )
                .addMigrations(AnxinDatabase.MIGRATION_1_2, AnxinDatabase.MIGRATION_2_3)
                .build()
                .also { instance = it }
        }
}
