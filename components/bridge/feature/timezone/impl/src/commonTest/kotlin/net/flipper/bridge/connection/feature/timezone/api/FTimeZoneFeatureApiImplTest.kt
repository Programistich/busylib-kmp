package net.flipper.bridge.connection.feature.timezone.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.UtcOffset
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcBleApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcBusyApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcMatterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSettingsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStorageApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStreamingApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSystemApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcTimeZoneApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcUpdaterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcWifiApi
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimezoneInfo
import net.flipper.bridge.connection.feature.rpc.api.model.RpcTimezoneListResponse
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class FTimeZoneFeatureApiImplTest {

    @Test
    fun GIVEN_device_zero_offset_WHEN_get_time_zone_info_THEN_offset_is_plus_zero() = runTest {
        val sut = FTimeZoneFeatureApiImpl(
            scope = backgroundScope,
            rpcFeatureApi = FakeRpcFeatureApi(FakeRpcTimeZoneApi(RpcTimezoneInfo("London", "+00:00", "GMT"))),
            fEventsFeatureApi = null
        )

        val info = sut.getTimeZoneInfoFlow().first()

        assertEquals(TimezoneInfo(name = "London", offset = "+00:00", abbr = "GMT"), info)
    }

    @Test
    fun GIVEN_device_non_zero_offsets_WHEN_get_time_zone_info_THEN_offset_keeps_device_format() = runTest {
        val offsets = listOf("+03:00", "-01:00", "+05:30", "-03:30", "+05:45")

        val result = offsets.map { offset ->
            FTimeZoneFeatureApiImpl(
                scope = backgroundScope,
                rpcFeatureApi = FakeRpcFeatureApi(FakeRpcTimeZoneApi(RpcTimezoneInfo("City", offset, "ABBR"))),
                fEventsFeatureApi = null
            ).getTimeZoneInfoFlow().first().offset
        }

        assertEquals(offsets, result)
    }

    @Test
    fun GIVEN_zero_offset_event_WHEN_device_pushes_time_zone_THEN_offset_is_plus_zero() = runTest {
        val events = FakeEventsFeatureApi()
        val sut = FTimeZoneFeatureApiImpl(
            scope = backgroundScope,
            rpcFeatureApi = FakeRpcFeatureApi(FakeRpcTimeZoneApi(RpcTimezoneInfo("Kyiv", "+03:00", "EEST"))),
            fEventsFeatureApi = events
        )
        val received = mutableListOf<TimezoneInfo>()
        backgroundScope.launch { sut.getTimeZoneInfoFlow().collect { received.add(it) } }
        runCurrent()

        events.flow.emit(
            ConsumableUpdateEvent.BusyLib(
                BusyLibUpdateEvent.Timezone(name = "London", offset = UtcOffset.ZERO, abbreviation = "GMT")
            )
        )
        runCurrent()

        assertEquals(TimezoneInfo(name = "London", offset = "+00:00", abbr = "GMT"), received.last())
    }
}

private class FakeEventsFeatureApi : FEventsFeatureApi {
    val flow = MutableSharedFlow<ConsumableUpdateEvent.BusyLib<*>>()

    override fun getBusyLibUpdateEvents(): Flow<ConsumableUpdateEvent.BusyLib<*>> = flow

    override fun onBusyLibEvent(event: BusyLibUpdateEvent) = Unit
}

private class FakeRpcTimeZoneApi(
    private val current: RpcTimezoneInfo
) : FRpcTimeZoneApi {
    override suspend fun getTimeTimezone(ignoreCache: Boolean): Result<RpcTimezoneInfo> = Result.success(current)

    override suspend fun postTimeTimezone(timezoneInfo: RpcTimezoneInfo): Result<Unit> = error("Not used")

    override suspend fun getTimeTzList(): Result<RpcTimezoneListResponse> = error("Not used")
}

private class FakeRpcFeatureApi(
    override val fRpcTimeZoneApi: FRpcTimeZoneApi
) : FRpcFeatureApi {
    override val fRpcSystemApi: FRpcSystemApi get() = error("Not used")
    override val fRpcWifiApi: FRpcWifiApi get() = error("Not used")
    override val fRpcBleApi: FRpcBleApi get() = error("Not used")
    override val fRpcSettingsApi: FRpcSettingsApi get() = error("Not used")
    override val fRpcStreamingApi: FRpcStreamingApi get() = error("Not used")
    override val fRpcAssetsApi: FRpcAssetsApi get() = error("Not used")
    override val fRpcStorageApi: FRpcStorageApi get() = error("Not used")
    override val fRpcUpdaterApi: FRpcUpdaterApi get() = error("Not used")
    override val fRpcMatterApi: FRpcMatterApi get() = error("Not used")
    override val fRpcBusyApi: FRpcBusyApi get() = error("Not used")
}
