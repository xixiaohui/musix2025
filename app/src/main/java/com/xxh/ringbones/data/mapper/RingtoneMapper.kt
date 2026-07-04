package com.xxh.ringbones.data.mapper

import com.xxh.ringbones.data.local.entity.RingtoneEntity
import com.xxh.ringbones.domain.model.Ringtone

/**
 * Maps between Room entity and domain model.
 * The domain model adds `isFavorite` which is resolved at the repository level.
 */
object RingtoneMapper {

    /** Convert Room entity to domain model, with favorite flag from entity. */
    fun toDomain(entity: RingtoneEntity): Ringtone = toDomain(entity, isFavorite = entity.isFavorite)

    /** Convert Room entity to domain model, overriding the favorite flag. */
    fun toDomain(entity: RingtoneEntity, isFavorite: Boolean): Ringtone = Ringtone(
        id = entity.id,
        title = entity.title,
        author = entity.author,
        duration = entity.duration,
        url = entity.url,
        mimeType = entity.mimeType,
        category = entity.category,
        coverImageUrl = entity.coverImageUrl,
        fileSize = entity.fileSize,
        downloadPath = entity.downloadPath,
        playCount = entity.playCount,
        lastPlayedAt = entity.lastPlayedAt,
        isFavorite = isFavorite
    )

    /** Convert domain model to Room entity. */
    fun toEntity(domain: Ringtone): RingtoneEntity = RingtoneEntity(
        id = domain.id,
        title = domain.title,
        author = domain.author,
        duration = domain.duration,
        url = domain.url,
        mimeType = domain.mimeType,
        category = domain.category,
        coverImageUrl = domain.coverImageUrl,
        fileSize = domain.fileSize,
        downloadPath = domain.downloadPath,
        playCount = domain.playCount,
        lastPlayedAt = domain.lastPlayedAt
    )
}
