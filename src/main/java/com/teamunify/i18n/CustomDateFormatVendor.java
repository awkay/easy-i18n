package com.teamunify.i18n;

import java.text.DateFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class on which you can register custom date formats.
 * 
 * @author tonykay
 */
final public class CustomDateFormatVendor {
  public static class DFKey {
    private int formatID;
    private String localeName;

    public DFKey(int formatID, Locale l) {
      if (formatID < 10)
        throw new IllegalArgumentException("Custom date format IDs must be > 10");
      this.formatID = formatID;
      this.localeName = l.toString();
    }
    
    private DFKey(int formatID, String lname) {
      this.formatID = formatID;
      this.localeName = lname;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + formatID;
      result = prime * result + ((localeName == null) ? 0 : localeName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      DFKey other = (DFKey) obj;
      if (formatID != other.formatID)
        return false;
      if (localeName == null) {
        if (other.localeName != null)
          return false;
      } else if (!localeName.equals(other.localeName))
        return false;
      return true;
    }

    public DFKey withoutCountry() {
        final int idx = this.localeName.indexOf("_");
      if(idx > 0) {
        final String lang = this.localeName.substring(0, idx);
        return new DFKey(this.formatID, lang);
      }
      return this;
    }
  }

  private ConcurrentHashMap<DFKey, DateFormat> registry = new ConcurrentHashMap<DFKey, DateFormat>();

  /**
   * Returns a date format object for the given (registered) formatID and locale.
   * 
   * <p>
   * This method attempts to find a match for the exact locale first. If that fails, it attempts dropping the country
   * code (if applicable) and searching for a match on just the language. If that fails, it will return Java
   * DateFormat.getInstance(style) for the alternate specified.
   * 
   * @param formatID
   *          The format ID previously registered
   * @param l
   *          The locale
   * @param alternate
   *          The Java locale-specific DateFormat.(SHORT, MEDIUM, LONG) to return if not found.
   * @return The registered date format for the given locale
   */
  public DateFormat getFormatFor(int formatID, Locale l, int alternate) {
    DFKey key = new DFKey(formatID, l);
    DateFormat rv = registry.get(key);
    if (rv == null) 
      rv = registry.get(key.withoutCountry());
    if (rv == null) 
      rv = DateFormat.getDateInstance(alternate);
    return rv;
  }

  /**
   * Register the given format with this vendor. You must manually remove a registered format, as this function will
   * refuse to overwrite.
   * 
   * @param formatID
   *          The format ID (MUST be > 10)
   * @param l
   *          The locale that this format applies to
   * @param fmt
   *          The format
   * @return True on success, false if there is already one registered
   */
  public boolean registerFormat(int formatID, Locale l, DateFormat fmt) {
    DFKey key = new DFKey(formatID, l);
    return registry.putIfAbsent(key, fmt) == fmt;
  }

  /**
   * Remove a registered format from the vendor.
   * 
   * @param formatID
   *          The format ID
   * @param l
   *          The locale to affect
   */
  public void unregisterFormat(int formatID, Locale l) {
    DFKey key = new DFKey(formatID, l);
    registry.remove(key);
  }
}
