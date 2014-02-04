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
 * This stores all of the information necessary to process locale-specific data, such as message strings, dates,
 * currency, etc. The central I class obtains the language settings via a LanguageSettingsProvider, which must be
 * set before any of the methods can be used.
 * <p>
 * Regular Java applications will use a provider that is simply a singleton. Webapps will most likely use a thread-local
 * provider so that a given set of settings are used on each request thread, allowing the webapp to set the locale
 * as each request is processed.
 * </p>
 * <p>
 * Compiled translation files (using GNU gettext conversion to Java) MUST be placed in the com.teamunify.i18n package,
 * and must be named according to the locale naming standards (e.g. messages_en_US). You can change the required package
 * by setting LanguageSetting.translationPackage BEFORE using any i18n facilities.
 * </p>
 *
 * @author tonykay
 * @see com.teamunify.i18n.webapp.AbstractLocaleFilter
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

  private static ResourceBundle loadResourceBundle(String fqcn) {
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
   * @param baseClassPackage The package name of your compiled translation resources (e.g. com.mycomp.i18n)
   * @param l                The locale you want translations for
   * @return An array of translation resources, possibly empty
   */
  public static ResourceBundle findBestTranslation(String baseClassPackage, Locale l) {
    String lang = l.getLanguage();
    String key = l.toString();

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