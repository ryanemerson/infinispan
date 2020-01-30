package org.infinispan.commons.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.infinispan.commons.util.Util;

/**
 * A byte buffer that exposes the internal byte array with minimal copying
 *
 * @author (various)
 * @since 4.0
 */
public class ByteBufferImpl implements ByteBuffer {
   private final byte[] buf;
   private final int offset;
   private final int length;

   public ByteBufferImpl(byte[] buf) {
      this(buf, 0, buf.length);
   }

   public ByteBufferImpl(byte[] buf, int offset, int length) {
      if (buf == null)
         throw new IllegalArgumentException("Null buff not allowed!");
      if (buf.length < offset + length)
         throw new IllegalArgumentException("Incorrect arguments: buff.length"
                                                  + buf.length + ", offset=" + offset +", length="+ length);
      this.buf = buf;
      this.offset = offset;
      this.length = length;
   }

   @Override
   public byte[] getBuf() {
      return buf;
   }

   @Override
   public int getOffset() {
      return offset;
   }

   @Override
   public int getLength() {
      return length;
   }

   @Override
   public ByteBufferImpl copy() {
      byte[] new_buf = buf != null ? new byte[length] : null;
      int new_length = new_buf != null ? new_buf.length : 0;
      if (new_buf != null)
         System.arraycopy(buf, offset, new_buf, 0, length);
      return new ByteBufferImpl(new_buf, 0, new_length);
   }

   @Override
   public String toString() {
      return String.format("ByteBufferImpl{length=%d, offset=%d, bytes=%s}", length, offset, buf == null ? null : Util.hexDump(buf));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ByteBufferImpl)) return false;

      ByteBufferImpl that = (ByteBufferImpl) o;

      if (length != that.length)
         return false;

      for (int i = 0; i < length; i++)
         if (buf[offset + i] != that.buf[that.offset + i])
            return false;

      return true;
   }

   @Override
   public int hashCode() {
      if (buf == null)
         return 0;

      int result = 1;
      for (int i = 0; i < length; i++) {
         result = 31 * result + buf[offset + i];
      }
      return result;
   }

   /**
    * @return an input stream for the bytes in the buffer
    */
   public InputStream getStream() {
      return new ByteArrayInputStream(getBuf(), getOffset(), getLength());
   }

   public java.nio.ByteBuffer toJDKByteBuffer() {
      return java.nio.ByteBuffer.wrap(buf, offset, length);
   }
}
