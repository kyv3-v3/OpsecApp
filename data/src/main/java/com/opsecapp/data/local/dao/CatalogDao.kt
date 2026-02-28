package com.opsecapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.opsecapp.data.local.entity.CatalogMetaEntity
import com.opsecapp.data.local.entity.CategoryEntity
import com.opsecapp.data.local.entity.ItemCategoryCrossRef
import com.opsecapp.data.local.entity.ItemEntity
import com.opsecapp.data.local.entity.ItemFtsEntity
import com.opsecapp.data.local.entity.ItemLinkEntity
import com.opsecapp.data.local.model.ItemWithLinks
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
  @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
  fun observeCategories(): Flow<List<CategoryEntity>>

  @Transaction
  @Query(
    """
      SELECT i.* FROM items i
      JOIN item_category_cross_ref c ON i.id = c.itemId
      WHERE c.categoryId = :categoryId
      ORDER BY i.sectionExact ASC, i.titleExact ASC
    """
  )
  fun observeCategoryItems(categoryId: String): Flow<List<ItemWithLinks>>

  @Transaction
  @Query("SELECT * FROM items ORDER BY titleExact ASC LIMIT :limit")
  fun observeHighlights(limit: Int): Flow<List<ItemWithLinks>>

  @Transaction
  @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
  fun observeItem(itemId: String): Flow<ItemWithLinks?>

  @Transaction
  @Query(
    """
      SELECT i.* FROM items i
      JOIN items_fts fts ON fts.itemId = i.id
      WHERE items_fts MATCH :ftsQuery
      AND (:sourceConfidence IS NULL OR i.sourceConfidence = :sourceConfidence)
      AND (:installType IS NULL OR i.installType = :installType)
      ORDER BY i.titleExact ASC
    """
  )
  fun search(
    ftsQuery: String,
    sourceConfidence: String?,
    installType: String?
  ): Flow<List<ItemWithLinks>>

  @Transaction
  @Query(
    """
      SELECT * FROM items
      WHERE (:sourceConfidence IS NULL OR sourceConfidence = :sourceConfidence)
      AND (:installType IS NULL OR installType = :installType)
      ORDER BY titleExact ASC
    """
  )
  fun filterItems(
    sourceConfidence: String?,
    installType: String?
  ): Flow<List<ItemWithLinks>>

  @Query("SELECT * FROM catalog_meta WHERE id = 0")
  fun observeMeta(): Flow<CatalogMetaEntity?>

  @Transaction
  suspend fun replaceAll(
    categories: List<CategoryEntity>,
    items: List<ItemEntity>,
    crossRefs: List<ItemCategoryCrossRef>,
    links: List<ItemLinkEntity>,
    ftsRows: List<ItemFtsEntity>,
    meta: CatalogMetaEntity
  ) {
    clearCategories()
    clearItems()
    clearCrossRefs()
    clearLinks()
    clearFts()
    clearMeta()

    insertCategories(categories)
    insertItems(items)
    insertCrossRefs(crossRefs)
    insertLinks(links)
    insertFts(ftsRows)
    insertMeta(meta)
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCategories(entries: List<CategoryEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertItems(entries: List<ItemEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCrossRefs(entries: List<ItemCategoryCrossRef>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLinks(entries: List<ItemLinkEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFts(entries: List<ItemFtsEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMeta(entry: CatalogMetaEntity)

  @Query("DELETE FROM categories")
  suspend fun clearCategories()

  @Query("DELETE FROM items")
  suspend fun clearItems()

  @Query("DELETE FROM item_category_cross_ref")
  suspend fun clearCrossRefs()

  @Query("DELETE FROM item_links")
  suspend fun clearLinks()

  @Query("DELETE FROM items_fts")
  suspend fun clearFts()

  @Query("DELETE FROM catalog_meta")
  suspend fun clearMeta()
}
