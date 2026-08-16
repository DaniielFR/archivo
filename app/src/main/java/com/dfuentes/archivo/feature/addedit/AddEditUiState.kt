package com.dfuentes.archivo.feature.addedit

import com.dfuentes.archivo.core.model.Format
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.Status

/**
 * Estado del formulario. Los números se guardan como String porque el usuario
 * escribe texto: convertir en cada pulsación haría que "12" fuese inescribible
 * cuando el "1" ya es un número válido pero incompleto.
 */
data class AddEditUiState(
    val type: MediaType = MediaType.BOOK,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val title: String = "",
    val creators: String = "",
    val year: String = "",
    val pageCount: String = "",
    val runtimeMinutes: String = "",
    val seasonCount: String = "",
    val synopsis: String = "",
    val status: Status = Status.FINISHED,
    val rating: Int? = null,
    val startedOn: Long? = null,
    val finishedOn: Long? = null,
    val format: Format? = null,
    val notes: String = "",
    val saved: Boolean = false,
) {
    /** Lo único obligatorio es el título. Cualquier otra exigencia sobra. */
    val canSave: Boolean get() = title.isNotBlank()

    val creatorLabel: String
        get() = when (type) {
            MediaType.BOOK -> "Autor o autora"
            MediaType.MOVIE -> "Dirección"
            MediaType.SERIES -> "Creación"
        }
}

sealed interface AddEditAction {
    data class TypeChanged(val type: MediaType) : AddEditAction

    data class TitleChanged(val value: String) : AddEditAction

    data class CreatorsChanged(val value: String) : AddEditAction

    data class YearChanged(val value: String) : AddEditAction

    data class PageCountChanged(val value: String) : AddEditAction

    data class RuntimeChanged(val value: String) : AddEditAction

    data class SeasonCountChanged(val value: String) : AddEditAction

    data class SynopsisChanged(val value: String) : AddEditAction

    data class StatusChanged(val status: Status) : AddEditAction

    data class RatingChanged(val rating: Int) : AddEditAction

    data class StartedOnChanged(val epochDay: Long?) : AddEditAction

    data class FinishedOnChanged(val epochDay: Long?) : AddEditAction

    data class FormatChanged(val format: Format?) : AddEditAction

    data class NotesChanged(val value: String) : AddEditAction

    data object SaveRequested : AddEditAction
}
