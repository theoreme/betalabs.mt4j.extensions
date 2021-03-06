/**
 * This material was prepared as an account of work sponsored by an agency of the United States Government.<br>
 * Neither the United States Government nor the United States Department of Energy, nor any of their employees,<br> 
 * nor any of their contractors, subcontractors or their employees, makes any warranty, express or implied, or<br>
 * assumes any legal liability or responsibility for the accuracy, completeness, or usefulness or any information,<br> 
 * apparatus, product, or process disclosed, or represents that its use would not infringe privately owned rights.
 */
package org.mt4jx.components.visibleComponents.font.fontFactories;

import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;

import org.mt4j.util.font.fontFactories.TTFontFactory;
import org.mt4j.util.logging.ILogger;
import org.mt4j.util.logging.MTLoggerFactory;

/**
 * <p>
 * Extension of TTFontFactory which quickly extracts the font name from a ttf file.
 * </p>
 * 
 * @author R.Scarberry
 */
public class EnhancedTTFontFactory extends TTFontFactory implements IEnhancedFontFactory {

  /** The Constant logger. */
  private static final ILogger LOG = MTLoggerFactory.getLogger(EnhancedTTFontFactory.class.getName());

  static {
    LOG.setLevel(ILogger.WARN);
  }

  // Learned how to do all this by perusing the code of sun.font.TrueTypeFont
  public static final int NAME_TAG = 0x6E616D65; // 'name'

  public static final int TTCF_TAG = 0x74746366; // 'ttcf' - TTC file

  public static final int V1TT_TAG = 0x00010000; // 'v1tt' - Version 1 TT font

  public static final int TRUE_TAG = 0x74727565; // 'true' - Version 2 TT font

  public static final int MS_PLATFORM_ID = 3;

  /* MS locale id for US English is the "default" */
  public static final short ENGLISH_LOCALE_ID = 0x0409; // 1033 decimal

  public static final int FAMILY_NAME_ID = 1;

  public static final int FULL_NAME_ID = 4;

  private static final int TTCHEADERSIZE = 12;

  private static final int DIRECTORYHEADERSIZE = 12;

  private static final int DIRECTORYENTRYSIZE = 16;

  public EnhancedTTFontFactory() {
  }

  /**
   * Extracts the name of the true-type font stored in the specified file.
   * It quickly extracts the name without fully instantiating a font.
   * 
   * @param fontFileName
   *          full path the the file.
   * 
   * @return name of the font or null, if no true-type font is contained
   *         in the file.
   */
  @Override
  public String extractFontName(String fontFileName) {
    try {
      final String[] names = getFontNames(new File(fontFileName), 0);
      if (names != null) {
        return names[1];
      }
    } catch (final Exception e) {
      // Could be either a FontFormatException or an IOException
      LOG.debug("Could not extract font name from " + fontFileName + ": " + e);
    }
    return null;
  }

