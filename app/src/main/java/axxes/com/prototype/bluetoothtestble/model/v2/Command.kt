package axxes.com.prototype.bluetoothtestble.model.v2

import android.util.Log
import axxes.com.prototype.bluetoothtestble.parameters.CommandDsrc2

class Command(command: CommandDsrc2, values: List<Int>?) {
    private val TAG = "COMMAND"

    private val requestObject: Request
    private val responseObject: Response

    val request: ByteArray

    init{

        if (!values.isNullOrEmpty()) {
            if(values.size != command.requestParamLength){
                throw IllegalArgumentException("Error, must have either values than parameters")
            }
        }
        else{
            if(command.requestParamLength != 0){
                throw IllegalArgumentException("Error, must have values")
            }
        }

        // Create resquest object
        requestObject = Request(
            command.requestTarget,
            command.category,
            command.value,
            command.requestParamLength,
            command.requestParameters
        )

        if(!values.isNullOrEmpty())
            requestObject.initParametersValues(values)

        request = requestObject.getRequestByteArray()

        // Create response object
        responseObject = Response(
            command.responseTarget,
            command.category,
            command.value,
            command.responseParamLength,
            command.responseParameters
        )
    }

    fun receiveResponse(response: ByteArray){
        responseObject.initResponse(response)
    }

    fun printRequest(): String{
        return requestObject.toString()
    }

    fun printResponse(): String{
        return responseObject.toString()
    }
}
/*
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
        Log.d(TAG,res)*/