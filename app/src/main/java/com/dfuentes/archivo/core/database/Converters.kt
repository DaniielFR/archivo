package com.dfuentes.archivo.core.database

import androidx.room.TypeConverter
import com.dfuentes.archivo.core.model.Format
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.MetadataSource
import com.dfuentes.archivo.core.model.PersonRole
import com.dfuentes.archivo.core.model.Status

/**
 * Los enums se guardan por NOMBRE, nunca por ordinal.
 * Guardar el ordinal significa que reordenar el enum corrompe silenciosamente
 * toda la base de datos, y es un bug que no da la cara hasta meses después.
 */
class Converters {
    @TypeConverter fun mediaTypeToString(v: MediaType): String = v.name
    @TypeConverter fun stringToMediaType(v: String): MediaType = MediaType.valueOf(v)

    @TypeConverter fun statusToString(v: Status): String = v.name
    @TypeConverter fun stringToStatus(v: String): Status = Status.valueOf(v)

    @TypeConverter fun formatToString(v: Format?): String? = v?.name
    @TypeConverter fun stringToFormat(v: String?): Format? = v?.let(Format::valueOf)

    @TypeConverter fun roleToString(v: PersonRole): String = v.name
    @TypeConverter fun stringToRole(v: String): PersonRole = PersonRole.valueOf(v)

    @TypeConverter fun sourceToString(v: MetadataSource): String = v.name
    @TypeConverter fun stringToSource(v: String): MetadataSource = MetadataSource.valueOf(v)
}
