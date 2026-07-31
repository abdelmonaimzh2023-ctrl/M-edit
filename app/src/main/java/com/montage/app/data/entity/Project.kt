package com.montage.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val videoUri: String,
    val thumbnailPath: String? = null,
    val duration: Long = 0,  // بالميلي ثانية
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val exportResolution: String = "1080p",
    val exportBitrate: Int = 8_000_000,  // 8 Mbps
    val exportFps: Int = 30
)
