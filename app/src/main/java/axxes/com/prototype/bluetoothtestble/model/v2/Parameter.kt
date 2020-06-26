package axxes.com.prototype.bluetoothtestble.model.v2

import kotlin.math.pow

class Parameter(val field: String, var length: Int, val description: String){
    // length > 0 -> Size of parameter
    // length == -1 -> Size variable, depend of attribute size
    var value: Int? = null
    var bytes: ByteArray? = null
    companion object{
        fun toBytes(param: Parameter): List<Byte> {
            val lengthByte = Byte.SIZE_BITS
            val value = param.value
            val length = param.length
            val listBytes: MutableList<Byte> = mutableListOf()
            for(n in length-1 downTo 0){
                val shift = lengthByte * n
                if (value != null) {
                    if(shift != 0)
                        listBytes.add(value.div(2f.pow(shift)).toByte())
                    else
                        listBytes.add(value.toByte())
                }
                value?.rem(2f.pow(shift))
            }
            return listBytes
        }
        fun toBytes(value: Long, length: Int): List<Byte> {
            val lengthByte = Byte.SIZE_BITS
            val listBytes: MutableList<Byte> = mutableListOf()
            for(n in length-1 downTo  0){
                val shift = lengthByte * n
                if(shift != 0)
                    listBytes.add(value.div(2f.pow(shift)).toByte())
                else
                    listBytes.add(value.toByte())
                value.rem(2f.pow(shift))
            }
            return listBytes
        }
        fun toBytes(value: Int, length: Int): List<Byte> {
            val lengthByte = Byte.SIZE_BITS
            val listBytes: MutableList<Byte> = mutableListOf()
            for(n in length-1 downTo  0){
                val shift = lengthByte * n
                if(shift != 0)
                    listBytes.add(value.div(2f.pow(shift)).toByte())
                else
                    listBytes.add(value.toByte())
                value.rem(2f.pow(shift))
            }
            return listBytes
        }
        fun bytesToInt(param: Parameter, byteArray: List<Byte>){

        }
        fun bytesToInt(byteArray: List<Byte>): Int{
            val l = byteArray.size
            return 1
        }
    }
}