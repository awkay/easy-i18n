package com.teamunify.i18n;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.apache.commons.lang.StringEscapeUtils.escapeJavaScript;
import gnu.gettext.GettextResource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The central translation facility. Use the static methods like tr() to translate messages.
 * 
 * <p>
 * Implementation details:
 * 
 * <ul>
 * <li>Uses GNU gettext java library (libintl.jar) to pull messages from ResourceBundle classes
 * <li>The ResourceBundle classes are <b>generated</b> by the GNU utility msgfmt, and are packaged up into a JAR file by
 * the build process for deployment. They are in gen/ on the project during development.
 * <li>The ServletLocaleFilter is responsible for putting the resource bundle in a place that the translation functions
 * can find it automatically (thread local variable).
 * <li>MessageFormat is used underneath for date, time, currency, and number formatting. @see java.text.MessageFormat.
 * </ul>
 * 
 * <p>
 * <b>IMPORTANT:</b> The language strings MUST be literals. The gettext utilities cannot extract language strings from
 * variables. This isn't that hard. For example, this is fine:
 * 
 * <pre>
 * String s = I.tr(&quot;message&quot;);
 * System.out.println(s);
 * </pre>
 * 
 * but this is <b>not</b>:
 * 
 * <pre>
 * String s = &quot;message&quot;;
 * System.out.println(I.tr(s));
 * </pre>
 * 
 * The reason for this is that the extraction utilities are not doing code analysis. They are doing pattern matching on
 * the function call tr, trc, trf, etc.
 * 
 * To use literal strings in JSPs, escape to Java:
 * 
 * <pre>
 *    <html fragment>
 *    <%= I.tr("message") %>
 *    ...
 * </pre>
 * 
 * <h2>BUILD NOTES</h2>
 * The build system has to do the following tasks in order for translations to work right:
 * <ul>
 * <li>Extract the strings from the java/jsp (uses GNU command line: xgettext)
 * <li>Merge the msgid (keys) into the supported language files (GNU command line util: msgmerge) (there are msgs/*.po
 * files)
 * <li>Compile the .po files into ResourceBundle Java classes (GNU command line util: msgfmt -java2)
 * <li>Deployment has to package up the generated ResourceBundle classes (as a jar file) and include them in the WAR.
 * </ul>
 * 
 * @author tonykay
 * @see java.text.MessageFormat
 * @see ServletLocaleFilter
 * @see ThreadLocalLanguageSetting
 * @see ServletLocaleFilter
 */
public final class I {
  private static BooleanFunction<Date> nullDateTest = new BooleanFunction<Date>() {
    public boolean apply(Date d) {
      return d == null;
    }
  };
  private static Date defaultDate = null;
  private static Logger log = Logger.getLogger(I.class.getName());
  private static ThreadLocalLanguageSetting currentLanguage = new ThreadLocalLanguageSetting();

  /**
   * This is a custom date format. Please use DateFormat.SHORT/MEDIUM/LONG for other formats, which are locale
   * sensitive.
   */
  public static final int TU_STANDARD_DATE_TYPE = 100;

  // visible in other package: e.g., swimoffice JSP's, Beans
  /**
   * Get a DateFormat object that is configured to output dates in a format standardized for our software platform. In
   * particular, a 4-digit year is always displayed.
   * 
   * @return A DateFormat object.
   * @see java.text.DateFormat
   */
  public static DateFormat getTuStandardDateFormat() {
    LanguageSetting s = currentLanguage.get();
    return s.getTuStandardDateFormat();
  }

  /**
   * Get a DateFormat object that is configured to output dates in a format standardized for our software platform. In
   * particular, a 2-digit year is always displayed.
   * 
   * @return A DateFormat object.
   * @see java.text.DateFormat
   */
  public static DateFormat getTuStandardDateFormatShort() {
    LanguageSetting s = currentLanguage.get();
    return s.getTuStandardDateFormatShort();
  }

  /**
   * Get a DateFormat object that is configured to output dates in a format standardized for our software platform. In
   * particular, no year is displayed.
   * 
   * @return A DateFormat object.
   * @see java.text.DateFormat
   */
  public static DateFormat getTuStandardDateFormatNoYear() {
    LanguageSetting s = currentLanguage.get();
    return s.getTuStandardDateFormatNoYear();
  }

  /**
   * Translate a literal message to the user's current language. This method is the most efficient to use, as it merely
   * needs to look up the translation, but need not apply any extra formatting.
   * 
   * @param msg
   *          The english-language message (which will also be the default if there is no translation).
   * @return The translated string, or msg if there is none.
   */
  public static String tr(String msg) {
    return escapeHtml(tru(msg));
  }

  /**
   * Translate a string, but do NOT escape any character entities for HTML. This is useful when embedding translations
   * into javascript.
   * 
   * @param msg
   *          The string to translate.
   * @return The translated string.
   */
  public static String tru(String msg) {
    LanguageSetting s = currentLanguage.get();
    return GettextResource.gettext(s.translation, msg);
  }

  /**
   * Translate a message with wiki markup. These are separate functions that allow you to take the overhead of
   * translating wiki markup only when needed.
   * 
   * The supported wiki markup is documented in the wikified() function.
   * 
   * IMPORTANT: Currently this does not support combinations of modifiers. E.g. You cannot have bold and underline.
   * 
   * @param msg
   *          A string that can contain special markup.
   * @return The translation, with wiki markup turned into HTML
   * @see I#wikified
   */
  public static String trw(String msg) {
    return wikified(tr(msg));
  }

  /**
   * Translate a string in the given context. Useful for resolving the possible differences in things like single words
   * (e.g. noun vs. verb form).
   * 
   * <p>
   * For example:
   * 
   * <pre>
   * I.trc(&quot;adjective&quot;, &quot;Running&quot;); // e.g. Running task
   * I.trc(&quot;single word meaning 'execute a task'&quot;, &quot;Run&quot;);
   * </pre>
   * 
   * @param context
   *          The context (you make this up...it could be a part of speech, definition, etc. e.g. "noun", "adjective",
   *          "execute a task")
   * @param msg
   *          The message to translate
   * @return The translated message, or msg if there is none.
   */
  public static String trc(String context, String msg) {
    LanguageSetting s = currentLanguage.get();
    return escapeHtml(GettextResource.pgettext(s.translation, context, msg));
  }

  /**
   * Just like trc, but wikified.
   * 
   * @see I#wikified
   */
  public static String trcw(String context, String src) {
    return wikified(trc(context, src));
  }

  /**
   * Just like trcf, but wikified.
   * 
   * @see I#wikified
   */
  public static String trcfw(String context, String src, Object... args) {
    return wikified(trcf(context, src, args));
  }

  /**
   * Translate a message that includes placeholders to format.
   * 
   * This function translates the text, then uses java.text.MessageFormat to do the actual parameter substitution.
   * 
   * <p>
   * Examples:
   * 
   * <pre>
   * int n = f();
   * String noun = n &gt; 5 ? I.tr(&quot;noun&quot;, &quot;birds&quot;) : I.tr(&quot;noun&quot;, &quot;cats&quot;);
   * String adj = n &gt; 3 ? I.tr(&quot;adjective&quot;, &quot;tall&quot;) : I.tr(&quot;adjective&quot;, &quot;short&quot;);
   * double amt = 4.99;
   * String message = I.trf(&quot;There are {0, number} {1} in the {2} tree. The {1} is worth {3, number, currency}&quot;, n, noun,
   *                        adj, amt);
   * </pre>
   * 
   * <p>
   * <b>IMPORTANT</b>: Single quotes (apostrophe) and { are SPECIAL here! If you need a literal apostrophe in a
   * formatted string, use two. If you need a literal {, use '{'. @see java.text.MessageFormat.
   * 
   * <pre>
   * String message = I.trf(&quot;Sam''s bucket contains {0, number} {1}.&quot;, n, noun);
   * </pre>
   * 
   * @param msg
   *          The message
   * @param args
   *          A comma-separated list of arguments to put in the placeholders
   * @return The translated string, or a formatted version of msg if there is none.
   * 
   * @see java.text.MessageFormat
   */
  public static String trf(String msg, Object... args) {
    return escapeHtml(trfu(msg, args));
  }

  /**
   * Exactly like trf, but does not escape HTML entities. Useful in javascript where nested quoting is a problem.
   * 
   * @param msg
   * @param args
   * @return
   */
  public static String trfu(String msg, Object... args) {
    LanguageSetting s = currentLanguage.get();
    String xlation = GettextResource.gettext(s.translation, msg);
    s.formatter.applyPattern(xlation);
    return s.formatter.format(args);
  }

  /**
   * Just like trcf, but wikified.
   * 
   * @see I#trf(String,Object...)
   */
  public static String trfw(String src, Object... args) {
    return wikified(trf(src, args));
  }

  /**
   * Translate a message with context and arguments.
   * 
   * @param context
   *          The context.
   * @param msg
   *          The message
   * @param args
   *          The argument to put in msg
   * @return The translation
   * 
   * @see I#trc(String,String)
   * @see I#trf(String,Object...)
   */
  public static String trcf(String context, String msg, Object... args) {
    LanguageSetting s = currentLanguage.get();
    String xlation = GettextResource.pgettext(s.translation, context, msg);
    s.formatter.applyPattern(xlation);
    return escapeHtml(s.formatter.format(args));
  }

  /**
   * Format a string that varies based on plural forms. It supports MessageFormat strings and wiki markup.
   * 
   * <p>
   * Example:
   * 
   * <pre>
   * int nitems;
   * int arg0 = nitems;
   * I.tr_plural(&quot;There is {0} file that matches&quot;, &quot;There are {0} files that match.&quot;, nitems, arg0);
   * </pre>
   * 
   * <p>
   * When nitems is 1, it returns "There is 1 file that matches", otherwise (say for 6) "There are 6 files that match".
   * 
   * @param singular
   *          The singular form (nitems is 1, for English)
   * @param plural
   *          The plural form (nitems is 0 or >1, for English)
   * @param nitems_for_plural_determination
   *          The number of items. This can be modulo 1000, since no known languages have a difference form above about
   *          100. THIS ARGUMENT IS USED FOR PLURAL DETERMINATION ONLY. IT IS NOT PLACED IN THE STRING.
   * @param args
   *          The arguments to use in the formatted string.
   * @return The formatted string
   * @see I#wikified
   */
  public static String tr_plural(String singular, String plural, int nitems_for_plural_determination, Object... args) {
    LanguageSetting s = currentLanguage.get();
    String xlation = GettextResource.ngettext(s.translation, singular, plural, nitems_for_plural_determination);
    s.formatter.applyPattern(xlation);
    return wikified(escapeHtml(s.formatter.format(args)));
  }

  /**
   * Set the current language. In the webapp, this is done via the ServletLocaleFilter. In other places (e.g. cron
   * jobs), you will likely need to set this at the beginning of any thread.
   * 
   * <p>
   * If a session is passed, it will remember the selected language in the session under the "lang" key.
   * 
   * @param session
   *          The current session object. Can be null if you are not running in a webapp.
   * @param name
   *          The language code (two letters, followed by optional _ and two-letter country). E.g. en es de en_US en_AU.
   *          IF YOU DROP THE COUNTRY, IT WILL DEFAULT TO US.
   * @see ServletLocaleFilter
   */
  public static void setLanguage(String name) {
    final String langOnly = name != null && name.contains("_") ? name.substring(0, 2) : name;
    final String countryOnly = name != null && name.contains("_") ? name.substring(3) : "US";
    LanguageSetting setting = new LanguageSetting(new Locale(langOnly, countryOnly));
    log.info("setting language bundle to " + setting.translation.getClass().getName());
    currentLanguage.set(setting);
  }

  /**
   * Return the system-default language code for this installation.
   * 
   * <p>
   * TODO: Make this depend on TuProp.
   * 
   * @return Current returns "en".
   */
  public static LanguageSetting getDefaultLanguage() {
    return new LanguageSetting(Locale.getDefault());
  }

  public static LanguageSetting getCurrentLanguage() {
    return currentLanguage.get();
  }

  /**
   * Test if a language code is supported by our translation files.
   * 
   * @param lang
   *          The language code, e.g. "de".
   * @return True if the language has translations.
   */
  public static boolean supports(String lang) {
    if (lang == null || lang.length() == 0)
      return false;
    String parts[] = lang.split("_");
    if (parts.length == 1)
      return supports(lang, "");
    else if (parts.length == 2)
      return supports(parts[0], parts[1]);

    return false;
  }

  /**
   * Are translations loaded that give at least language-level support for the given locale
   */
  public static boolean supports(Locale l) {
    LanguageSetting setting = new LanguageSetting(l);
    return setting.translation != LanguageSetting.emptyLanguageBundle;
  }

  /**
   * Are translations loaded that give at least language-level support for the given locale
   */
  public static boolean supports(String lang, String country) {
    return supports(new Locale(lang, country));
  }

  private static Pattern boldPattern = Pattern.compile("\\*\\*([^*/_]*)\\*\\*");
  private static Pattern italicPattern = Pattern.compile("//([^/*_]*)//");
  private static Pattern underlinePattern = Pattern.compile("__([^*/_]*)__");
  private static Pattern linebreak = Pattern.compile("_br_");
  private static Pattern redfont = Pattern.compile("_r_([^*/_]*)_r_");
  private static Pattern linkPattern = Pattern.compile("\\[\\[([^|]*)\\|([^]]*)\\]\\]");

  /**
   * Convert wiki markup into HTML markup.
   * 
   * The wiki support in this class honors:
   * 
   * <ul>
   * <li>bold (surround with **)
   * <li>italics (surround with //)
   * <li>underline (surround with __)
   * <li>linebreak (_br_)
   * <li>links ([[URL|text]]).
   * </ul>
   * 
   * @param msg
   *          The message with wiki markup
   * @return The HTML form of the markup.
   */
  public static String wikified(String msg) {
    Matcher m = boldPattern.matcher(msg);
    if (m.find())
      msg = m.replaceAll("<b>$1</b>");
    m = italicPattern.matcher(msg);
    if (m.find())
      msg = m.replaceAll("<i>$1</i>");
    m = underlinePattern.matcher(msg);
    if (m.find())
      msg = m.replaceAll("<u>$1</u>");
    m = redfont.matcher(msg);
    if (m.find())
      msg = m.replaceAll("<font color=red>$1</font>");
    m = linebreak.matcher(msg);
    if (m.find())
      msg = m.replaceAll("<br>");
    m = linkPattern.matcher(msg);
    if (m.find())
      msg = m.replaceAll("<a href=\"$1\">$2</a>");
    return msg;
  }

  /**
   * Convert a date to the specified style.
   * 
   * @param d
   *          The date to format
   * @param style
   *          One of DateFormat formats (e.g. SHORT/LONG/MEDIUM) or DEFAULT.
   * @return the locale-corrected string version of the date.
   */
  public static String dateToString(Date d, int style) {
    if (isNullDate(d))
      return "";
    LanguageSetting s = currentLanguage.get();
    return s.formatterFor(style).format(d);
  }

  /**
   * A handy function to set the default date output type to SHORT
   * 
   * @param d
   *          The date to format
   * @return the locale-corrected string version of the date.
   */
  public static String dateToShortString(Date d) {
    if (isNullDate(d))
      return "";
    else
      return dateToString(d, DateFormat.SHORT);
  }

  /**
   * A handy function to set the default date output type to TU_STANDARD_DATE_TYPE
   * 
   * @param d
   *          The date to format
   * @return the locale-corrected string version of the date.
   */
  public static String dateToString(Date d) {
    if (isNullDate(d))
      return "";
    else
      return dateToString(d, TU_STANDARD_DATE_TYPE);
  }

  /**
   * Convert a date object (which holds significant time as well) to a string that includes the date and time.
   * 
   * @param d
   *          The date
   * @return A timestamp string
   */
  public static String timestampToString(Date d) {
    if (isNullDate(d))
      return "";
    else
      return timestampToString(d, false, true);
  }

  public static String timestampToString(Date d, boolean timeOnly, boolean showSeconds) {
    return timestampToString(d, timeOnly, showSeconds, false);
  }

  public static String timestampToString(Date d, boolean timeOnly, boolean showSeconds, boolean showTimezone) {
    if (isNullDate(d))
      return "";
    LanguageSetting s = currentLanguage.get();
    String strTime =
      (showSeconds ? s.getLongTimeFormat().format(d) : s.getShortTimeFormat().format(d))
          + (showTimezone ? " " + getTimeZone().getDisplayName(getTimeZone().inDaylightTime(d), TimeZone.SHORT) : "");
    if (timeOnly)
      return strTime;
    else
      return dateToString(d, TU_STANDARD_DATE_TYPE) + " " + strTime;
  }

  /**
   * Attempts to parse the given date using the current language's locale, accepting any non-ambiguous date string
   * imaginable in that locale. This function accepts any legal date format for the given locale...
   * 
   * <p>
   * For example, in the US locale, this function will correctly accept ANY of 1/1/93, 01/01/93, 1/1/1993, 1993-01-01,
   * Jan 1, 1993, January 1, 2011.
   * 
   * <p>
   * ALL locales always accept the ISO standard YYYY-MM-DD as a fallback, which is useful when interacting with SQL.
   * 
   * @param source
   *          The source date string. Can be a locale-specific string. Always accepts YYYY-MM-DD as a fallback.
   * @return The date. If parsing fails, the date will be whatever you have your defaultDate set to
   */
  public static Date stringToDate(String source) {
    Date rv = getDefaultDate();
    if (source == null || source.isEmpty())
      return rv;
    ParseException e = null;
    LanguageSetting s = currentLanguage.get();
    for (SimpleDateFormat fmt : s.getDateParsers()) {
      try {
        rv = fmt.parse(source);
        return rv;
      } catch (ParseException e1) {
        e = e1;
      }
    }
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST,
              String.format("Failed to parse date >%s< when using language settings for %s", source,
                            s.locale.getLanguage()), e);
    return rv;
  }

  /**
   * Convert an incoming string that is composed of a date and time into a Date object. This function is designed to be
   * very tolerant of user input. It will always accept ISO format: yyyy-MM-dd hh:mm:ss, but will also accept many
   * localized, non-ambiguous version of a timestamp (mm/dd/yy hh:mm, MMM dd, yyyy hh:mm, etc.)
   * 
   * @param source
   *          The string to interpret.
   * @param defaultDate
   *          date to return if all parsing fails (overrides the global default date for this call).
   * @return The Date the represented in the string, to as much accuracy as can be derived from the string.
   */
  public static Date stringToTimestamp(String source, Date defaultDate) {
    String date;
    String time;
    source = source.trim();
    Matcher m = timestampPattern.matcher(source);
    if (m.matches()) {
      date = m.group(1);
      time = m.group(2);
    } else { // do our best...
      int firstSpace = source.indexOf(' ');
      date = source.substring(0, firstSpace);
      time = source.substring(firstSpace + 1);
    }
    Date d = stringToDate(date);
    if (isNullDate(d))
      d = defaultDate;

    return stringToTime(d, time);
  }

  private final static Pattern timestampPattern = Pattern.compile("^(.*)\\s+(\\d+:\\d+(?:\\d+)?(?:\\s*\\w+)?)$");

  /**
   * Given a reference date, set the time in it using the given string. E.g. treat the date as a pure date (no time),
   * and add the time into it.
   * 
   * @param refDate
   *          The date to use for the date portion
   * @param timeString
   *          The string to parse the time from. If the time does not include an am/pm, then it is assumed to be 24-hour
   *          time.
   * @param defaultDate
   *          The date object to return if time parsing fails. May be refDate. If null, it is treated as NULL_DATE.
   * @return A new date object, with the date from refDate, and time from timeString. Returns defaultDate if time cannot
   *         be parsed.
   */
  @SuppressWarnings("deprecation")
  public static Date stringToTime(Date refDate, String timeString) {
    Date timeDate = new Date(0, 0, 0, 0, 0, 0);

    LanguageSetting s = currentLanguage.get();
    for (DateFormat fmt : new DateFormat[] { s.getLongTimeFormat(), s.getShortTimeFormat(),
      s.getMilitaryTimeFormat(true), s.getMilitaryTimeFormat(false) }) {
      try {
        timeDate = fmt.parse(timeString);
        break;
      } catch (ParseException e) {}
    }

    return new Date(refDate.getYear(), refDate.getMonth(), refDate.getDate(), timeDate.getHours(),
      timeDate.getMinutes(), timeDate.getSeconds());
  }

  /**
   * <p>
   * <b><i>DO NOT USE THIS!</i></b>
   * 
   * <p>
   * This was meant to parse TeamUnify standard date string input: MM/dd/yyyy (en); dd/MM/yyyy (fr); dd.MM.yyyy (de)
   * 
   * <p>
   * However, the stringToDate function will accept <i>any</i> date format, including these. This function is not only
   * unneeded, but does not tolerate legal user input (user's often like to type dates with 2-digit years, and that is
   * unambiguous...no reason to annoy them by not accepting it). Again, do NOT use this function.
   * 
   * It remains here as a reminder that we should be as tolerant of legal user input as possible, (and has been
   * rewritten internally to do so).
   * 
   * @param source
   * @return
   */
  @Deprecated
  public static Date tuStandardDateStringToDate(String source) {
    return stringToDate(source);
  }

  /**
   * Returns the date format string that is preferred for date input in the current locale. (e.g. m/d/yy for English).
   * 
   * <p>
   * This is useful to use on forms so that the user knows at least one legal way to type a date. For example,
   * 
   * <pre>
   *    &lt;input type="text" name="startDate"> <i><%= I.preferredDateFormat() %></i>
   * </pre>
   * 
   * would show the following in the "en" locale:
   * 
   * <br>
   * &nbsp;&nbsp;<input type="text">&nbsp;<i>m/d/yy</i>
   * 
   * @return The format string, for helping the user understand input
   */
  public static Object preferredDateFormat() {
    LanguageSetting s = currentLanguage.get();
    return s.getShortDateParser().toPattern();
  }

  /**
   * Assume that the input is an integer that has been multiplied by a power of 10 sufficient to not lose data. E.g. for
   * the US, this would be dollars * 100.
   * 
   * This function properly divides the integer, and then formats it as a currency.
   * 
   * <b>IMPORTANT</b>: Make sure you convert the <i>amount</i> of the currency, as needed. E.g. You stored dollars, but
   * are showing a value in Euros. This funciton assumes the amount is in the correct, current, locale money unit.
   * 
   * @param amount
   *          The amount of money, as stored in an long (e.g. as cents)
   * @return A string formatted in the current locale that represents the monetary amount.
   */
  public static String longToCurrencyString(long amount) {
    return longToCurrencyString(amount, true);
  }

  /**
   * Assume that the input is an integer that has been multiplied by a power of 10 sufficient to not lose data. E.g. for
   * the US, this would be dollars * 100.
   * 
   * This function properly divides the integer, and then formats it as a currency.
   * 
   * <b>IMPORTANT</b>: Make sure you convert the <i>amount</i> of the currency, as needed. E.g. You stored dollars, but
   * are showing a value in Euros. This funciton assumes the amount is in the correct, current, locale money unit.
   * 
   * @param amount
   *          The amount of money, as stored in an long (e.g. as cents)
   * @return A number that has be correctly divided to have the right number of fractional digits.
   */
  public static String longToCurrencyString(long amount, boolean bCurrencySign) {
    int scale = getFractionDigits();
    int divisor = scale > 0 ? (int) Math.pow(10, scale) : 1;
    BigDecimal b = new BigDecimal(amount);
    b = b.divide(new BigDecimal(divisor), scale, RoundingMode.HALF_UP);
    return numberToCurrencyString(b, bCurrencySign);
  }

  /**
   * Returns default fraction digits according to the locale.
   * 
   * @return The number of floating point digits.
   */
  public static int getFractionDigits() {
    LanguageSetting s = currentLanguage.get();
    Currency c = Currency.getInstance(s.locale);
    int scale = c.getDefaultFractionDigits();
    return scale;
  }

  /**
   * Accepts a floating-point number (usually double or Double) that represents a currency amount. This function
   * properly rounds the amount, and returns a string formatted for the current locale (including a currency symbol).
   * 
   * @param damount
   *          The floating-point amount to format.
   * @return A locale-specific string rounded and formatted to look like a currency.
   */
  public static String numberToCurrencyString(Number damount) {
    return numberToCurrencyString(damount, true);
  }

  /**
   * Get a locale-specific string representing the amount of currency provided. This is identical to
   * numberToCurrencyString(Number), but allows you to turn off the currency symbol.
   * 
   * @param damount
   *          The amount
   * @param bCurrencySign
   *          whether to include the currency symbol in the string.
   * @return The currency string.
   */
  public static String numberToCurrencyString(Number damount, boolean bCurrencySign) {
    LanguageSetting s = currentLanguage.get();
    String rv = "";
    DecimalFormat d = (DecimalFormat) NumberFormat.getCurrencyInstance(s.locale);
    if (damount.doubleValue() < 0) {
      if (d.getNegativePrefix().contains("("))
        d.setNegativePrefix(d.getNegativePrefix().replace("(", "-"));
      if (d.getNegativeSuffix().contains(")"))
        d.setNegativeSuffix(d.getNegativeSuffix().replace(")", ""));
    }

    if (!bCurrencySign) {
      d.setPositivePrefix("");
      d.setPositiveSuffix("");
      d.setNegativePrefix("-");
      d.setNegativeSuffix("");
    }
    rv = d.format(damount.doubleValue());
    rv.replace((char) 160, ' ');
    return escapeHtml(rv);
  }

  /**
   * Take a string that represents a currency amount (with or without the currency symbol), mulitplies it by the correct
   * power of 10 to push the fractional digits into an integer form, and returns the result as a long.
   * <p>
   * E.g. In US: 100.34 -> 10034<br/>
   * In France/Germany: 100,34 -> 10034<br/>
   * etc.<br/>
   * <p>
   * <b>This function is quite tolerant of user input</b>, and will accept anything that is "normal" when writing a
   * currency in that locale.
   * 
   * <pre>
   * // locale is en
   * I.currencyStringToLong(&quot;$1,345.66&quot;, 0L); // returns 134566
   * I.currencyStringToLong(&quot;1345.66&quot;, 0L); // returns 134566
   * // locale is fr
   * I.currencyStringToLong(&quot;1 345,66&quot;, 0L); // returns 134566
   * I.currencyStringToLong(&quot;1345.66 &amp;euro&quot;, 0L); // returns 134566
   * </pre>
   * 
   * @param amount
   *          The string representing a user-input amount of currency.
   * @param defaultValue
   *          The value to return if the parsing fails.
   * @return A long, multiplied by the correct power of 10 for the current fractional storage for the currency.
   */
  public static long currencyStringToLong(String amount, long defaultValue) {
    Number n = currencyStringToNumber(amount, new Long(defaultValue));
    int scale = getFractionDigits();
    int multiple = scale > 0 ? (int) Math.pow(10, scale) : 1;
    BigDecimal b = new BigDecimal(n.toString());
    return b.multiply(new BigDecimal(multiple)).longValue();
  }

  /**
   * Parse the given locale-specific currency string, and return a Number that represents the amount. The Number object
   * then easily allows conversion to primitives or even BigDecimal.
   * <p>
   * Use preferredCurrencyFormat() to get a help string that indicates the preferred input format for the currency.
   * <p>
   * <b>This function is quite tolerant of user input</b>, and will accept anything that is "normal" when writing a
   * currency in that locale.
   * 
   * <pre>
   * // locale is en
   * I.currencyStringToNumber(&quot;$1,345.66&quot;, 0); // returns 1345.66
   * I.currencyStringToNumber(&quot;1345.66&quot;, 0); // returns 1345.66
   * // locale is fr
   * I.currencyStringToNumber(&quot;1 345,66&quot;, 0); // returns 1345.66
   * I.currencyStringToNumber(&quot;1345.66 &amp;euro&quot;, 0); // returns 1345.66
   * </pre>
   * 
   * 
   * @param amount
   *          The string (e.g. 100.34) to be parsed
   * @param defaultValue
   *          The Number to return if the parsing fails.
   * @return A Number (e.g. rv.toDouble() == 100.34), or defaultValue if the string isn't understandable.
   */
  public static Number currencyStringToNumber(String amount, Number defaultValue) {
    if (amount == null || amount.isEmpty())
      return defaultValue;
    LanguageSetting s = currentLanguage.get();
    NumberFormat fmt = NumberFormat.getCurrencyInstance(s.locale);
    try {
      DecimalFormat d = (DecimalFormat) fmt;
      DecimalFormatSymbols symbols = d.getDecimalFormatSymbols();
      if (d.getNegativePrefix().length() > 0)
        amount = amount.replace(d.getNegativePrefix(), "-").trim();
      if (d.getNegativeSuffix().length() > 0)
        amount = amount.replace(d.getNegativeSuffix(), "").trim();
      if (d.getPositivePrefix().length() > 0)
        amount = amount.replace(d.getPositivePrefix(), "").trim();
      if (d.getPositiveSuffix().length() > 0)
        amount = amount.replace(d.getPositiveSuffix(), "").trim();
      d.setPositivePrefix("");
      d.setPositiveSuffix("");
      d.setNegativePrefix("-");
      d.setNegativeSuffix("");
      // In french, the official grouping separator is a Unicode thin space...convert ASCII spaces to thin
      // spaces keeps input conversion from failing....
      if (symbols.getGroupingSeparator() == '\u00a0')
        amount = amount.replace(" ", "\u00a0");
      return fmt.parse(amount);
    } catch (ParseException e) {
      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, "Failed to parse currency: " + amount, e);
    }
    return defaultValue;
  }

  /**
   * Get a help string that indicates the desired currency input format. This is useful in UI forms:
   * 
   * <pre>
   *    &lt;input type="text" name="amount"> &lt;%= I.preferredCurrencyFormat() %>
   * </pre>
   * <p>
   * would show something like this:<br>
   * &nbsp;&nbsp;<input type="text">&nbsp;#,###.##
   * 
   * @return A String of the form #,###.##
   */
  public static String preferredCurrencyFormat() {
    LanguageSetting s = currentLanguage.get();
    NumberFormat fmt = NumberFormat.getCurrencyInstance(s.locale);
    StringBuffer rv = new StringBuffer();
    if (fmt instanceof DecimalFormat) {
      DecimalFormat cfmt = ((DecimalFormat) fmt);
      int fractional = cfmt.getMaximumFractionDigits();
      int groupSize = cfmt.getGroupingSize();
      DecimalFormatSymbols symbols = cfmt.getDecimalFormatSymbols();
      rv.append("#");
      rv.append(symbols.getGroupingSeparator());
      for (int i = 0; i < groupSize; i++)
        rv.append("#");
      rv.append(symbols.getDecimalSeparator());
      for (int i = 0; i < fractional; i++)
        rv.append("#");
    }
    return rv.toString();
  }

  /**
   * Parse the given locale-specific number, and return a Number object that represents the value.
   * <p>
   * Use preferredNumberFormat() to get a help string that indicates the preferred input format for numbers.
   * <p>
   * In general, this function is very tolerant of user input. Digit groupings are optional, but the fractional
   * separator must be correct.
   * 
   * <pre>
   * // in en locale
   * I.stringToNumber(&quot;1,534,100.34&quot;, 0); // returns 1534100.34
   * I.stringToNumber(&quot;1534100.34&quot;, 0); // returns 1534100.34
   * // in fr locale
   * I.stringToNumber(&quot;1 534 100,34&quot;, 0); // returns 1534100.34
   * I.stringToNumber(&quot;1534100,34&quot;, 0); // returns 1534100.34
   * </pre>
   * 
   * @param value
   *          The string (e.g. 100.34) to be parsed
   * @param defaultValue
   *          The Number to return if the parsing fails.
   * @return A Number (e.g. rv.toDouble() == 100.34), or defaultValue if the string isn't understandable.
   */
  public static Number stringToNumber(String value, Number defaultValue) {
    if (value == null || value.isEmpty())
      return defaultValue;
    LanguageSetting s = currentLanguage.get();
    NumberFormat fmt = NumberFormat.getInstance(s.locale);
    try {
      value = value.replace(" ", "");
      return fmt.parse(value);
    } catch (ParseException e) {
      log.log(Level.SEVERE, "Failed to parse number: " + value, e);
    }
    return defaultValue;
  }

  /**
   * Returns a help string that indicates the recommended number input format for the current locale. This will be the
   * locale-specific format. The input funcitons all tolerate plain math numbers (without groupings), though the
   * locale-specific fraction separator is required.
   * 
   * @param nFractional
   *          Indicate the number of fractional digits wanted. 0 means you want an integer.
   * @return A string representing the recommended number input for the locale.
   * @see I#preferredCurrencyFormat()
   */
  public static String preferredNumberFormat(int nFractional) {
    LanguageSetting s = currentLanguage.get();
    NumberFormat fmt = NumberFormat.getInstance(s.locale);
    StringBuffer rv = new StringBuffer();
    if (fmt instanceof DecimalFormat) {
      DecimalFormat cfmt = ((DecimalFormat) fmt);
      int groupSize = cfmt.getGroupingSize();
      DecimalFormatSymbols symbols = cfmt.getDecimalFormatSymbols();
      rv.append("#");
      rv.append(symbols.getGroupingSeparator());
      for (int i = 0; i < groupSize; i++)
        rv.append("#");
      if (nFractional > 0) {
        rv.append(symbols.getDecimalSeparator());
        for (int i = 0; i < nFractional; i++)
          rv.append("#");
      }
    }
    return rv.toString();
  }

  /**
   * Convert a number to a string. The returned string is formatted according to the locale to include digit groupings
   * for easy reading.
   * 
   * <pre>
   * // in en locale
   * I.numberToString(1294855.234); // returns &quot;1,294,855.234&quot;
   * // in fr locale
   * I.numberToString(1294855.234); // returns &quot;1 294 855,234&quot;
   * // in de locale
   * I.numberToString(1294855.234); // returns &quot;1.294.855,234&quot;
   * </pre>
   * 
   * @param d
   *          The number to format.
   * @return The number as a string. Tolerates null input (returns 0)
   */
  public static String numberToString(Number d) {
    if (d == null)
      return "0";
    LanguageSetting s = currentLanguage.get();
    NumberFormat fmt = NumberFormat.getInstance(s.locale);
    return fmt.format(d).replace("\u00a0", " ");
  }

  /**
   * Get the currency symbol for the current locale.
   * 
   * @return A string containing the currency symbol in the current locale.
   */
  public static String currencySign() {
    LanguageSetting s = currentLanguage.get();
    NumberFormat fmt = NumberFormat.getCurrencyInstance(s.locale);
    DecimalFormat d = (DecimalFormat) fmt;
    return d.getDecimalFormatSymbols().getCurrencySymbol();
  }

  /**
   * localized the image name: xxxx_de.png; xxxx_fr.png;
   * 
   * <p>
   * <b>IMPORTANT NOTE:</b> In general, text should not be embedded in images, as it makes the application much harder
   * to localize (you must hire a graphic artist to fix all of the images, in addition to the translator needed to
   * translate the text.
   * 
   * <p>
   * A better approach is to leave the text out of the image, and use CSS (or an image API in Java) to overlay text on
   * the image. This way, you can localize the images with simple translations.
   * 
   * <p>
   * <b>TODO</b>: Currently this function is inconsistent with I.tr(). The I.tr() functions will default to the english
   * text if there is no translation, ensuring that something useful is displayed. This function does no such check, and
   * should fall back to the "en" version if the localized version is missing, and should carefully cache the existence
   * of files so that it doesn't generate much filesystem IO.
   * 
   * @param url
   *          A string ending with a . suffix (e.g. x.png)
   * @return A string with the locale inserted (e.g. x_fr.png)
   */
  public static String imageURL(String url) {
    if (currentLanguage.get().locale.getLanguage().equals("en") || url == null || url.length() == 0)
      return url;

    int iIdx = url.lastIndexOf(".");

    if (iIdx == -1)
      return url;
    else
      return url.substring(0, iIdx) + "_" + currentLanguage.get().locale.getLanguage() + url.substring(iIdx);
  }

  /**
   * Use the current locale's understanding of currency to round a double to the correct number of fractional digits.
   * 
   * <p>
   * <b>CAUTION:</b> Currency calculations must be done in a way that is consistent with accounting practices. Usually,
   * this means rounding at the end of a sequence of operations (so that rounding errors don't accumulate). The basic
   * rule is to round any currency amount that becomes visible to the user. So, for example:
   * 
   * <pre>
   * double discount = 0.0123 * amount; // 1.23% discount
   * String userMessage = I.trf(&quot;You get a {0,currency} discount!&quot;, discount); // currency formatting will show it rounded
   * double roundedDiscount = I.roundCurrency(discount);
   * // now use the rounded number, since that is what they expect.
   * double price = price - roundedDiscount;
   * </pre>
   * 
   * @param unroundedNumber
   *          The number to round
   * @return The same value, but rounded to the correct number of significant fractional digits for the locale's
   *         currency.
   */
  public static double roundCurrency(double unroundedNumber) {
    int scale = getFractionDigits();
    double divisor = scale > 0 ? (int) Math.pow(10, scale) : 1.0;
    return Math.round(unroundedNumber * divisor) / divisor;
  }

  /**
   * Translate a number (e.g. 0.35) to a locale-specific percentage (35%). This function shows up to 2 fractional digits
   * of the percentage, but only as many as are present.
   * <p>
   * e.g.
   * 
   * <pre>
   *   // In en locale
   *   fractionalNumberToPercentage(0.88) -> 88%
   *   fractionalNumberToPercentage(0.835) -> 83.5%
   *   fractionalNumberToPercentage(0.04356) -> 4.36%
   * </pre>
   * 
   * @param n
   *          The number
   * @return The string for your locale that represents the percentage form.
   */
  public static String fractionalNumberToPercentage(Number n) {
    Locale l = I.currentLanguage.get().locale;
    NumberFormat fmt = NumberFormat.getPercentInstance(l);
    fmt.setMaximumFractionDigits(2);
    fmt.setMinimumFractionDigits(0);
    return fmt.format(n);
  }

  /**
   * Convert an integer to a percentage. The second argument supports cases where the int has been pre-multiplied (say
   * by 100) in order to store fractional parts, specify that in fractionalDigits, and it will be divided correctly to
   * compensate.
   * <p>
   * e.g.
   * 
   * <pre>
   * intToPercentage(88,0) -> 88%
   * intToPercentage(8835,2) -> 88.35%
   * intToPercentage(88351,3) -> 88.35%
   * intToPercentage(8835678,5) -> 88.36%
   * </pre>
   * 
   * @param n
   *          The integer to represent as a percentage.
   * @param fractionalDigits
   *          The number of fractional digits in the int (using premutliplication). Useful values are 0, 1, or 2. Higher
   *          numbers are supported and will correctly divide, but the later digits will be rounded to 2 fractional
   *          places.
   * @return The localized percentage string.
   */
  public static String intToPercentage(int n, int fractionalDigits) {
    double pct = n / 100.0;
    while (fractionalDigits-- > 0)
      pct /= 10.0;
    return fractionalNumberToPercentage(pct);
  }

  /**
   * Translate a pre-multiplied integer number (e.g. 35) to a locale-specific percentage (35%).
   * 
   * <pre>
   * wholeNumberToPercentage(88) -> 88%
   * </pre>
   * 
   * @param n
   *          The number
   * @return The string for your locale that represents the percentage form.
   */
  public static String wholeNumberToPercentage(int n) {
    return I.trf("{0,number,percent}", n / 100.0);
  }

  /**
   * Returns a full name, properly composed for the locale. NOTE: This function's behavior relies on the correct
   * translation. Make sure you generate the translation files for new locales, and have the translator properly define
   * this order.
   * 
   * @param firstName
   *          The first name.
   * @param lastName
   *          The last name.
   * @return A string with the name composed in the proper order.
   */
  public static String fullName(String firstName, String lastName) {
    // DO NOT EDIT THIS CODE! If there is a problem, edit the translation file to set the proper format for the name.
    return I.trcf("full_name", "{0} {1}", firstName, lastName);
  }

  /**
   * Given a currency string in the current locale, remove all unnessary characters (group separators and currency
   * symbols).
   * 
   * <p>
   * <b>NOTE:</b> Compressed strings like this are less clear to the user, and should be avoided if at all possible. The
   * input functions accept the normal formats, so there is no worry about the extra symbols from a functionality
   * standpoint.
   * 
   * @param str
   *          The currency string
   * @return The compacted currency string
   */
  public static String compressCurrencyString(String str) {
    LanguageSetting s = currentLanguage.get();
    DecimalFormat d = (DecimalFormat) NumberFormat.getCurrencyInstance(s.locale);
    Character c = d.getDecimalFormatSymbols().getGroupingSeparator();
    str = str.replaceAll("\\Q" + c.toString() + "\\E", "");
    String sym = d.getDecimalFormatSymbols().getCurrencySymbol();
    str = str.replaceAll("\\Q" + sym + "\\E", "");
    str = str.replaceAll("&[^;]*;", "");
    return str;
  }

  /**
   * Given a number string in the current locale, remove all unnessary characters (group separators).
   * 
   * <p>
   * <b>NOTE:</b> Compressed strings like this are less clear to the user, and should be avoided if at all possible. The
   * input functions accept the normal formats, so there is no worry about the extra symbols from a functionality
   * standpoint.
   * 
   * @param str
   *          The string
   * @return The compacted string
   */
  public static String compressNumberString(String str) {
    LanguageSetting s = currentLanguage.get();
    DecimalFormat d = (DecimalFormat) NumberFormat.getNumberInstance(s.locale);
    Character c = d.getDecimalFormatSymbols().getGroupingSeparator();
    str = str.replaceAll("\\Q" + c.toString() + "\\E", "");
    String sym = d.getDecimalFormatSymbols().getCurrencySymbol();
    str = str.replaceAll("\\Q" + sym + "\\E", "");
    str = str.replaceAll("&[^;]*;", "");
    str = str.replaceAll(" ", "");
    return str;
  }

  /**
   * Same as longToCurrencyString, but omits all unnecessary symbols (grouping separators and currency symbol)
   * 
   * <p>
   * <b>NOTE:</b> Compressed strings like this are less clear to the user, and should be avoided if at all possible. The
   * input functions accept the normal formats, so there is no worry about the extra symbols from a functionality
   * standpoint.
   * 
   * @see I#longToCurrencyString(long)
   */
  public static String longToCompactCurrencyString(int amount) {
    return compressCurrencyString(longToCurrencyString(amount, false));
  }

  /**
   * Same as numberToCurrencyString, but omits all unnecessary symbols (grouping separators and currency symbol).
   * 
   * <p>
   * <b>NOTE:</b> Compressed strings like this are less clear to the user, and should be avoided if at all possible. The
   * input functions accept the normal formats, so there is no worry about the extra symbols from a functionality
   * standpoint.
   * 
   * @see I#numberToCurrencyString(long)
   */
  public static String numberToCompactCurrencyString(Number amount) {
    return compressCurrencyString(numberToCurrencyString(amount, false));
  }

  /**
   * Same as numberToString, but omits all unnecessary symbols (grouping separators)
   * 
   * <p>
   * <b>NOTE:</b> Compressed strings like this are less clear to the user, and should be avoided if at all possible. The
   * input functions accept the normal formats, so there is no worry about the extra symbols from a functionality
   * standpoint.
   * 
   * @see I#numberToString(long)
   */
  public static String numberToCompactString(Number d) {
    return compressNumberString(numberToString(d));
  }

  /**
   * Get the currnet language setting.
   * 
   * @return languagne, for instance, en, fr, de, ...
   */
  public static String getLanguage() {
    return currentLanguage.get().locale.getLanguage();
  }

  /**
   * Languages deal with compond lists in a sentence differently. For example, in English the proper format is:
   * 
   * <pre>
   *  two items: "a and b"
   *  three or more items: "a, b, c, and d"
   * </pre>
   * <p>
   * However, other languages may not use comma, and may or may not have words that separate the last item from the rest
   * of the list.
   * <p>
   * This function centralizes the handling of proper sentence structure for lists like this. Most list classes (e.g.
   * ArrayList, Vector, TreeSet) have a toArray() method, so this expects an array of strings that are your list.
   * 
   * <p>
   * <b>NOTE: the strings you pass must have been previous translated!</b>. If you have a list of literals, use a
   * pattern like:
   * </p>
   * 
   * <pre>
   * I.localizedStringsAsList(I.tr(&quot;A&quot;), I.tr(&quot;B&quot;), I.tr(&quot;C&quot;));
   * </pre>
   * 
   * Remember, <b>it is impossible for the translation system to translate strings from variables.</b>
   * 
   * @param preTranslatedWords
   *          Words you've already run through tr (as literals). May be null, empty, or singular.
   * @param inclusive
   *          Pass true to use "And", false to use "Or". E.g. A, B, and C vs. A, B, or C.
   * @return A stringified list acceptable for use in the middle of a sentence. e.g. String[] { I.tr("A"), I.tr("B"),
   *         I.tr("B") } -&gt; "A, B, and C". If you pass a null list or empty list, "" is returned. If you pass a list
   *         with a single item, just that item is returned. Never returns null.
   */
  public static String localizedStringsAsList(String preTranslatedWords[], boolean inclusive) {
    String comma = I.trc("The separator for lists in a sentence (e.g. a, b, and c)", ",");
    String justTwo =
      inclusive ? I.trc("a list in a sentence with more exactly two things", "{0} and {1}")
               : I.trc("a list of options in a sentence with exactly two things", "{0} or {1}");
    String compoundList =
      inclusive ? I.trc("ending of list in a sentence with three or more things", "{0}, and {1}")
               : I.trc("ending of list of options in a sentence with three or more things", "{0}, or {1}");

    if (preTranslatedWords == null || preTranslatedWords.length == 0)
      return "";
    if (preTranslatedWords.length == 1)
      return preTranslatedWords[0];
    if (preTranslatedWords.length == 2)
      return I.trf(justTwo, preTranslatedWords[0], preTranslatedWords[1]);

    StringBuilder mainList = new StringBuilder();
    int i = 0;
    for (i = 0; i < preTranslatedWords.length - 1; i++) {
      mainList.append(preTranslatedWords[i]);
      if (i < preTranslatedWords.length - 2) {
        mainList.append(comma);
        mainList.append(' ');
      }
    }

    return I.trf(compoundList, mainList.toString(), preTranslatedWords[i]);
  }

  /**
   * Same as tr, but escaped for inclusion in JavaScript. This method uses apache-commons
   * StringEscapeUtils.escapeJavascript. See the documentation for that for further info.
   * 
   * <p>
   * Essentially, you can use this to get strings that can be embedded within any type of javascript quote. The
   * recommended usage is:
   * 
   * <pre>
   *    ... import com.teamunify.i18n.I and com.teamunify.util.S ...
   *    &lt;script&gt;
   *       var a = <%= S.q(I.trj("message")) %>;
   *       window.alert(a);
   *    &lt;/script&gt;
   * </pre>
   * 
   * @param string
   *          The string to be translated
   * @return The translated and javascript-escaped result
   */
  public static String trj(String string) {
    return escapeJavaScript(tru(string));
  }

  /**
   * Same as trf, but escaped for inclusion in JavaScript. This method uses apache-commons
   * StringEscapeUtils.escapeJavascript. See the documentation for that for further info.
   * <p>
   * Remember that trf treats ' as a special character, and you must double them.
   * 
   * <p>
   * Essentially, you can use this to get strings that can be embedded within any type of javascript quote. The
   * recommended usage is:
   * 
   * <pre>
   *    ... import com.teamunify.i18n.I and com.teamunify.util.S ...
   *    &lt;script&gt;
   *       var a = <%= S.q(I.trfj("There are {0} apples in Jim''s {1}", count, location)) %>;
   *       window.alert(a);
   *    &lt;/script&gt;
   * </pre>
   * 
   * @param msg
   *          The message to be translated
   * @param args
   *          The arguments for the format
   * @return The translated and javascript-escaped result
   */
  public static String trfj(String msg, Object... args) {
    return escapeJavaScript(trfu(msg, args));
  }

  /*
   * Convert a date to a full ISO timestamp with ms accuracy.
   * 
   * @param date The date
   * 
   * @return A String version of it.
   */
  public static String timestampToISOString(Date date) {
    if (date == null)
      return "";
    return MessageFormat.format("{0,date,yyyy-MM-dd} {0,time,HH:mm:ss.S}", date);
  }

  /**
   * Convert an ISO timestamp string into a Date object.
   * 
   * @param iso
   *          The string, in format: yyyy-MM-dd HH:mm:ss.S
   * @param defaultValue
   *          The date to return if parsing fails
   * @return The parsed date, or defaultValue.
   */
  public static Date ISOTimestampStringDate(String iso, Date defaultValue) {
    if (iso == null || iso.isEmpty())
      return defaultValue;
    try {
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S", Locale.ENGLISH);
      return df.parse(iso);
    } catch (Exception e) {
      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, "Unable to parse date: " + iso, e);
    }
    return defaultValue;
  }

  /**
   * Get date format string pattern in yyyy
   * 
   * @return date format in string
   */
  public static String getTuStandardDateFormatPattern() {
    DateFormat fmt = getTuStandardDateFormat();
    if (fmt instanceof SimpleDateFormat) {
      return ((SimpleDateFormat) fmt).toPattern();
    }
    return "MM/dd/yyyy"; // default;
  }

  /**
   * Get date format string pattern in yy
   * 
   * @return date format in string
   */
  public static String getTuStandardDateFormatPatternShort() {
    DateFormat fmt = getTuStandardDateFormatShort();
    if (fmt instanceof SimpleDateFormat) {
      return ((SimpleDateFormat) fmt).toPattern();
    }
    return "MM/dd/yy"; // default;
  }

  /**
   * Get date format string pattern with no year part
   * 
   * @return date format in string
   */
  public static String getTuStandardDateFormatPatternNoYear() {
    DateFormat fmt = getTuStandardDateFormatNoYear();
    if (fmt instanceof SimpleDateFormat) {
      return ((SimpleDateFormat) fmt).toPattern();
    }
    return "MM/dd"; // default;
  }

  /**
   * Get date format for Date Picker.
   * 
   * @return date format, for instance, "mdy", "dmy", and "ymd"
   */
  public static String getDatePickerFormat() {
    DateFormat fmt = getTuStandardDateFormat();
    if (fmt instanceof SimpleDateFormat) {
      String pattern = ((SimpleDateFormat) fmt).toPattern();
      if (pattern.startsWith("dd"))
        return "dmy";
      else if (pattern.startsWith("yy"))
        return "ymd";
    }
    return "mdy"; // default;
  }

  /**
   * Get date separator for Date Picker.
   * 
   * @return either "/", "." or "-"
   */
  public static char getDatePickerSeparator() {
    String today = dateToString(new Date());
    for (int i = 0; i < today.length(); i++) {
      char c = today.charAt(i);
      if (c < '0' || c > '9')
        return c;
    }
    return '/'; // default
  }

  /**
   * Get default timezone; note this is locale independent and is set by the machine (or vm).
   * 
   * <br/>
   * TODO: Allow time zone setup in LanguageSetting
   * 
   * @return timezone object for this machine
   */
  public static TimeZone getTimeZone() {
    TimeZone rv = null;
    try {
      rv = TimeZone.getTimeZone(System.getProperty("user.timezone"));
      System.err.println("tz set to " + rv.getDisplayName());
    } catch (Exception e) {
      rv = TimeZone.getDefault();
      System.err.println("tz not set. Defaulted to " + rv.getDisplayName());
    }
    return rv;
  }

  /**
   * Test if the given Date object is what we consider to be the NULL date...
   * 
   * FIXME: More general mechanism would be nice...
   * 
   * @param d
   */
  public static final boolean isNullDate(Date d) {
    return getNullDateTest().apply(d);
  }

  public static BooleanFunction<Date> getNullDateTest() {
    return nullDateTest;
  }

  /**
   * <p>
   * The nullDateTest is a BooleanFunction. You can define this test, and it will be used by the date parsing routines
   * to determine if an incoming Date object should be treated as the lack of a date. This allows you to use a specific
   * date to represent "Nothing" as opposed to the error-prone value null.
   * 
   * <p>
   * If you set a custom function, be sure you test for null, since it is possible you will be asked if null is a Null
   * Date.
   * 
   * @return
   */
  public static void setNullDateTest(BooleanFunction<Date> nullDateTest) {
    if (nullDateTest == null)
      throw new NullPointerException("Date testing function cannot be null");
    I.nullDateTest = nullDateTest;
  }

  /**
   * The default date can be set globally to avoid the return of null.
   * 
   * @return
   */
  public static Date getDefaultDate() {
    return defaultDate;
  }

  public static void setDefaultDate(Date defaultDate) {
    I.defaultDate = defaultDate;
  }
}
