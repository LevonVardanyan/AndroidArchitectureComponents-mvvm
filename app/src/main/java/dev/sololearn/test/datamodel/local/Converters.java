package dev.sololearn.test.datamodel.local;

import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import dev.sololearn.test.util.Utils;

/**
 * converter for Dao
 */
public class Converters {

    @Nullable
    @TypeConverter
    public static Long fromISO(@Nullable String isoDate) {
        return isoDate == null ? null : Utils.convertFromString(isoDate).getTime();
    }

}
