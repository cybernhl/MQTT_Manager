package idv.neo.utils

 fun Long.toFormattedBytes(): String {
     if (this < 0) return "Invalid size"
     if (this < 1024) return "$this Bytes"
     val kb = this / 1024.0
     if (kb < 1024) return "%.2f KB".format(kb)
     val mb = kb / 1024.0
     if (mb < 1024) return "%.2f MB".format(mb)
     val gb = mb / 1024.0
     return "%.2f GB".format(gb)
 }