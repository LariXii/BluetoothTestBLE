package axxes.com.prototype.bluetoothtestble

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.example.android.whileinuselocation.manager.DSRCAttributManager
import com.example.android.whileinuselocation.manager.DSRCManager
import com.example.android.whileinuselocation.model.DSRCAttribut
import kotlinx.android.synthetic.main.activity_device.*


class DeviceActivity: AppCompatActivity() {

    val dsrcManager: DSRCManager = DSRCManager()

    var bluetoothDevice: BluetoothDevice? = null
    var bluetoothGatt: BluetoothGatt? = null
    lateinit var bluetoothAdapter : BluetoothAdapter
    lateinit var bluetoothManager : BluetoothManager
    lateinit var notificationManager: NotificationManager

    var requestCharacteristic: BluetoothGattCharacteristic? = null
    var responseCharacteristic: BluetoothGattCharacteristic? = null
    var eventCharacteristic: BluetoothGattCharacteristic? = null

    val STATE_CONNECTED = 0
    val STATE_DISCONNECTED = 1
    val STATE_SENDABLE = 2
    val STATE_NOT_SENDABLE = 3

    var stateDevice = STATE_DISCONNECTED

    var responseParameters: List<Pair<String, Int>>? = null

    @SuppressLint("HandlerLeak")
    private val handlerGattState: Handler =
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
                    btn_get_attr.isEnabled = true
                    editText_EID.isEnabled = true
                    editText_attrID.isEnabled = true
                }
                STATE_NOT_SENDABLE -> {
                    btn_send.isEnabled = false
                    btn_get_attr.isEnabled = false
                    editText_EID.isEnabled = false
                    editText_attrID.isEnabled = false
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
                        Log.d(TAG,"Etat du device $stateDevice")
                        if(stateDevice == STATE_DISCONNECTED){
                            // Clear previous connection
                            disconnectBLE()
                            closeBLE()
                            // Then connect
                            connectBLE(bluetoothDevice)
                        }
                    }
                }
            }
        }
    }

    val SEND = 0
    val RECEIVE = 1
    val END = 2

    // Handler used to do specific action in fact with packet sended to bdo
    private var handleResponse: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        bluetoothAdapter = bluetoothManager.adapter

        registerReceiver(broadCastBluetooth, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        bluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE)
        connectBLE(bluetoothDevice)

        btn_send.isEnabled = false

        btn_send.setOnClickListener {
            val packet = getPacketSetGNSSAttribut()
            sendPacketToBDO(bluetoothGatt!!,requestCharacteristic!!,packet)
            responseParameters = dsrcManager.readGNSSPacket()
        }

        btn_get_attr.setOnClickListener {
            val eid: Int = editText_EID.text.toString().toInt()
            val attrId: Int = editText_attrID.text.toString().toInt()
            val attribut = DSRCAttributManager.finAttribut(eid,attrId)
            if(attribut == null){
                btn_get_attr.setTextColor(Color.RED)
                Handler().postDelayed({
                    btn_get_attr.setTextColor(Color.BLACK)
                }, 2000)
            }
            else{
                btn_get_attr.setTextColor(Color.BLACK)
                val packet = getPacketGetAttribut(attribut)
                sendPacketToBDO(bluetoothGatt!!,requestCharacteristic!!,packet)
                responseParameters = dsrcManager.readGetAttributPacket(attribut)
            }
        }

        btn_test.setOnClickListener {
            getMenuAttribut()
        }

        btn_disconnect.setOnClickListener {
            when(stateDevice){
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadCastBluetooth)
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
                        disconnectBLE()
                        closeBLE()
                        connectBLE(bluetoothDevice)
                    }
                    Activity.RESULT_CANCELED -> {
                        disconnectBLE()
                        closeBLE()
                    }
                }
            }
        }
    }

    private fun createNotificationBluetooth(): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Création du nouveau Channel de notification
            val name = "BLUETOOTH_DESABLED"
            val mChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_BLUETOOTH_ID,
                name,
                NotificationManager.IMPORTANCE_LOW
            )

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(mChannel)
        }

        val notificationCompatBuilder =
            NotificationCompat.Builder(
                applicationContext,
                NOTIFICATION_CHANNEL_BLUETOOTH_ID
            )
        return notificationCompatBuilder
            .setContentTitle("Information")
            .setContentText("La connexion au BDO à échoué\nCliquez pour vous reconnecter")
            .setOngoing(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    private fun connectBLE(device: BluetoothDevice?) {
        Log.d(TAG,"onConnectBLE")
        if (device == null) {
            return
        }
        bluetoothGatt = device.connectGatt(this, true, bluetoothGattCallback)
        stateDevice = STATE_CONNECTED
    }

    private fun disconnectBLE(){
        Log.d(TAG,"onDisconnectBLE")
        bluetoothGatt?.disconnect()
        stateDevice = STATE_DISCONNECTED
    }

    private fun closeBLE(){
        Log.d(TAG,"onCloseBLE")
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    private fun discoverServices() {
        if (bluetoothGatt == null) {
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

    private fun getPacketGetAttribut(attributes: DSRCAttribut): ByteArray{
        return dsrcManager.prepareReadCommandPacket(attributes, false)
    }

    private fun getPacketSetGNSSAttribut(): ByteArray{
        val latitude = 6043662
        val longitude = 47258182
        val timeStamp: Long = System.currentTimeMillis()

        val gnssData = dsrcManager.prepareGNSSPacket(timeStamp,longitude,latitude,12,4)

        val attribut = DSRCAttributManager.finAttribut(2,50)

        return dsrcManager.prepareWriteCommandPacket(attribut!!,gnssData,
            autoFillWithZero = true,
            temporaryData = true
        )
    }

    private fun sendPacketToBDO(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, packet: ByteArray){
        characteristic.value = packet
        gatt.writeCharacteristic(characteristic)
    }

    private fun getMenuAttribut(){
        // Store all packets to send
        val queueRequest: MutableList<Pair<DSRCAttribut,ByteArray>> = mutableListOf()
        // Store all packets receive
        val queueResponse: MutableList<ByteArray> = mutableListOf()
        // Store all attributes to get
        val listAttribut: MutableList<DSRCAttribut> = mutableListOf()
        var attribut = DSRCAttributManager.finAttribut(1, 4)
        attribut?.let{listAttribut.add(it)}
        attribut = DSRCAttributManager.finAttribut(1, 19)
        attribut?.let{listAttribut.add(it)}
        attribut = DSRCAttributManager.finAttribut(1, 24)
        attribut?.let{listAttribut.add(it)}

        Log.d(TAG,"Liste des attributs : $listAttribut")

        val handler = @SuppressLint("HandlerLeak")
        object: Handler(){
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                var packetResponse: ByteArray? = null
                if(msg.obj != null) {
                    packetResponse = msg.obj as ByteArray
                }

                when(msg.what){
                    SEND -> {

                    }
                    RECEIVE -> {
                        // Add response
                        packetResponse?.let { queueResponse.add(it) }
                        // Remove first request packet added
                        val p = queueRequest.removeAt(0)
                        Log.d(TAG,"Suppression et lecture de l'attribut ${p.first.attrName}")

                        packetResponse?.let {
                            showLog(dsrcManager.readResponse(responseParameters!!,it))
                            Log.d(TAG,dsrcManager.readResponse(responseParameters!!,it))
                        }


                        if(queueRequest.isNotEmpty()){
                            Log.d(TAG,"Envoie de l'attribut ${queueRequest[0].first.attrName}")
                            sendPacketToBDO(bluetoothGatt!!,requestCharacteristic!!,queueRequest[0].second)
                            responseParameters = dsrcManager.readGetAttributPacket(queueRequest[0].first)
                        }
                        else{
                            obtainMessage(END).sendToTarget()
                        }
                    }
                    END -> {
                        // TODO change var ect with queueReponse
                        handleResponse = null
                    }
                }
            }
        }
        // TODO add all packet to queueRequest
        handleResponse = handler

        for(attr in listAttribut){
            queueRequest.add(Pair(attr,getPacketGetAttribut(attr)))
        }

        Log.d(TAG,"Envoie de l'attribut ${queueRequest[0].first.attrName}")
        sendPacketToBDO(bluetoothGatt!!,requestCharacteristic!!,queueRequest[0].second)
        responseParameters = dsrcManager.readGetAttributPacket(queueRequest[0].first)
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
            else{
                if (status == BluetoothGatt.GATT_FAILURE){
                    showLog("Echec de la connexion avec le BDO...\nPlusieurs raisons peuvent en être les causes. Vérifiez que vous êtes assez proche du badge lors de la connexion.")
                    val notificationBuilder = createNotificationBluetooth()
                    notificationManager.notify(NOTIFICATION_BLUETOOTH_ID, notificationBuilder.build())
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

                if(handleResponse != null){
                    handleResponse!!.obtainMessage(RECEIVE,characteristic.value).sendToTarget()
                }
                else{
                    showLog(dsrcManager.readResponse(responseParameters!!,characteristic.value))
                    responseParameters = null
                }
            }
        }
    }

    private fun showLog(message: String) {
        textView_log.text = message
    }

    companion object{
        private const val TAG = "DeviceActivity "
        const val EXTRA_DEVICE = "myExtra.DEVICE"
        private const val PERMISSION_BLUETOOTH_REQUEST_CODE = 102

        private const val NOTIFICATION_BLUETOOTH_ID = 87654321
        private const val NOTIFICATION_CHANNEL_BLUETOOTH_ID = "test_bluetooth_ble_01"
    }
}