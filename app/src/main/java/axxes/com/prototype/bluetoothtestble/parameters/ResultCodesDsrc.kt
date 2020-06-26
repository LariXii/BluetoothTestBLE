package axxes.com.prototype.bluetoothtestble.parameters

enum class ResultCodesDsrc (val value: Int, val comments: String) {
    OK (0x00,"Command was successfully executed"),
    ALREADY (0x01,"Operation not performed because system was already in commanded state"),
    BUSY (0x10,"System is busy and cannot process or evaluate the command"),
    DENIED (0x20,"Command cannot be executed because configuration or other state that must be resolved by host system"),
    ACCESS_DENIED (0x21,"Not allowed to access this information"),
    BLOCKED (0x22,"Command cannot be executed because of transient state [is this really different from busy?]"),
    NOT_IMPLEMENTED (0x28,"Command cannot be executed because it has not been implemented"),
    ILLEGAL_VALUE (0x30,"Illegal parameter value (e.g. outside range)"),
    INCONSISTENT_VALUE (0x31,"Parameter value is inconsistent with other parameter values or configured"),
    SECURITY (0x40,"Command cannot be executed due to security restrictions"),
    MEMORY_FULL (0x50,"Command cannot be executed because memory is full"),
    HARDWARE_ERROR (0x80,"Command cannot be executed because of permanent memory error")
}