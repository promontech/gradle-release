package net.researchgate.release.configs


class GitConfig {
    var requireBranch = "master"
    var pushToRemote = "origin" // needs to be def as can be boolean or string
    var pushOptions = emptyList<String>()
    var signTag = false

    /** @deprecated Remove in version 3.0  */
    @Deprecated("You are setting the deprecated and unused option pushToCurrentBranch. You can safely remove it. The deprecated option will be removed in 3.0")
    var pushToCurrentBranch = false
    var pushToBranchPrefix: String = ""
    var commitVersionFileOnly = false

//    fun setProperty(name: String, value: Any) {
//        if (name == "pushToCurrentBranch") {
//            project.logger.warn("You are setting the deprecated and unused option '$name'. You can safely remove it. The deprecated option will be removed in 3.0")
//        }
//            metaClass.setProperty(this, name, value) // TODO tyler examine effect of this
//    }
}
