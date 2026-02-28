package com.opsecapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.opsecapp.data.local.dao.CatalogDao
import com.opsecapp.data.local.entity.CatalogMetaEntity
import com.opsecapp.data.local.entity.CategoryEntity
import com.opsecapp.data.local.entity.ItemCategoryCrossRef
import com.opsecapp.data.local.entity.ItemEntity
import com.opsecapp.data.local.entity.ItemFtsEntity
import com.opsecapp.data.local.entity.ItemLinkEntity

@Database(
  entities = [
    CategoryEntity::class,
    ItemEntity::class,
    ItemCategoryCrossRef::class,
    ItemLinkEntity::class,
    ItemFtsEntity::class,
    CatalogMetaEntity::class
  ],
  version = 1,
  exportSchema = true
)
abstract class CatalogDatabase : RoomDatabase() {
  abstract fun catalogDao(): CatalogDao
}