  public static String[] getFontNames(File ttfFile, int fIndex)
      throws IOException, FontFormatException {

    RandomAccessFile raf = null;
    FileChannel channel = null;
    int fileSize = 0;
    int headerOffset = 0;

    try {
      raf = new RandomAccessFile(ttfFile, "r");
      channel = raf.getChannel();
      fileSize = (int) channel.size();

      ByteBuffer buffer = readBlock(channel, 0, TTCHEADERSIZE);

      switch (buffer.getInt()) {
        case TTCF_TAG:
          buffer.getInt(); // skip TTC version ID
          final int directoryCount = buffer.getInt();
          if (fIndex >= directoryCount) {
            throw new FontFormatException("bad font index: " + fIndex);
          }
          buffer = readBlock(channel, TTCHEADERSIZE + (4 * fIndex), 4);
          headerOffset = buffer.getInt();
          break;
        case V1TT_TAG:
        case TRUE_TAG:
          break;
        default:
          throw new FontFormatException("not a valid true type font file: " + ttfFile);
      }

      /*
       * Now have the offset of this TT font (possibly within a TTC) After
       * the TT version/scaler type field, is the short representing the
       * number of tables in the table directory. The table directory
       * begins at 12 bytes after the header. Each table entry is 16 bytes
       * long (4 32-bit ints)
       */
      buffer = readBlock(channel, headerOffset + 4, 2);
      final int numTables = buffer.getShort();
      final int directoryOffset = headerOffset + DIRECTORYHEADERSIZE;
      final ByteBuffer bbuffer = readBlock(channel, directoryOffset, numTables * DIRECTORYENTRYSIZE);
      final IntBuffer ibuffer = bbuffer.asIntBuffer();
      DirectoryEntry table = null;
      final DirectoryEntry[] tableDirectory = new DirectoryEntry[numTables];
      for (int i = 0; i < numTables; i++) {
        tableDirectory[i] = table = new DirectoryEntry();
        table.tag = ibuffer.get();
        /* checksum */ibuffer.get();
        table.offset = ibuffer.get();
        table.length = ibuffer.get();
        if ((table.offset + table.length) > fileSize) {
          throw new FontFormatException("bad table, tag=" + table.tag);
        }
      }

      final byte[] name = new byte[256];
      buffer = getTableBuffer(channel, tableDirectory, NAME_TAG);

      String familyName = null;
      String fullName = null;

      if (buffer != null) {
        final ShortBuffer sbuffer = buffer.asShortBuffer();
        sbuffer.get(); // format - not needed.
        final short numRecords = sbuffer.get();
        /*
         * The name table uses unsigned shorts. Many of these are known
         * small values that fit in a short. The values that are sizes
         * or offsets into the table could be greater than 32767, so
         * read and store those as ints
         */
        final int stringPtr = sbuffer.get() & 0xffff;
        for (int i = 0; i < numRecords; i++) {
          final short platformID = sbuffer.get();
          if (platformID != MS_PLATFORM_ID) {
            sbuffer.position(sbuffer.position() + 5);
            continue; // skip over this record.
          }
          final short encodingID = sbuffer.get();
          final short langID = sbuffer.get();
          final short nameID = sbuffer.get();
          final int nameLen = (sbuffer.get()) & 0xffff;
          final int namePtr = ((sbuffer.get()) & 0xffff) + stringPtr;
          switch (nameID) {
            case FAMILY_NAME_ID:
              if ((familyName == null) || (langID == ENGLISH_LOCALE_ID)) {
                buffer.position(namePtr);
                buffer.get(name, 0, nameLen);
                familyName = makeString(name, nameLen, encodingID);
              }
              break;
            case FULL_NAME_ID:
              if ((fullName == null) || (langID == ENGLISH_LOCALE_ID)) {
                buffer.position(namePtr);
                buffer.get(name, 0, nameLen);
                fullName = makeString(name, nameLen, encodingID);
              }
              break;
          }
        }
      }

      return new String[] { familyName, fullName };

    } finally {

      if (raf != null) {
        try {
          raf.close();
        } catch (final IOException ioe) {
          // Ignore.
        }
      }
    }
  }

  private static ByteBuffer getTableBuffer(FileChannel channel,
      DirectoryEntry[] entries, int tag) throws IOException {

    DirectoryEntry entry = null;

    for (final DirectoryEntry e : entries) {
      if (e.tag == tag) {
        entry = e;
        break;
      }
    }

    if ((entry == null) || (entry.length == 0)) {
      return null;
    }

    return readBlock(channel, entry.offset, entry.length);

  }

  private static String makeString(byte[] bytes, int len, short encoding) {
    /*
     * Check for fonts using encodings 2->6 is just for some old DBCS fonts,
     * apparently mostly on Solaris. Some of these fonts encode ascii names
     * as double-byte characters. ie with a leading zero byte for what
     * properly should be a single byte-char.
     */
    if ((encoding >= 2) && (encoding <= 6)) {
      final byte[] oldbytes = bytes;
      final int oldlen = len;
      bytes = new byte[oldlen];
      len = 0;
      for (int i = 0; i < oldlen; i++) {
        if (oldbytes[i] != 0) {
          bytes[len++] = oldbytes[i];
        }
      }
    }
    String charset;
    switch (encoding) {
      case 1:
        charset = "UTF-16";
        break; // most common case first.
      case 0:
        charset = "UTF-16";
        break; // symbol uses this
      case 2:
        charset = "SJIS";
        break;
      case 3:
        charset = "GBK";
        break;
      case 4:
        charset = "MS950";
        break;
      case 5:
        charset = "EUC_KR";
        break;
      case 6:
        charset = "Johab";
        break;
      default:
        charset = "UTF-16";
        break;
    }
    try {
      return new String(bytes, 0, len, charset);
    } catch (final UnsupportedEncodingException e) {
      return new String(bytes, 0, len);
    } catch (final Throwable t) {
      return null;
    }
  }

  public static ByteBuffer readBlock(FileChannel channel, int offset,
      int length) throws IOException {
    ByteBuffer bb = null;
    if ((offset + length) <= channel.size()) {
      bb = ByteBuffer.allocate(length);
      channel.position(offset);
      channel.read(bb);
      bb.flip();
    }
    return bb;
  }

  static class DirectoryEntry {
    public int tag;

    public int offset;

    public int length;
  }
}
