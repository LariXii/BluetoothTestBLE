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