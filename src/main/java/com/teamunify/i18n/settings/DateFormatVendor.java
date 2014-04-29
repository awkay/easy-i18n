package com.teamunify.i18n.settings;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class on which you can register custom date formats.
 *
 * @author tonykay
 */
final public class DateFormatVendor {
  public static final int DEFAULT_DATE_FORMAT_ID = 9;
  private ConcurrentHashMap<DFKey, DateFormat> registry = new ConcurrentHashMap<DFKey, DateFormat>();
  private HashMap<DFKey, DateFormat[]> inputRegistry = new HashMap<DFKey, DateFormat[]>();

  /**
   * Returns a date format object for the given (registered) formatID and locale.
   * <p/>
   * <p/>
   * This method attempts to find a match for the exact locale first. If that fails, it attempts dropping the country
   * code (if applicable) and searching for a match on just the language. If that fails, it will return Java
   * DateFormat.getInstance(style) for the alternate specified.
   *
   * @param formatID  The format ID previously registered
   * @param l         The locale
   * @param alternate The Java locale-specific DateFormat.(SHORT, MEDIUM, LONG) to return if not found.
   * @return The registered date format for the given locale, the alternate if necessary, and DateFormat.SHORT if all else fails.
   */
  public DateFormat getFormatFor(int formatID, Locale l, int alternate) {
    DateFormat rv = null;
    if (formatID == DateFormat.SHORT || formatID == DateFormat.LONG || formatID == DateFormat.MEDIUM)
      rv = DateFormat.getDateInstance(formatID, l);
    else {
      DFKey key = new DFKey(formatID, l);
      rv = clonedFormat(registry.get(key));
      if (rv == null)
        rv = clonedFormat(registry.get(key.withoutCountry()));
      if (rv == null && formatID == DEFAULT_DATE_FORMAT_ID)
        return DateFormat.getDateInstance(DateFormat.SHORT, l);
    }
    if (rv == null)
      return getFormatFor(alternate, l, DateFormat.SHORT);
    return rv;
  }

  private DateFormat clonedFormat(DateFormat f) {
    if(f == null) return null;
    return (DateFormat)f.clone();
  }

  /**
   * Register the given format with this vendor. You must manually remove a registered format, as this function will
   * refuse to overwrite.
   *
   * @param formatID     The format ID. MUST be > DEFAULT_DATE_FORMAT_ID
   * @param l            The locale that this format applies to
   * @param fmt          The format
   * @param useWithInput Pass true if you want this format to be accepted for date input.
   * @throws IllegalArgumentException if formatID is too small.
   */
  public void registerFormat(int formatID, Locale l, DateFormat fmt, boolean useWithInput) {
    if (formatID < DEFAULT_DATE_FORMAT_ID)
      throw new IllegalArgumentException(String.format("Custom date format IDs must be greater than DEFAULT_DATE_FORMAT_ID (%d)", DEFAULT_DATE_FORMAT_ID));
    DFKey key = new DFKey(formatID, l);
    registry.put(key, fmt);

    if (useWithInput)
      this.registerInputFormat(l, fmt);
  }

  /**
   * Remove a registered format from the vendor.
   *
   * @param formatID The format ID
   * @param l        The locale to affect
   */
  public void unregisterFormat(int formatID, Locale l) {
    DFKey key = new DFKey(formatID, l);
    registry.remove(key);
  }

  public void registerInputFormat(Locale l, DateFormat fmt) {
    DFKey key = new DFKey(0xFFFF, l);
    synchronized (inputRegistry) {
      DateFormat[] list = inputRegistry.get(l);
      if (list == null) {
        list = new DateFormat[0];
      }
      DateFormat[] newList = Arrays.copyOf(list, list.length + 1);
      newList[list.length] = fmt;
      inputRegistry.put(key, newList);
    }
  }

  private static final DateFormat emptyList[] = new DateFormat[0];

  public DateFormat[] getInputFormats(Locale l) {
    DFKey k = new DFKey(0xFFFF, l);
    DateFormat rv[] = null;
    rv = inputRegistry.get(k);
    if (rv == null)
      rv = inputRegistry.get(k.withoutCountry());
    if (rv == null)
      rv = emptyList;

    return getDateParsers(rv, l);
  }

  DateFormat[] getDateParsers(DateFormat[] customFormats, Locale locale) {
    DateFormat[] dateParsers = new SimpleDateFormat[4 + customFormats.length];
    dateParsers[0] = DateFormat.getDateInstance(DateFormat.SHORT, locale);
    dateParsers[1] = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
    dateParsers[2] = DateFormat.getDateInstance(DateFormat.LONG, locale);
    dateParsers[3] = new SimpleDateFormat("yyyy-MM-dd", locale);

    for (int i = 0; i < customFormats.length; i++)
      dateParsers[4 + i] = clonedFormat(customFormats[i]);

    return dateParsers;
  }
}

class DFKey {
  private int formatID;
  private String localeName;

  public DFKey(int formatID, Locale l) {
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
    if (idx > 0) {
      final String lang = this.localeName.substring(0, idx);
      return new DFKey(this.formatID, lang);
    }
    return this;
  }
}
