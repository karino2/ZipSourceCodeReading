package com.livejournal.karino2.zipsourcecodereading;

/**
 * Created by _ on 2017/08/28.
 */

public class Slice {
    byte[] buf;
    int offset;
    int length;
    public Slice(byte[] buf, int offset, int len) {
        this.buf = buf;
        this.offset = offset;
        this.length = len;
    }

    public void set(byte[] buf, int offset, int len) {
        this.buf = buf;
        this.offset = offset;
        this.length = len;
    }

    static Slice _EMPTY_SLICE = null;
    public static Slice getEmptySlice() {
        if(_EMPTY_SLICE == null){
            _EMPTY_SLICE = new Slice(new byte[]{}, 0, 0);
        }
        return _EMPTY_SLICE;
    }
    public static Slice wrappedBuffer(byte[] buf, int offset, int len) {
        return new Slice(buf, offset, len);
    }
    public static Slice wrappedBuffer(byte[] buf) {
        return new Slice(buf, 0, buf.length);
    }

    public int length() { return length; }
    public byte getByte(int pos) {
        return buf[offset+pos];
    }

    public byte[] getBuf() { return buf; }

    public Slice slice(int offset, int len) {
        return new Slice(buf, offset, len);
    }

}
