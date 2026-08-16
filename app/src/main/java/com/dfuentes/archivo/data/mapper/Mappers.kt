package com.dfuentes.archivo.data.mapper

import com.dfuentes.archivo.core.database.dao.WorkCard
import com.dfuentes.archivo.core.database.entity.EntryEntity
import com.dfuentes.archivo.core.database.entity.WorkEntity
import com.dfuentes.archivo.core.database.entity.WorkWithRelations
import com.dfuentes.archivo.core.model.Entry
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.PersonRole
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.core.model.Work
import com.dfuentes.archivo.core.util.sortTitleOf
import com.dfuentes.archivo.data.repository.WorkSummary

/**
 * Cada capa tiene su modelo y los mappers viven en la capa que consume.
 * Parece ceremonia hasta el día en que cambias una columna de la base de datos
 * y no tienes que tocar ni un composable.
 */

fun WorkCard.toSummary() = WorkSummary(
    id = id,
    // La proyección trae los enums como String (ver LibraryDao.WorkCard).
    // enumValueOf falla ruidosamente si algún día se renombra una constante,
    // que es justo lo que queremos: mejor un crash en debug que datos silenciosamente mal.
    type = MediaType.valueOf(type),
    title = title,
    year = year,
    coverPath = coverPath,
    dominantColor = dominantColor,
    creators = creators,
    status = status?.let(Status::valueOf),
    rating = rating,
    finishedOn = finishedOn,
)

fun WorkWithRelations.toDomain() = Work(
    id = work.id,
    type = work.type,
    title = work.title,
    originalTitle = work.originalTitle,
    year = work.year,
    synopsis = work.synopsis,
    language = work.language,
    coverPath = work.coverPath,
    coverUrl = work.coverUrl,
    dominantColor = work.dominantColor,
    pageCount = work.pageCount,
    publisher = work.publisher,
    isbn13 = work.isbn13,
    runtimeMinutes = work.runtimeMinutes,
    seasonCount = work.seasonCount,
    episodeCount = work.episodeCount,
    creators = people.map { it.name },
    genres = genres.map { it.name },
    tags = tags.map { it.name },
    entries = entries.map { it.toDomain() },
)

fun EntryEntity.toDomain() = Entry(
    id = id,
    workId = workId,
    status = status,
    rating = rating,
    startedOn = startedOn,
    finishedOn = finishedOn,
    notes = notes,
    format = format,
    isFavourite = isFavourite,
    progressValue = progressValue,
    progressSeason = progressSeason,
    round = round,
)

fun Work.toEntity(now: Long) = WorkEntity(
    id = id,
    type = type,
    title = title,
    originalTitle = originalTitle,
    sortTitle = sortTitleOf(title),
    year = year,
    synopsis = synopsis,
    language = language,
    coverUrl = coverUrl,
    coverPath = coverPath,
    dominantColor = dominantColor,
    pageCount = pageCount,
    publisher = publisher,
    isbn13 = isbn13,
    runtimeMinutes = runtimeMinutes,
    seasonCount = seasonCount,
    episodeCount = episodeCount,
    createdAt = now,
    updatedAt = now,
)

fun Entry.toEntity(now: Long) = EntryEntity(
    id = id,
    workId = workId,
    status = status,
    rating = rating,
    startedOn = startedOn,
    finishedOn = finishedOn,
    notes = notes,
    format = format,
    isFavourite = isFavourite,
    progressValue = progressValue,
    progressSeason = progressSeason,
    round = round,
    createdAt = now,
    updatedAt = now,
)

/** Roles que se muestran como autoría en tarjetas y cabeceras. */
val PRIMARY_ROLES = PersonRole.PRIMARY
