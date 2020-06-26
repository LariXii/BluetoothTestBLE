package axxes.com.prototype.bluetoothtestble.parameters

import axxes.com.prototype.bluetoothtestble.model.v2.Parameter

/**
 * APPLICATION COMMANDS
 * 6.1 Identification commands
 * This chapter defines command numbers (valid in the context of the command class) and the corresponding command parameters.
 * Protocol version identification
 * Identifies the version of the interface protocol
 */
enum class CommandDsrc2(val value: Int, val category: CategoryDsrc, val requestTarget: TargetDsrc, val requestParamLength: Int, val requestParameters: List<Parameter>?, val responseTarget: TargetDsrc, val responseParamLength: Int, val responseParameters: List<Parameter>?, val description: String) {
    /**
     * IDENTIFICATION COMMANDS
     */

    // Protocol version identification. Identifies the version of the interface protocol
    IDENTIFICATION_INTRF_PRTCL(
        0x01,
        CategoryDsrc.IDENTIFICATION,
        TargetDsrc.BDO, 0, null,
        TargetDsrc.HOST_SYSTEM,6,
            listOf(
                Parameter(
                    "Version",
                    2,
                    "Consisting of minor/major version"
                ),
                Parameter(
                    "FeatureMask",
                    4,
                    "Optional protocol features supported [TBD]"
                )
            ),
        "Protocol version identification"
    ),

    // Provides authenticated hardware identity of a BDO
    // TODO Not yet implement
    /*IDENTIFICATION_HARDWARE(
        0x02,
        CategoryDsrc.IDENTIFICATION,
        Request(TargetDsrc.BDO, 8,
            listOf(
                Parameter("RND1",8,"Remote random number input for signature generated/provided by host system")
            )),
        TODO(),
        "Provides authenticated hardware identity of a BDO"
    ),*/

    // Identifies software version(s) installed in BDO
    IDENTIFICATION_SOFTWARE(
        0x03,
        CategoryDsrc.IDENTIFICATION,
        TargetDsrc.BDO,0, null,
        TargetDsrc.HOST_SYSTEM, 12,
            listOf(
                Parameter(
                    "BDO SW version",
                    4,
                    "Encoding defined by Manufacturer. Part may be used for feature mask"
                ),
                Parameter(
                    "DSRC SW information",
                    8,
                    "Optional extra information describing or defining DSRC SW. Encoding defined by Manufacturer"
                )
            ),
        "Identifies software version(s) installed in BDO"
    ),

    /**
     * DSRC OPERATION COMMANDS
     */

    // Enable/disable DSRC element
    OPERATION_ED_DSRC_EL(
        0x10,
        CategoryDsrc.DSRC_OPERATION,
        TargetDsrc.DSRC, 2,
            listOf(
                Parameter(
                    "EID",
                    1,
                    "Element ID of the application element to change"
                ),
                Parameter(
                    "New state",
                    1,
                    "0x00 – Disable, 0x01 – Enable. Other values reserved"
                )
            ),
        TargetDsrc.HOST_SYSTEM,1,
            listOf(
                Parameter(
                    "ResultCode",
                    1,
                    "Code returned by the command. See technical notice for more explanation"
                )
            ),
        "Enable/disable DSRC element"
    ),

    // Get attribute
    OPERATION_GET_ATTR(
        0x20,
        CategoryDsrc.DSRC_OPERATION,
        TargetDsrc.DSRC,3,
            listOf(
                Parameter(
                    "ValueProperty",
                    1,
                    "Type and representation of value"
                ),
                Parameter(
                    "EID",
                    1,
                    "Element ID of the application element to change"
                ),
                Parameter(
                    "AttributeId",
                    1,
                    "AttributeId or other identification for non-attribute information"
                )
            ),
        TargetDsrc.HOST_SYSTEM,6,
            listOf(
                Parameter(
                    "ResultCode",
                    1,
                    "Code returned by the command. See technical notice for more explanation"
                ),
                Parameter(
                    "ValueProperty",
                    1,
                    "Type and representation of value"
                ),
                Parameter(
                    "EID",
                    1,
                    "Element ID of the application element read from"
                ),
                Parameter(
                    "AttributeId",
                    1,
                    "AttributeId or other identification for non-attribute information"
                ),
                Parameter(
                    "AttrLen",
                    1,
                    "Number of octets in attribute following, including ContainerType"
                ),
                Parameter(
                    "Attribute",
                    -1,
                    "Attribute value including ContainerType and any length determinant"
                )
            ),
        "Get attribute"
    ),

    // Set Attribute
    OPERATION_SET_ATTR(
        0x21,
        CategoryDsrc.DSRC_OPERATION,
        TargetDsrc.DSRC,5,
            listOf(
                Parameter(
                    "ValueProperty",
                    1,
                    "Type and representation of value"
                ),
                Parameter(
                    "EID",
                    1,
                    "Element ID of the application element to change"
                ),
                Parameter(
                    "AttributeId",
                    1,
                    "AttributeId or other identification for non-attribute information"
                ),
                Parameter(
                    "AttrLen",
                    1,
                    "Number of octets in attribute following, including ContainerType"
                ),
                Parameter(
                    "Attribute",
                    -1,
                    "Attribute value including ContainerType and any length determinant"
                )
            ),
        TargetDsrc.HOST_SYSTEM, 4,
            listOf(
                Parameter(
                    "ResultCode",
                    1,
                    "Code returned by the command. See technical notice for more explanation"
                ),
                Parameter(
                    "ValueProperty",
                    1,
                    "Type and representation of value"
                ),
                Parameter(
                    "EID",
                    1,
                    "Element ID of the application element read from"
                ),
                Parameter(
                    "AttributeId",
                    1,
                    "AttributeId or other identification for non-attribute information"
                )
            ),
        "Set attribute"
    ),

    /**
     * SYSTEM CONTROL COMMANDS
     */

    // BLE Fast connection
    CONTROL_BLE_FAST_CONNECTION(
        0x31,
        CategoryDsrc.CONTROL,
        TargetDsrc.BDO,0,null,
        TargetDsrc.HOST_SYSTEM,1,
            listOf(
                Parameter(
                    "ResultCode",
                    1,
                    "Code returned by the command. See technical notice for more explanation"
                )
            ),
        "BLE Fast connection"
    )
}