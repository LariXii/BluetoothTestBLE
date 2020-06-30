package axxes.com.prototype.bluetoothtestble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.PersistableBundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import axxes.com.prototype.bluetoothtestble.model.v2.Command
import axxes.com.prototype.bluetoothtestble.model.v2.Parameter
import axxes.com.prototype.bluetoothtestble.parameters.CommandDsrc2
import com.example.android.whileinuselocation.manager.DSRCAttributManager
import com.example.android.whileinuselocation.manager.DSRCManager
import kotlinx.android.synthetic.main.activity_device.*
import kotlinx.android.synthetic.main.activity_main.*

class DeviceActivity: AppCompatActivity() {

    val dsrcManager: DSRCManager = DSRCManager()

    var bluetoothDevice: BluetoothDevice? = null
    var bluetoothGatt: BluetoothGatt? = null

    var requestCharacteristic: BluetoothGattCharacteristic? = null
    var responseCharacteristic: BluetoothGattCharacteristic? = null
    var eventCharacteristic: BluetoothGattCharacteristic? = null

    val STATE_CONNECTED = 0
    val STATE_DISCONNECTED = 1
    val STATE_SENDABLE = 2
    val STATE_NOT_SENDABLE = 3
    val handlerGattState: Handler = @SuppressLint("HandlerLeak")
    object: Handler(){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                STATE_CONNECTED -> {
                    btn_send.isEnabled = true
                    textView_state.text = "Connected"
                    textView_state.setTextColor(Color.GREEN)
                }
                STATE_DISCONNECTED -> {
                    btn_send.isEnabled = false
                    textView_state.text = "Disconnected"
                    textView_state.setTextColor(Color.RED)
                }
                STATE_SENDABLE -> {
                    btn_send.isEnabled = true
                }
                STATE_NOT_SENDABLE -> {
                    btn_send.isEnabled = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)

        bluetoothDevice = intent.getParcelableExtra<BluetoothDevice>(EXTRA_DEVICE)
        connectBLE(bluetoothDevice)

        btn_send.isEnabled = false

        btn_send.setOnClickListener {
            sendPacketToBDO(bluetoothGatt!!,requestCharacteristic!!)
        }

        btn_disconnect.setOnClickListener {
            disconnectBLE()
            finish()
        }

        textView_log.movementMethod = ScrollingMovementMethod()
    }

    private fun connectBLE(device: BluetoothDevice?) {
        if (device == null) {
            return
        }
        bluetoothGatt = device.connectGatt(this, true, bluetoothGattCallback)
    }

    private fun disconnectBLE(){
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    private fun discoverServices() {
        if (bluetoothGatt == null) {
            //showLog("Erreur, l'objet BluetoothGatt est null")
            return
        }
        bluetoothGatt!!.discoverServices()
    }

    private fun enabledGattNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic){
        val enabled = true
        val descriptor = characteristic.getDescriptor(characteristic.descriptors[0].uuid)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        //gatt.writeDescriptor(descriptor)
        gatt.setCharacteristicNotification(characteristic, enabled)
    }

    private fun sendPacketToBDO(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic){

        val latitude = 6043662
        val longitude = 47258182
        val timeStamp: Long = System.currentTimeMillis()

        val gnssData = dsrcManager.prepareGNSSPacket(timeStamp,longitude,latitude,12,4)

        val attribut = DSRCAttributManager.finAttribut(2,50)
        val dataToSend = dsrcManager.prepareWriteCommandPacket(attribut!!,gnssData,
            autoFillWithZero = true,
            temporaryData = true
        )

        characteristic.value = dataToSend
        gatt.writeCharacteristic(characteristic)
    }

    private var bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            //showLog("\nLe statut de connexion du device a été modifié : $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    handlerGattState.obtainMessage(STATE_CONNECTED).sendToTarget()
                    bluetoothDevice = gatt.device
                    textView_name.text = bluetoothDevice?.name
                    discoverServices()
                }
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    handlerGattState.obtainMessage(STATE_DISCONNECTED).sendToTarget()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            showLog("Des services ont été découverts")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.services.forEach{  gattService ->
                    //if(ServiceDsrcUtils.isKnowing(gattService.uuid.toString()))
                    //Log.d(TAG,"Service : ${gattService.uuid}")
                    gattService.characteristics.forEach { gattCharacteristic ->
                        if(ServiceDsrcUtils.isKnowing(gattCharacteristic.uuid.toString())){
                            //Log.d(TAG,"Characteristic : ${gattCharacteristic.uuid}\nPermissions : ${gattCharacteristic.permissions}\nProperties : ${gattCharacteristic.properties}")
                            when(gattCharacteristic.uuid.toString()){
                                ServiceDsrcUtils.COMMAND -> {
                                    requestCharacteristic = gattCharacteristic
                                }
                                ServiceDsrcUtils.RESPONSE -> {
                                    responseCharacteristic = gattCharacteristic
                                }
                                ServiceDsrcUtils.EVENT -> {
                                    eventCharacteristic = gattCharacteristic
                                }
                            }
                        }
                    }
                }
                enabledGattNotification(gatt,responseCharacteristic!!)
                handlerGattState.obtainMessage(STATE_SENDABLE).sendToTarget()
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (characteristic != null) {

            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if(characteristic != null){

            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if(characteristic != null){
                showLog(dsrcManager.readGNSSPacket(characteristic.value))
            }
        }
    }

    private fun showLog(message: String) {
        textView_log.append("$message\n")
    }

    companion object{
        const val TAG = "DeviceActivity "
        const val EXTRA_DEVICE = "myExtra.DEVICE"
    }
}