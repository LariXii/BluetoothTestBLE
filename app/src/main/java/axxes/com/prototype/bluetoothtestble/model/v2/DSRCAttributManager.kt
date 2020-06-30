package com.example.android.whileinuselocation.manager

import com.example.android.whileinuselocation.model.DSRCAttribut
import com.example.android.whileinuselocation.model.DSRCAttributEID1
import com.example.android.whileinuselocation.model.DSRCAttributEID2

class DSRCAttributManager {
    companion object{
        fun finAttribut(eid: Int, attrId: Int): DSRCAttribut?{
            when(eid){
                1 -> {
                    // Search in EID 1
                    val attrArray = DSRCAttributEID1.values()
                    for(attr in attrArray){
                        if(attr.attribut.attrId == attrId)
                            return attr.attribut
                    }
                }
                2 -> {
                    // Search in EID 2
                    val attrArray = DSRCAttributEID2.values()
                    for(attr in attrArray){
                        if(attr.attribut.attrId == attrId)
                            return attr.attribut
                    }
                }
            }
            return null
        }
    }
}