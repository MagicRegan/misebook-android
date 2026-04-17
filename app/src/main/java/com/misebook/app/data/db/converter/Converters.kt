package com.misebook.app.data.db.converter

import androidx.room.TypeConverter
import com.misebook.app.domain.model.MeasurementUnit

class Converters {
    @TypeConverter fun unitToString(u: MeasurementUnit): String = u.name
    @TypeConverter fun stringToUnit(s: String): MeasurementUnit =
        runCatching { MeasurementUnit.valueOf(s) }.getOrDefault(MeasurementUnit.NONE)
}
