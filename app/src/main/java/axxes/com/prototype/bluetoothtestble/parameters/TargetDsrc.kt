package axxes.com.prototype.bluetoothtestble.parameters

enum class TargetDsrc(val value: Int, val comments: String) {
    HOST_SYSTEM (0, "When sending message to host system"),
    BDO (1, "When sending messages to the BDO system"),
    DSRC (2, "When sending message to DSRC sub-system")
}