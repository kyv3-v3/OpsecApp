package com.opsecapp.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.opsecapp.data.local.entity.ItemEntity
import com.opsecapp.data.local.entity.ItemLinkEntity

data class ItemWithLinks(
  @Embedded val item: ItemEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "itemId"
  )
  val links: List<ItemLinkEntity>
)
