package com.example.healthmate.ble

import java.util.UUID

object BluetoothUUIDs {
    val UUID_THERMOMETER_SERVICE: UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb")
    val UUID_THERMOMETER_CHARACTERISTIC: UUID = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb")

    val UUID_BPM_SERVICE: UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb")
    val UUID_BPM_CHARACTERISTIC: UUID = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb")

    val CCC_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val UUID_SERVICE_DEVICE_INFO: UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
    val UUID_MANUFACTURER: UUID = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb")
    val UUID_MODEL_NUMBER: UUID = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb")

    val UUID_SERVICE_BATTERY: UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
    val UUID_BATTERY_LEVEL: UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")

    /*val deviceDetailsUUIDs = listOf(
        UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")
    )*/

}