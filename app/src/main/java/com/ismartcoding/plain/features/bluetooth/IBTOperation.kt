package com.ismartcoding.plain.features.bluetooth

interface IBTOperation {
    val device: BTDevice

    fun run()
}

data class BTOperationConnect(override val device: BTDevice) : IBTOperation {
    override fun run() {
        device.connect()
    }

    override fun toString(): String {
        return "BTOperationConnect: ${device.mac}"
    }
}

data class BTOperationDisconnect(override val device: BTDevice) : IBTOperation {
    override fun run() {
        device.disconnect()
        BluetoothUtil.signalEndOfOperation()
    }

    override fun toString(): String {
        return "BTOperationDisconnect: ${device.mac}"
    }
}

data class BTOperationCharacteristicWrite(
    override val device: BTDevice,
    val api: BluetoothApi,
    val value: String,
) : IBTOperation {
    override fun run() {
        if (!device.writeCharacteristic(api, value)) {
            BluetoothUtil.signalEndOfOperation()
        }
    }

    override fun toString(): String {
        return "BTOperationCharacteristicWrite: ${device.mac}, ${api.charUUID}"
    }
}

data class BTOperationCharacteristicRead(
    override val device: BTDevice,
    val api: BluetoothApi,
) : IBTOperation {
    override fun run() {
        if (!device.readCharacteristic(api)) {
            BluetoothUtil.signalEndOfOperation()
        }
    }

    override fun toString(): String {
        return "BTOperationCharacteristicRead: ${device.mac}, ${api.charUUID}"
    }
}

data class BTOperationEnableNotifications(
    override val device: BTDevice,
    val api: BluetoothApi,
    val enable: Boolean,
) : IBTOperation {
    override fun run() {
        if (!device.enableNotification(api, enable)) {
            BluetoothUtil.signalEndOfOperation()
        }
    }

    override fun toString(): String {
        return "BTOperationEnableNotifications: ${device.mac}, ${api.charUUID}, $enable"
    }
}

data class BTOperationMtuRequest(
    override val device: BTDevice,
    val mtu: Int,
) : IBTOperation {
    override fun run() {
        device.bluetoothGatt?.requestMtu(mtu)
    }

    override fun toString(): String {
        return "BTOperationMtuRequest: ${device.mac}, $mtu"
    }
}
