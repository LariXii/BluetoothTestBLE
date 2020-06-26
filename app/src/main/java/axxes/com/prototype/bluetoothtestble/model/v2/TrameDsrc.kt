package axxes.com.prototype.bluetoothtestble.model.v2

class TrameDsrc {
    val parameters: List<Parameter> = listOf(
        Parameter("Target", 1, "Identifies that the command is routed to/from DSRC"),
        Parameter("Category", 1, "Categories are used to separate generic and proprietary commands"),
        Parameter("Command", 1, "Command number, valid in the context of command category"),
        Parameter("Reserved", 1, "Reserved for future use, shall be 0"),
        Parameter("ParamLength", 2, "Length of the following parameter")
    )
    val size: Int
    init{
        var tmp = 0
        for(p in parameters){
            tmp += p.length
        }
        size = tmp
    }
}