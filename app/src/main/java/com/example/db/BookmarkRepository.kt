package com.example.db

import com.example.model.BookmarkEntity
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun addBookmark(updateId: String) {
        bookmarkDao.insertBookmark(BookmarkEntity(updateId))
    }

    suspend fun removeBookmark(updateId: String) {
        bookmarkDao.deleteBookmarkById(updateId)
    }
}
