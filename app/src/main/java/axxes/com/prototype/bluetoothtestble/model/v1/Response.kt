package axxes.com.prototype.bluetoothtestble.model.v1

import android.util.Log
import axxes.com.prototype.bluetoothtestble.model.v1.Parameter
import axxes.com.prototype.bluetoothtestble.parameters.TargetDsrc
import axxes.com.prototype.bluetoothtestble.parameters.TypeCommandDsrc

class Response (val target: TargetDsrc, val paramLength: Int, val parameters: List<Parameter>?) {
    val type = TypeCommandDsrc.RESPONSE
    init{
        Log.d("RESPONSE","Init response")
    }
}