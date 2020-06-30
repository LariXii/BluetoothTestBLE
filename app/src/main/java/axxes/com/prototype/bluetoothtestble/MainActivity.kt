package axxes.com.prototype.bluetoothtestble

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import axxes.com.prototype.bluetoothtestble.model.v2.Command
import axxes.com.prototype.bluetoothtestble.model.v2.Parameter
import axxes.com.prototype.bluetoothtestble.parameters.CommandDsrc2
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    var bluetoothManager: BluetoothManager? = null
    var bluetoothAdapter: BluetoothAdapter? = null
    var bluetoothDevice: BluetoothDevice? = null
    var bluetoothGatt: BluetoothGatt? = null

    private lateinit var set_pairedDevices: MutableList<BluetoothDevice>
    private lateinit var hashMap_pairedDevices: HashMap<String,BluetoothDevice>
    private lateinit var adapter_paired_devices: ListAdapter_BLE_Devices

    private lateinit var myCommand: Command

    var deviceDetected = false
    var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter

        if (!locationPermissionApproved()) {
            if(Build.VERSION.SDK_INT > 22){
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        }
        txt_log.movementMethod = ScrollingMovementMethod()
        button_start.setOnClickListener {
            txt_log.text = ""
            startScan()
        }
        initializeLayout()
    }

    private fun initializeLayout() {
        set_pairedDevices = mutableListOf()
        hashMap_pairedDevices = HashMap()

        adapter_paired_devices = ListAdapter_BLE_Devices(applicationContext,R.layout.ble_devices_listitems,set_pairedDevices)
        lv_paired_devices.adapter = adapter_paired_devices
        lv_paired_devices.onItemClickListener = this
    }

    override fun onStop() {
        super.onStop()
        bluetoothGatt?.close()
    }

    private fun locationPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != PERMISSION_REQUEST_CODE) return
        var idxPermFine = -1
        var i = 0
        while (i < permissions.size && idxPermFine == -1) {
            if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION) idxPermFine = i
            i++
        }
        if (grantResults[idxPermFine] == PackageManager.PERMISSION_GRANTED) initScan()
    }

    private fun initScan() {
        if (!bluetoothAdapter?.isEnabled!!) {
            val CODE_DEMANDE_ACTIVATION = 1234
            val demandeActivationBLE = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(demandeActivationBLE, CODE_DEMANDE_ACTIVATION)
        } else startScan()
    }

    private fun startScan() {
        showLog("Start scan ...")
        isScanning = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) scanAPI18() else scanAPI21()
    }

    @TargetApi(18)
    private fun scanAPI18() {
        //Création du callback
        val leScanCallback: BluetoothAdapter.LeScanCallback =
                BluetoothAdapter.LeScanCallback{ device: BluetoothDevice, rssi: Int, _: ByteArray ->
                    Log.d(TAG,"Device trouvé : ${device.uuids}, ${device.name} ${device.address} $rssi")
                    showLog(" Force du signal : $rssi")
                    set_pairedDevices.add(device)

                    //connectBLE()
        }
        showLog("Lancement du scan en API 18")
        bluetoothAdapter?.startLeScan(arrayOf(UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")),leScanCallback)
        //Arrêt du scan 5s après
        Handler().postDelayed({
            bluetoothAdapter?.stopLeScan(leScanCallback)
            isScanning = false
            showLog("Arrêt du scan en API 18")
            if (!deviceDetected) relaunchScanDelayed()
        }, 5000)
    }

    @TargetApi(21)
    private fun scanAPI21() {
        //Création du callback
        val scanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                val rssi: Int = result.rssi
                showLog(" Force du signal : $rssi")
                bluetoothDevice = result.device
                addDevice(result.device)
            }
        }
        //Récupération du scanner
        val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
        //Début du scan
        val builderScanFilter = ScanFilter.Builder()
        builderScanFilter
            //.setServiceUuid(ParcelUuid(UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")))
            .setDeviceAddress("1C:23:2C:46:43:52")
        val bScanSettings = ScanSettings.Builder()
        bScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        //bluetoothLeScanner?.startScan(mutableListOf(builderScanFilter.build()),bScanSettings.build(),scanCallback)
        bluetoothLeScanner?.startScan(scanCallback)
        //showLog("Lancement du scan en API 21")
        //Arrêt du scan 5s plus tard
        Handler().postDelayed({
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            showLog("Arrêt du scan en API 21")
            Log.d(TAG,"${adapter_paired_devices.count}")
            //if (!deviceDetected) relaunchScanDelayed()
        }, 5000)
    }

    private fun relaunchScanDelayed() {
        showLog("Attente 5 sec.")
        Handler().postDelayed({
            startScan()
        }, 5000)
    }

    fun addDevice(device: BluetoothDevice) {
        val address = device.address
        if (!hashMap_pairedDevices.containsKey(address)) {
            hashMap_pairedDevices[address] = device
            set_pairedDevices.add(device)
        }
        adapter_paired_devices.notifyDataSetChanged()
    }

    private fun connectBLE(device: BluetoothDevice?) {
        if (device == null) {
            return
        }
        bluetoothGatt = device.connectGatt(this, true, bluetoothGattCallback)
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

    private fun preparePacketGNSS(_timestamp: Long, _longitude: Int, _latitude: Int, _hdop: Int, _numberOfSattelites: Int): ByteArray{
        val ret: ByteArray
        val listTmp = mutableListOf<Byte>()

        listTmp.addAll(Parameter.toBytes(_timestamp,4))
        listTmp.addAll(Parameter.toBytes(_longitude,4))
        listTmp.addAll(Parameter.toBytes(_latitude,4))

        listTmp.addAll(Parameter.toBytes(_hdop,1))
        listTmp.addAll(Parameter.toBytes(_numberOfSattelites,1))
        ret = listTmp.toByteArray()

        return ret
    }

    private fun requestingFastConnection(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic){
        //myCommand = Command(CommandDsrc2.CONTROL_BLE_FAST_CONNECTION,null)
        //myCommand = Command(CommandDsrc2.OPERATION_GET_ATTR, listOf(0, 1, 16))

        val latitude = 6043662
        val longitude = 47258182
        val timeStamp: Long = System.currentTimeMillis()
        val packetGNSS = preparePacketGNSS(timeStamp,longitude,latitude,12,4)

        myCommand = Command(CommandDsrc2.OPERATION_SET_ATTR, listOf(2,2,50,23))
        characteristic.value = myCommand.request
        gatt.writeCharacteristic(characteristic)
    }

    private var bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            //showLog("\nLe statut de connexion du device a été modifié : $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    bluetoothDevice = gatt.device
                    showLog("Le device suivant est connecté :" + gatt.device.address)
                    showLog("Nom : ${bluetoothDevice?.name}\n" +
                            "Adresse : ${bluetoothDevice?.address}\n" +
                            "ClassJava : ${bluetoothDevice?.javaClass}\n" +
                            "Bluetooth class ${bluetoothDevice?.bluetoothClass}\n" +
                            "Bond State : ${bluetoothDevice?.bondState}\n" +
                            "UUID ${bluetoothDevice?.uuids?.get(0).toString()}\n")
                    discoverServices()
                    deviceDetected = true
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            showLog("Des services ont été découverts")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                var charac_command: BluetoothGattCharacteristic? = null
                var charac_response: BluetoothGattCharacteristic? = null
                var charac_event: BluetoothGattCharacteristic? = null
                gatt.services.forEach{  gattService ->
                    //if(ServiceDsrcUtils.isKnowing(gattService.uuid.toString()))
                        //Log.d(TAG,"Service : ${gattService.uuid}")
                    gattService.characteristics.forEach { gattCharacteristic ->
                        if(ServiceDsrcUtils.isKnowing(gattCharacteristic.uuid.toString())){
                            //Log.d(TAG,"Characteristic : ${gattCharacteristic.uuid}\nPermissions : ${gattCharacteristic.permissions}\nProperties : ${gattCharacteristic.properties}")
                            when(gattCharacteristic.uuid.toString()){
                                ServiceDsrcUtils.COMMAND -> {
                                    charac_command = gattCharacteristic
                                }
                                ServiceDsrcUtils.RESPONSE -> {
                                    charac_response = gattCharacteristic
                                }
                                ServiceDsrcUtils.EVENT -> {
                                    charac_event = gattCharacteristic
                                }
                            }
                        }
                    }
                }
                enabledGattNotification(gatt,charac_response!!)
                requestingFastConnection(gatt, charac_command!!)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (characteristic != null) {
                //Log.d(TAG,myCommand.printRequest())
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
                //myCommand.receiveResponse(characteristic.value)
                //Log.d(TAG,myCommand.printResponse())
                var ret = ""
                for(b in characteristic.value){
                    ret += "%02x".format(b) + " "
                }
                Log.d(TAG,"Reçu : $ret")
            }
        }
    }

    private fun showLog(message: String) {
        txt_log.append("$message\n")
    }

    companion object {
        const val TAG = "BLE SCANNING"
        const val PERMISSION_REQUEST_CODE = 101

        const val OK = 0x00 //Command was successfully executed
        const val ALREADY = 0x01 //Operation not performed because system was already in commanded state
        const val BUSY = 0x10 //System is busy and cannot process or evaluate the command
        const val DENIED = 0x20 //Command cannot be executed because configuration or other state that must be resolved by host system
        const val ACCESS_DENIED = 0x21 //Not allowed to access this information
        const val BLOCKED = 0x22 //Command cannot be executed because of transient state [is this really different from busy?]
        const val NOT_IMPLEMENTED = 0x28 //Command cannot be executed because it has not been implemented
        const val ILLEGAL_VALUE = 0x30 //Illegal parameter value (e.g. outside range)
        const val INCONSISTENT_VALUE = 0x31 //Parameter value is inconsistent with other parameter values or configured
        const val SECURITY = 0x40 //Command cannot be executed due to security restrictions
        const val MEMORY_FULL = 0x50 //Command cannot be executed because memory is full
        const val HARDWARE_ERROR = 0x80 //Command cannot be executed because of permanent memory error

    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val intent = Intent(this,DeviceActivity::class.java)
        intent.putExtra(DeviceActivity.EXTRA_DEVICE,set_pairedDevices[position])
        startActivity(intent)
        finish()
    }
}