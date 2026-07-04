package com.xxh.ringbones.data.local.seeder

import kotlinx.serialization.Serializable

/**
 * JSON data model matching the structure of assets/jsonres JSON files.
 * Each JSON file contains an array of these objects representing ringtone metadata.
 *
 * @property title Ringtone display title
 * @property author Creator uploader name
 * @property time Publish date string (e.g. "Aug 4, 2017"), mapped to duration field
 * @property url Direct MP3 download URL
 * @property type Category label (e.g. "Baby", "Music", "Funny")
 */
@Serializable
data class JsonRingtoneModel(
    val title: String,
    val author: String,
    val time: String,
    val url: String,
    val type: String
)