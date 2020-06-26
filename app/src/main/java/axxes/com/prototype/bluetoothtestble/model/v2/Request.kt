package axxes.com.prototype.bluetoothtestble.model.v2

import android.util.Log
import axxes.com.prototype.bluetoothtestble.parameters.CategoryDsrc
import axxes.com.prototype.bluetoothtestble.parameters.TargetDsrc
import axxes.com.prototype.bluetoothtestble.parameters.TypeCommandDsrc

class Request (private val targetDsrc: TargetDsrc, private val category: CategoryDsrc, private val commandNo: Int, private val paramLength: Int, private val parameters: List<Parameter>?) {

    private var isTrameInit = false
    private var isParametersInit = false
    private var requestByteArray: ByteArray? = null

    val type = TypeCommandDsrc.REQUEST
    val trame = TrameDsrc()

    init{
        if(paramLength > 0 && parameters.isNullOrEmpty()){
            throw IllegalArgumentException("Must have some parameters")
        }
        if(paramLength == 0 && !parameters.isNullOrEmpty()){
            throw IllegalArgumentException("Must have some parameters")
        }

        initTrame()

    }

    private fun initTrame(){
        trame.parameters[TARGET].value = targetDsrc.value
        trame.parameters[CATEGORY].value = category.value
        trame.parameters[COMMAND].value = commandNo
        trame.parameters[RESERVED].value = 0
        trame.parameters[PLENGTH].value = paramLength
        isTrameInit = true
    }

    fun initParametersValues(values: List<Int>?){
        if(values.isNullOrEmpty() && paramLength == 0){
            isParametersInit = true
            return
        }
        if(values != null && parameters != null){
            for(i in values.indices){
                parameters[i].value = values[i]
            }
            isParametersInit = true
        }
    }

    private fun createRequestByteArray(){
        assert(isTrameInit && isParametersInit)

        val tmpList: MutableList<Byte> = mutableListOf()

        // Add trame's values
        tmpList.addAll(Parameter.toBytes(trame.parameters[TARGET]))
        trame.parameters[TARGET].bytes = Parameter.toBytes(trame.parameters[TARGET]).toByteArray()

        tmpList.addAll(Parameter.toBytes(trame.parameters[CATEGORY]))
        trame.parameters[CATEGORY].bytes = Parameter.toBytes(trame.parameters[CATEGORY]).toByteArray()

        tmpList.addAll(Parameter.toBytes(trame.parameters[COMMAND]))
        trame.parameters[COMMAND].bytes = Parameter.toBytes(trame.parameters[COMMAND]).toByteArray()

        tmpList.addAll(Parameter.toBytes(trame.parameters[RESERVED]))
        trame.parameters[RESERVED].bytes = Parameter.toBytes(trame.parameters[RESERVED]).toByteArray()

        tmpList.addAll(Parameter.toBytes(trame.parameters[PLENGTH]))
        trame.parameters[PLENGTH].bytes = Parameter.toBytes(trame.parameters[PLENGTH]).toByteArray()

        // Add parameters's values
        if(parameters != null){
            for(p in parameters){
                p.bytes = Parameter.toBytes(p).toByteArray()
                tmpList.addAll(Parameter.toBytes(p))
            }
        }

        requestByteArray = tmpList.toByteArray()
    }

    fun getRequestByteArray(): ByteArray{
        if(requestByteArray == null){
            createRequestByteArray()
        }

        return requestByteArray!!
    }

    override fun toString(): String {
        var ret = "\nRequest : \n"
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