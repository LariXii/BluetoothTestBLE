package axxes.com.prototype.bluetoothtestble.model.v1

import android.util.Log
import axxes.com.prototype.bluetoothtestble.parameters.CommandDsrc

class Command(private val command: CommandDsrc, private val values: List<Int>?) {
    private val TAG = "COMMAND"

    private val requestObject = command.request
    private val responseObject = command.response

    private val target: Int = command.request.target.value
    private val category: Int = command.category.value
    private val commandNo: Int = command.value
    private val reserved = 0x00
    private val paramLength: Int = command.request.paramLength
    private var parameters: List<Parameter>? = null

    val request: ByteArray

    init{
        Log.d(TAG,"Init command")

        if (values != null) {
            if(values.size != paramLength){
                throw IllegalArgumentException("Error, must have either values than parameters")
            }
        }

        // Construct list of byte
        val listParamCommand = mutableListOf<Byte>()
        listParamCommand.addAll(listOf(target.toByte(),category.toByte(),commandNo.toByte(),reserved.toByte()))
        listParamCommand.addAll(
            Parameter.toManyBytes(
                paramLength,
                2
            )
        )

        if(command.request.parameters != null && values != null){
            parameters = command.request.parameters
            // Set value for each parameter
            for(n in values.indices){
                parameters!![n].value = values[n]
            }

            for(param in parameters!!){
                if(param.length > 1){
                    listParamCommand.addAll(
                        Parameter.toManyBytes(
                            param
                        )
                    )
                }
                else{
                    listParamCommand.add(param.value!!.toByte())
                }
            }
        }

        request = listParamCommand.toByteArray()
    }

    fun receiveResponse(response: ByteArray){
        var res = ""
        val ite = response.iterator()
        var n = 0
        var t = 0
        while(ite.hasNext()){
            var currByte = ite.next()
            when(n){
                0 -> {
                    res += "Target : ${"%02x".format(currByte)}\n"
                }
                1 -> {
                    res += "Category : ${"%02x".format(currByte)}\n"
                }
                2 -> {
                    res += "Command number : ${"%02x".format(currByte)}\n"
                }
                3 -> {
                    res += "Reserved : ${"%02x".format(currByte)}\n"
                }
                4 -> {
                    res += "Parameter length : ${"%02x".format(currByte)}"
                }
                5 -> {
                    res += "${"%02x".format(currByte)}\n"
                }
                else -> {
                    if(responseObject.paramLength > 0 && t < responseObject.parameters!!.size){
                        Log.d(TAG,"Il y a ${responseObject.paramLength} paramètres et $t on été affiché")
                        var l = responseObject.parameters[t].length
                        if(l == -1){
                            l = responseObject.parameters[t-1].value!!
                        }
                        Log.d(TAG,"Le $n paramètre comprend $l octets")
                        res += "${responseObject.parameters[t].field} : ${"%02x".format(currByte)}"
                        responseObject.parameters[t].value = currByte.toInt()
                        Log.d(TAG,"${responseObject.parameters[t].field} : ${"%02x".format(currByte)}")
                        l--
                        Log.d(TAG,"Il reste $l octet a ce paramètre")
                        while(ite.hasNext() && l != 0){
                            Log.d(TAG,"Recupere next octet")
                            currByte = ite.next()
                            Log.d(TAG," "+"%02x".format(currByte))
                            res += " "+"%02x".format(currByte)
                            l--
                            Log.d(TAG,"Il reste $l octet a ce paramètre")
                        }
                        Log.d(TAG,"Fin")
                        res += "\n"
                        t++
                    }
                }
            }
            n++
        }
        Log.d(TAG,res)
    }
}