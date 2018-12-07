package net.researchgate.release

///*
// * This file is part of the gradle-release plugin.
// *
// * (c) Eric Berry
// * (c) ResearchGate GmbH
// *
// * For the full copyright and license information, please view the LICENSE
// * file that was distributed with this source code.
// */
//
//import org.gradle.api.GradleException
//import org.slf4j.Logger
//import java.io.File
//
//class ExecutorKotlin(private val logger: Logger? = null) {
//
//    fun exec(options: Map<Any, Any> = emptyMap(), commands: List<String>): String {
//        val out: StringBuffer = StringBuffer()
//        val err: StringBuffer = StringBuffer()
//
//        val directory: File? = if (options["directory"] != null) options["directory"] as File else null
//        val processEnv = if (options["env"] != null) System.getenv()+(options["env"] as Map<*, *>) else System.getenv()
//
//        logger?.info("Running $commands in [$directory]")
//        val process:Process = commands.execute(processEnv.collect { "$it.key=$it.value" }, directory)
//        process.waitForProcessOutput(out, err)
//        logger?.info("Running $commands produced output: [${out.toString().trim()}]")
//
//        if (process.exitValue()) {
//            def message = "Running $commands produced an error: [${err.toString().trim()}]"
//
//            if (options["failOnStderr"] as boolean) {
//                throw new GradleException (message)
//            } else {
//                logger?.warn(message)
//            }
//        }
//
//        if (options["errorPatterns"] && [out, err] * . toString ().any { String s -> (options["errorPatterns"] as List<String>).any { s.contains(it) } }) {
//            throw  GradleException("${options["errorMessage"] ? options["errorMessage"] as String : "Failed to run [" + commands.join(" ") + "]"} - [$out][$err]")
//        }
//
//        out.toString()
//    }
//}
