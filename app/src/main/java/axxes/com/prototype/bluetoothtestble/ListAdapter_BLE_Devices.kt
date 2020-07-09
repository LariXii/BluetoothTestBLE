package axxes.com.prototype.bluetoothtestble

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class ListAdapter_BLE_Devices(_context: Context,private val _resources: Int, _objects: MutableList<BluetoothDevice>): ArrayAdapter<BluetoothDevice>(_context, _resources, _objects) {
    var devices: MutableList<BluetoothDevice> = _objects

    override fun getView(position: Int, _convertView: View?, parent: ViewGroup): View {
        var convertView: View? = _convertView

        if (convertView == null) {
            val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(_resources, parent, false)
        }

        val device: BluetoothDevice = devices[position]
        val name: String? = device.name
        val address: String? = device.address
        var tv: TextView
        tv = convertView!!.findViewById(R.id.tv_name) as TextView
        if (name != null) {
            tv.text = device.name
        } else {
            tv.text = "No Name"
        }
        tv = convertView.findViewById(R.id.tv_macaddr) as TextView
        if (address != null) {
            tv.text = device.address
        } else {
            tv.text = "No Address"
        }
        return convertView
    }
}