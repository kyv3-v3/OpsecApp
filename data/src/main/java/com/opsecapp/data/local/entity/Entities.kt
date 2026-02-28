package com.opsecapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
  @PrimaryKey val id: String,
  val titleExact: String,
  val descriptionExact: String,
  val path: String,
  val sortOrder: Int
)

@Entity(
  tableName = "items",
  indices = [Index("sourceConfidence"), Index("installType")]
)
data class ItemEntity(
  @PrimaryKey val id: String,
  val titleExact: String,
  val descriptionExact: String,
  val badgeExact: String,
  val sectionExact: String,
  val packageId: String?,
  val githubRepo: String?,
  val sourceConfidence: String,
  val installType: String
)

@Entity(
  tableName = "item_category_cross_ref",
  primaryKeys = ["itemId", "categoryId"],
  indices = [Index("categoryId"), Index("itemId")]
)
data class ItemCategoryCrossRef(
  val itemId: String,
  val categoryId: String
)

@Entity(
  tableName = "item_links",
  primaryKeys = ["itemId", "url"],
  indices = [Index("itemId")]
)
data class ItemLinkEntity(
  val itemId: String,
  val type: String,
  val url: String,
  val labelExact: String,
  val expectedSha256: String?,
  val expectedSignerSha256: String?
)

@Fts4
@Entity(tableName = "items_fts")
data class ItemFtsEntity(
  @PrimaryKey
  @ColumnInfo(name = "rowid")
  val rowId: Int,
  val itemId: String,
  val titleExact: String,
  val descriptionExact: String,
  val sectionExact: String,
  val linksText: String
)

@Entity(tableName = "catalog_meta")
data class CatalogMetaEntity(
  @PrimaryKey val id: Int = 0,
  val schemaVersion: String,
  val generatedAt: String,
  val lastSyncedFromSourceAt: String,
  val sourceUrl: String,
  val sourceLanguage: String,
  val extractor: String,
  val signed: Boolean,
  val trustStatus: String
)
