package com.teamunify.i18n.settings;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This stores the pair of Locale and ResourceBundle. The former is needed by message format to format currency, dates,
 * etc. The latter holds the current translations for all messages. These are stored in thread local variables during
 * request processing.
 * 
 * @see com.teamunify.i18n.webapp.ServletLocaleFilter
 * 
 * @author tonykay
 */
public final class LanguageSetting {
  private static Logger log = LoggerFactory.getLogger(LanguageSetting.class);
  public static String translationPackage = "com.teamunify.i18n";
  public static final ResourceBundle emptyLanguageBundle = new ResourceBundle() {
    @Override
    public Enumeration<String> getKeys() {
      return new Vector<String>().elements();
    }

    @Override
    protected Object handleGetObject(String key) {
      return null;
    }
    
    @Override
    public String toString() { 
      return "EmptyBundle";
    }
  };

  public LanguageSetting(Locale locale) {
    super();
    this.locale = locale;
    this.formatter = new MessageFormat("", locale);

    translation = findBestTranslation(translationPackage, locale);

    DecimalFormat d = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
    currencySymbol = d.getDecimalFormatSymbols().getCurrencySymbol();
  }

  private ResourceBundle loadResourceBundle(String fqcn) {
    try {
      return (ResourceBundle) Class.forName(fqcn).newInstance();
    } catch (Exception e) {
      log.warn(String.format("Could not find resource bundle: %s.", fqcn));
    }
    return null;
  }

  // cache for loaded resource bundles.
  private static HashMap<String, ResourceBundle> translations = new HashMap<String, ResourceBundle>();

  /**
   * Look up the possible translation resources that should be used for the locale.
   * 
   * @param baseClassPackage
   *          The package name of your compiled translation resources (e.g. com.mycomp.i18n)
   * @param l
   *          The locale you want translations for
   * @return An array of translation resources, possibly empty
   */
  public ResourceBundle findBestTranslation(String baseClassPackage, Locale l) {
    String lang = locale.getLanguage();
    String key = locale.toString();

    ResourceBundle rv = translations.get(key);

    if (rv != null) {
      if (log.isDebugEnabled())
        log.debug(String.format("Using preloaded %s for %s", rv.getClass().getSimpleName(), key));
      return rv;
    }

    try {
      rv = loadResourceBundle(baseClassPackage + ".messages_" + key);
      if (rv != null)
        return rv;
      rv = loadResourceBundle(baseClassPackage + ".messages_" + lang);
      if (rv != null)
        return rv;
      // This ensures that we don't keep retrying to load a locale that has failed 
      // to load...it also makes other bits of the API work by providing an empty
      // bundle to look things up against.
      rv = emptyLanguageBundle;
    } finally {
      if (rv != null) {
        if (log.isDebugEnabled())
          log.debug(String.format("Saving bundle %s for %s", rv.getClass().getSimpleName(), key));
        translations.put(key, rv);
      }
    }

    return rv;
  }

  public final Locale locale;
  public final ResourceBundle translation;
  public final MessageFormat formatter;
  public final String currencySymbol;

  public DateFormat formatterFor(int style) {
    switch (style) {
      case DateFormat.SHORT:
        return getShortDateParser();
      case DateFormat.MEDIUM:
        return getMediumDateParser();
      case DateFormat.LONG:
        return getLongDateParser();
      default:
        return null;
    }
  }

  public SimpleDateFormat getShortDateParser() {
    return (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, locale);
  }

  public SimpleDateFormat getMediumDateParser() {
    return (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
  }

  public SimpleDateFormat getLongDateParser() {
    return (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.LONG, locale);
  }

  public SimpleDateFormat[] getDateParsers() {
    SimpleDateFormat[] dateParsers = new SimpleDateFormat[5];
    dateParsers[0] = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, locale);
    dateParsers[1] = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
    dateParsers[2] = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.LONG, locale);
    dateParsers[3] = new SimpleDateFormat("yyyy-MM-dd", locale);
    return dateParsers;
  }

  public DateFormat getShortTimeFormat() {
    return (SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.SHORT);
  }

  public DateFormat getLongTimeFormat() {
    return (SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.MEDIUM);
  }

  public DateFormat getMilitaryTimeFormat(boolean withSeconds) {
    return withSeconds ? new SimpleDateFormat("H:m:s") : new SimpleDateFormat("H:m");
  }
}