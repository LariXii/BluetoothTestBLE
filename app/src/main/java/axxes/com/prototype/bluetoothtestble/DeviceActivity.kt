package axxes.com.prototype.bluetoothtestble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.method.ScrollingMovementMethod
import androidx.appcompat.app.AppCompatActivity
import com.example.android.whileinuselocation.manager.DSRCAttributManager
import com.example.android.whileinuselocation.manager.DSRCManager
import kotlinx.android.synthetic.main.activity_device.*


class DeviceActivity: AppCompatActivity() {

    val dsrcManager: DSRCManager = DSRCManager()

    var bluetoothDevice: BluetoothDevice? = null
    var bluetoothGatt: BluetoothGatt? = null
    var bluetoothAdapter : BluetoothAdapter? = null
    var bluetoothManager : BluetoothManager? = null

    var requestCharacteristic: BluetoothGattCharacteristic? = null
    var responseCharacteristic: BluetoothGattCharacteristic? = null
    var eventCharacteristic: BluetoothGattCharacteristic? = null

    val STATE_CONNECTED = 0
    val STATE_DISCONNECTED = 1
    val STATE_SENDABLE = 2
    val STATE_NOT_SENDABLE = 3

    var stateGatt = STATE_DISCONNECTED

    @SuppressLint("HandlerLeak")
    val handlerGattState: Handler =
    object: Handler(){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                STATE_CONNECTED -> {
                    btn_send.isEnabled = true
                    textView_state.text = "Connected"
                    textView_state.setTextColor(Color.GREEN)
                    btn_disconnect.text = "Déconnexion"
                }
                STATE_DISCONNECTED -> {
                    btn_send.isEnabled = false
                    textView_state.text = "Disconnected"
                    textView_state.setTextColor(Color.RED)
                    btn_disconnect.text = "Connexion"
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

    private val broadCastBluetooth: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        askToEnabledBT()
                    }
                    BluetoothAdapter.STATE_ON -> {
                        // TODO reconnect to BDO
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter

        registerReceiver(broadCastBluetooth, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        bluetoothDevice = intent.getParcelableExtra<BluetoothDevice>(EXTRA_DEVICE)
        connectBLE(bluetoothDevice)

        btn_send.isEnabled = false

        btn_send.setOnClickListener {
            sendPacketToBDO(bluetoothGatt!!,requestCharacteristic!!)
        }

        btn_disconnect.setOnClickListener {
            when(stateGatt){
                STATE_CONNECTED -> {
                    disconnectBLE()
                }
                STATE_DISCONNECTED -> {
                    // Clear previous connection
                    disconnectBLE()
                    closeBLE()
                    // Then connect
                    connectBLE(bluetoothDevice)
                }
            }
        }

        btn_exit.setOnClickListener {
            closeBLE()
            finish()
        }

        textView_log.movementMethod = ScrollingMovementMethod()
    }

    private fun askToEnabledBT(){
        val intentEnabledBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(intentEnabledBluetooth,
            PERMISSION_BLUETOOTH_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            PERMISSION_BLUETOOTH_REQUEST_CODE -> {
                when(resultCode){
                    Activity.RESULT_OK -> {

                    }
                    Activity.RESULT_CANCELED -> {

                    }
                }
            }
        }
    }

    private fun connectBLE(device: BluetoothDevice?) {
        if (device == null) {
            return
        }
        bluetoothGatt = device.connectGatt(this, true, bluetoothGattCallback)
        stateGatt = STATE_CONNECTED
    }

    private fun disconnectBLE(){
        bluetoothGatt?.disconnect()
        stateGatt = STATE_DISCONNECTED
    }

    private fun closeBLE(){
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
            showLog("\nLe statut de connexion du pont a été modifié : $status\nNouveau status de connexion du device: $newState")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    handlerGattState.obtainMessage(STATE_CONNECTED).sendToTarget()
                    bluetoothDevice = gatt.device
                    textView_name.text = bluetoothDevice?.name
                    discoverServices()
                }
                if(newState == BluetoothProfile.STATE_DISCONNECTED){
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
        const val PERMISSION_BLUETOOTH_REQUEST_CODE = 102
    }
}