package com.livejournal.karino2.zipsourcecodereading.text

/**
 * Created by _ on 2017/10/03.
 */
class ArrayUtils {
    companion object {
        fun idealByteArraySize(need: Int): Int {
            for (i in 4..31)
                if (need <= (1 shl i) - 12)
                    return (1 shl i) - 12

            return need
        }

        fun idealIntArraySize(need: Int): Int {
            return idealByteArraySize(need * 4) / 4
        }

        fun idealCharArraySize(need: Int): Int {
            return idealByteArraySize(need * 2) / 2
        }


    }
}