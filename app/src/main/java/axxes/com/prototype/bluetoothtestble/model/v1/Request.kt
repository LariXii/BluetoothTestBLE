package axxes.com.prototype.bluetoothtestble.model.v1

import android.util.Log
import axxes.com.prototype.bluetoothtestble.parameters.TargetDsrc
import axxes.com.prototype.bluetoothtestble.parameters.TypeCommandDsrc

class Request (val target: TargetDsrc, val paramLength: Int, val parameters: List<Parameter>?) {
    val type = TypeCommandDsrc.REQUEST
    init{
        Log.d("REQUEST","Init Request")
        Log.d("REQUEST","ParamLength : $paramLength,,,,,Parameters : ${parameters.isNullOrEmpty()}")
        if(paramLength > 0 && parameters.isNullOrEmpty()){
            throw IllegalArgumentException("Must have some parameters")
        }
        if(paramLength == 0 && !parameters.isNullOrEmpty()){
            throw IllegalArgumentException("Must have some parameters")
        }
        if(parameters != null){
            var verify = 0
            for(param in parameters){
                verify += param.length
            }
            //TODO handle case param is variable so length = -1
            /*if(verify != paramLength){
                throw IllegalArgumentException("Field paramLength must be egal to parameters length")
            }*/
        }
    }

    fun getRequestByteArray(){

    }
}