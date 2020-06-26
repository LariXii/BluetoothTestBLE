package axxes.com.prototype.bluetoothtestble.model.v2

import android.util.Log
import axxes.com.prototype.bluetoothtestble.parameters.CategoryDsrc
import axxes.com.prototype.bluetoothtestble.parameters.TargetDsrc
import axxes.com.prototype.bluetoothtestble.parameters.TypeCommandDsrc

class Response (private val targetDsrc: TargetDsrc, private val category: CategoryDsrc, private val commandNo: Int, private val paramLength: Int, val parameters: List<Parameter>?) {
    val type = TypeCommandDsrc.RESPONSE
    val trame = TrameDsrc()

    private var isTrameInit = false
    private var isParametersInit = false

    init{

        if(paramLength > 0 && parameters.isNullOrEmpty()){
            throw IllegalArgumentException("Must have some parameters")
        }
        if(paramLength == 0 && !parameters.isNullOrEmpty()){
            throw IllegalArgumentException("Must have some parameters")
        }

    }

    fun initResponse(values: ByteArray){
        initTrame(values)

        if(parameters != null)
            initParameters(values)
    }

    private fun initTrame(values: ByteArray){
        val ite = values.iterator()
        var i = 0
        while(ite.hasNext() && !isTrameInit){
            val listTmp: MutableList<Byte> = mutableListOf()
            val param = trame.parameters[i]

            var currByte = ite.next()
            listTmp.add(currByte)

            if(param.length > 1){
                var n = param.length - 1
                while(ite.hasNext() && n != 0){
                    currByte = ite.next()
                    listTmp.add(currByte)
                    n--
                }
            }

            param.bytes = listTmp.toByteArray()

            if((trame.parameters.size - 1) == i) {
                isTrameInit = true
            }
            i++
        }
    }

    private fun initParameters(values: ByteArray){
        val ite = values.iterator()
        var i = 0

        // Pass trame's bytes
        while(ite.hasNext() && i < trame.size){
            ite.next()
            i++
        }

        i = 0

        while(ite.hasNext() && !isParametersInit){
            val listTmp: MutableList<Byte> = mutableListOf()
            val param = parameters!![i]

            var currByte = ite.next()
            listTmp.add(currByte)

            if(param.length == -1){
                param.length = parameters[i - 1].bytes!![0].toInt()
            }

            if(param.length > 1){
                var n = param.length - 1
                while(ite.hasNext() && n != 0){
                    currByte = ite.next()
                    listTmp.add(currByte)
                    n--
                }
            }

            param.bytes = listTmp.toByteArray()

            if((paramLength - 1) == i) {
                isParametersInit = true
            }
            i++
        }
    }

    override fun toString(): String {
        var ret = "\nResponse : \n"
        for(t in trame.parameters){
            ret += "${t.field} : "
            for(b in t.bytes!!){
                ret += "%02x".format(b) + " "
            }
            ret += "\n"
        }
        if(!parameters.isNullOrEmpty()){
            for(p in parameters){
                ret += "${p.field} : "
                for(b in p.bytes!!){
                    ret += "%02x".format(b) + " "
                }
                ret += "\n"
            }
        }
        return ret
    }

    companion object{
        private const val TARGET = 0
        private const val CATEGORY = 1
        private const val COMMAND = 2
        private const val RESERVED = 3
        private const val PLENGTH = 4
    }
}