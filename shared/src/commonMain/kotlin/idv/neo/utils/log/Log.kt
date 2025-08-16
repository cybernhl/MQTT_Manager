package idv.neo.utils.log

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

//FIXME https://github.com/AAkira/Napier
public object Log {
    private var TAG: String = "Please init your TAG"

    public fun init(tag: String? = null) {
        TAG = tag ?: TAG
        Napier.base(DebugAntilog())
    }

    private fun resolveTag(tempTag: String?) = tempTag ?: TAG

    public fun w(msg: String, tag: String? = null) {
        Napier.w(msg, throwable = null, tag = resolveTag(tag))
    }

    public fun i(msg: String, tag: String? = null) {
        Napier.i(msg, throwable = null, tag = resolveTag(tag))
    }

    public fun d(msg: String, tag: String? = null) {
        Napier.d(msg, throwable = null, tag = resolveTag(tag))
    }

    public fun e(msg: String, throwable: Throwable? = null, tag: String? = null) {
        Napier.e(msg, throwable = throwable, tag = resolveTag(tag))
    }
}