package com.dfuentes.archivo.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ArchivoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),   // tarjetas de obra
    large = RoundedCornerShape(20.dp),    // bottom sheets y diálogos
    extraLarge = RoundedCornerShape(28.dp),
)
