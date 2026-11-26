package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.CallRouteType
import com.example.domain.model.CallType
import com.example.domain.model.SipTransport

class Converters {

    @TypeConverter
    fun fromSipTransport(value: SipTransport): String = value.name

    @TypeConverter
    fun toSipTransport(value: String): SipTransport =
        runCatching { SipTransport.valueOf(value) }.getOrDefault(SipTransport.TLS)

    @TypeConverter
    fun fromCallType(value: CallType): String = value.name

    @TypeConverter
    fun toCallType(value: String): CallType =
        runCatching { CallType.valueOf(value) }.getOrDefault(CallType.OUTGOING)

    @TypeConverter
    fun fromCallRouteType(value: CallRouteType): String = value.name

    @TypeConverter
    fun toCallRouteType(value: String): CallRouteType =
        runCatching { CallRouteType.valueOf(value) }.getOrDefault(CallRouteType.SIM1)
}
