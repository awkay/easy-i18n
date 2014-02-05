package com.teamunify.i18n;

import com.teamunify.i18n.escape.EscapeFunction;
import com.teamunify.i18n.settings.BooleanFunction;
import com.teamunify.i18n.settings.GlobalLanguageSettingsProvider;
import com.teamunify.i18n.settings.LanguageSetting;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.apache.commons.lang.StringEscapeUtils.unescapeHtml;
import static org.junit.Assert.*;

public class ITests {
  static {
    LanguageSetting.translationPackage = "i18n.msgs";
    GlobalLanguageSettingsProvider provider = new GlobalLanguageSettingsProvider();
    I.setLanguageSettingsProvider(provider);
    I.setLanguage(Locale.US);
    I.setEscapeFunction(EscapeFunction.EscapeHTML);
  }

  /**
   * This is a custom date format ID.
   */
  public static final int TU_STANDARD_DATE_TYPE = 100;
  public static final int NO_YEAR = 101;

  private static final Calendar c = Calendar.getInstance();
  private static final int NULL_YEAR = 1930;

  static {
    c.set(Calendar.YEAR, NULL_YEAR);
    c.set(Calendar.MONTH, 0);
    c.set(Calendar.DAY_OF_MONTH, 1);
  }

  private static final Date myNullDate = c.getTime();
  private static final Date nulldatePlusSome = new Date(myNullDate.getTime() + 103295);

  @BeforeClass
  public static void csetup() {
    I.addCustomDateFormat(TU_STANDARD_DATE_TYPE, "en", "", "MM/dd/yyyy", true);
    I.addCustomDateFormat(TU_STANDARD_DATE_TYPE, "en", "US", "MM/dd/yyyy", true);
    I.addCustomDateFormat(TU_STANDARD_DATE_TYPE, "en", "AU", "dd/MM/yyyy", true);
    I.addCustomDateFormat(TU_STANDARD_DATE_TYPE, "fr", "", "dd/MM/yyyy", true);
    I.addCustomDateFormat(TU_STANDARD_DATE_TYPE, "de", "", "dd.MM.yyyy", true);
    I.addCustomDateFormat(NO_YEAR, "en", "", "MM/dd", false);
    I.addCustomDateFormat(NO_YEAR, "en", "AU", "d/M", false);
    I.addCustomDateFormat(NO_YEAR, "fr", "", "dd/MM", false);
    I.addCustomDateFormat(NO_YEAR, "de", "", "dd.MM", false);

    I.setDefaultDateFormat("en", "", "MM/dd/yyyy");
    I.setDefaultDateFormat("en", "AU", "dd/MM/yyyy");
    I.setDefaultDateFormat("fr", "", "dd/MM/yyyy");
    I.setDefaultDateFormat("de", "", "dd.MM.yyyy");
  }

  @Before
  public void setup() {
    I.setLanguage(Locale.US);
    restoreNullDate();
  }

  @Test
  public void indicates_which_languages_have_translations() {
    assertTrue(I.supports("fr"));
    assertTrue(I.supports("fr_CA"));
    assertTrue(I.supports("fr", "CA"));
    assertTrue(I.supports("en_AU"));
    assertTrue(I.supports("en", "AU"));
    assertTrue(I.supports("en"));

    assertFalse(I.supports("cz"));
    assertFalse(I.supports("es"));
  }

  @Test
  public void translation_bundles_are_never_null() {
    I.setLanguage("xyz");
    assertNotNull(I.getCurrentLanguage().translation);
  }

  @Test
  public void basic_translations_are_found() {
    I.setLanguage("en");
    assertEquals("Add", I.tr("Add"));
    I.setLanguage("fr");
    assertEquals("Ajouter", I.tr("Add"));
    I.setLanguage("en_AU");
    assertEquals("Put it", I.tr("Add"));
  }

  @Test
  public void the_default_escape_mechanism_is_empty() {
    assertTrue(I.defaultEscapeFunction == EscapeFunction.NoEscape);
  }

  @Test
  public void html_escape_mechanism_works_when_set_as_default_escape() throws Exception {
    final String src = "\"Tony & Joshua\" have < \"Donald Trump's\" pinky";
    final String desired = "&quot;Tony &amp; Joshua&quot; have &lt; &quot;Donald Trump's&quot; pinky";

    String result = I.tr(src);
    assertEquals(desired, result);

    result = I.trc("none", src);
    assertEquals(desired, result);

    // In formatted strings, ' and { are special!
    final String fmtsrc = "\"Tony & Joshua\" have < \"Donald Trump''s\" {0}";
    final String fmtdesired = "&quot;Tony &amp; Joshua&quot; have &lt; &quot;Donald Trump's&quot; thumb";

    result = I.trf(fmtsrc, "thumb");
    assertEquals(fmtdesired, result);

    result = I.trcf("none", fmtsrc, "thumb");
    assertEquals(fmtdesired, result);

    final String allSyms = "!@#$%^&*(, args)-_=+`~[]'{'};'':\",<.>/?\\|";
    final String desiredEscapes = "!@#$%^&amp;*(, args)-_=+`~[]{};':&quot;,&lt;.&gt;/?\\|";
    result = I.trf(allSyms);
    assertEquals(result, desiredEscapes);
  }

  @Test
  public void html_wiki_markup_converts_to_html() throws Exception {
    String src = "//Hi//, I __really__ like your **rooster**.";
    String fmtsrc = "//Hi//, I __really__ like your **{0}**.";
    String desiredResult = "<i>Hi</i>, I <u>really</u> like your <b>rooster</b>.";
    String result;

    assertEquals("Hi $1 There", I.wikified("Hi $1 There"));

    // Regular version do NOT do wiki
    result = I.tr(src);
    assertEquals(src, result);

    // wiki versions do
    result = I.trw(src);
    assertEquals(desiredResult, result);
    result = I.trfw(fmtsrc, "rooster");
    assertEquals(desiredResult, result);
    result = I.trcw("none", src);
    assertEquals(desiredResult, result);
    result = I.trcfw("none", fmtsrc, "rooster");
    assertEquals(desiredResult, result);
  }

  @Test
  public void wiki_functions_use_default_escape_mechanism() {
    String src = "//Hi//, I __really__ like Joe's **rooster** & \"other\" {things}";
    String fmtsrc = "//Hi//, I __really__ like Joe''s **{0}** & \"other\" '{'things'}'";
    String nowikiResult = "//Hi//, I __really__ like Joe's **rooster** &amp; &quot;other&quot; {things}";
    String desiredResult = "<i>Hi</i>, I <u>really</u> like Joe's <b>rooster</b> &amp; &quot;other&quot; {things}";
    String result;

    // Regular version so NOT do wiki
    result = I.tr(src);
    assertEquals(nowikiResult, result);

    // wiki versions do
    result = I.trw(src);
    assertEquals(desiredResult, result);
    result = I.trfw(fmtsrc, "rooster");
    assertEquals(desiredResult, result);
    result = I.trcw("none", src);
    assertEquals(desiredResult, result);
    result = I.trcfw("none", fmtsrc, "rooster");
    assertEquals(desiredResult, result);
  }

  @Test
  public void html_wiki_support_handles_links() {
    final String src = "Click [[javascript:void(f(\"test & such\", 0)|Here]] to proceed";
    final String fmtsrc = "Click [[javascript:void(f(\"{0}\", 0)|Here]] to proceed";
    final String desired = "Click <a href=\"javascript:void(f(&quot;test &amp; such&quot;, 0)\">Here</a> to proceed";

    String result = I.trw(src);
    assertEquals(desired, result);

    result = I.trcw("context", src);
    assertEquals(desired, result);

    result = I.trfw(fmtsrc, "test & such");
    assertEquals(desired, result);

    result = I.trcfw("cntxt", fmtsrc, "test & such");
    assertEquals(desired, result);
  }

  public void setupNullDate() {
    I.setNullDateTest(new BooleanFunction<Date>() {
      public boolean apply(Date obj) {
        if (obj == null)
          return true;
        Calendar c = Calendar.getInstance();
        c.setTime(obj);
        return (c.get(Calendar.YEAR) == NULL_YEAR);
      }
    });
    I.setDefaultDate(myNullDate);
  }

  public void restoreNullDate() {
    I.setNullDateTest(new BooleanFunction<Date>() {
      public boolean apply(Date obj) {
        return obj == null;
      }
    });
    I.setDefaultDate(null);
  }

  @Test
  public void date_conversion_to_string_works_for_locales() {
    final Date d = makeDate(1, 3, 1999);

    I.setLanguage("en");
    assertEquals("1/3/99", I.dateToString(d, DateFormat.SHORT));
    assertEquals("Jan 3, 1999", I.dateToString(d, DateFormat.MEDIUM));
    assertEquals("January 3, 1999", I.dateToString(d, DateFormat.LONG));
    assertNotSame("", I.dateToString(nulldatePlusSome, DateFormat.SHORT));
    assertNotSame("", I.dateToString(nulldatePlusSome, DateFormat.MEDIUM));
    assertNotSame("", I.dateToString(nulldatePlusSome, DateFormat.LONG));
    setupNullDate();
    assertEquals("", I.dateToString(nulldatePlusSome, DateFormat.SHORT));
    assertEquals("", I.dateToString(nulldatePlusSome, DateFormat.MEDIUM));
    assertEquals("", I.dateToString(nulldatePlusSome, DateFormat.LONG));
    assertEquals("", I.dateToString(null, DateFormat.SHORT));
    assertEquals("", I.dateToString(null, DateFormat.MEDIUM));
    assertEquals("", I.dateToString(null, DateFormat.LONG));
    I.setLanguage("fr_FR");
    assertEquals("03/01/99", I.dateToString(d, DateFormat.SHORT));
    assertEquals("3 janv. 1999", I.dateToString(d, DateFormat.MEDIUM));
    assertEquals("3 janvier 1999", I.dateToString(d, DateFormat.LONG));
    restoreNullDate();
  }

  @Test
  public void date_input_always_accepts_iso_format() {
    final Date targetDate = makeDate(4, 2, 1997);
    I.setLanguage("en");
    assertEquals(targetDate, I.stringToDate("1997-04-02"));
    I.setLanguage("fr_FR");
    assertEquals(targetDate, I.stringToDate("1997-04-02"));
  }

  @Test
  public void date_input_returns_predefined_null_date_on_parsing_error() {
    I.setLanguage("en");
    I.setDefaultDate(myNullDate);
    Date val = I.stringToDate("199704-020");
    assertEquals(myNullDate, val);
  }

  @Test
  public void date_input_accepts_all_possible_predefined_formats() {
    final Date d = makeDate(1, 3, 2003);

    I.setDefaultDate(null);
    assertEquals(null, I.stringToDate(null));
    assertEquals(null, I.stringToDate(""));
    setupNullDate();
    assertEquals(myNullDate, I.stringToDate(null));
    assertEquals(myNullDate, I.stringToDate(""));
    restoreNullDate();

    I.setLanguage("en");
    assertEquals(d, I.stringToDate("1/3/03"));
    assertEquals(d, I.stringToDate("1/3/2003"));
    assertEquals(d, I.stringToDate("01/03/03"));
    assertEquals(d, I.stringToDate("01/03/2003"));
    assertEquals(d, I.stringToDate("2003-01-03"));
    assertEquals(d, I.stringToDate("Jan 3, 2003"));
    assertEquals(d, I.stringToDate("January 3, 2003"));
    I.setLanguage("fr_FR");
    assertEquals(d, I.stringToDate("3/1/03"));
    assertEquals(d, I.stringToDate("03/01/03"));
    assertEquals(d, I.stringToDate("3/1/2003"));
    assertEquals(d, I.stringToDate("03/01/2003"));
    assertEquals(d, I.stringToDate("2003-01-03"));
    assertEquals(d, I.stringToDate("3 janv. 2003"));
    assertEquals(d, I.stringToDate("3 janvier 2003"));
    I.setLanguage("de_DE");
    assertEquals(d, I.stringToDate("3.1.03"));
    assertEquals(d, I.stringToDate("03.01.03"));
    assertEquals(d, I.stringToDate("3.1.2003"));
    assertEquals(d, I.stringToDate("03.01.2003"));
    assertEquals(d, I.stringToDate("2003-01-03"));
  }

  @Test
  public void preferred_date_format_returns_localized_default_for_current_locale() {
    I.setLanguage("en");
    assertEquals("MM/dd/yyyy", I.preferredDateFormat());
    I.setLanguage("fr_FR");
    assertEquals("jj/MM/aaaa", I.preferredDateFormat());
    I.setLanguage("de_DE");
    assertEquals("tt.MM.uuuu", I.preferredDateFormat());
  }

  @Test
  public void preferred_date_format_can_be_obtained_for_alternate_format_ids() {
    I.setLanguage("en");
    assertEquals("MMMM d, yyyy", I.preferredDateFormat(DateFormat.LONG));
  }

  @Test
  public void preferred_date_format_is_short_for_undefined_format_ids() {
    final int undefinedFormat = 12398745;
    I.setLanguage("en");
    assertEquals("M/d/yy", I.preferredDateFormat(undefinedFormat));
  }

  @Test
  public void currencies_can_be_formatted_from_fixed_point_longs() {
    I.setLanguage("en");
    assertEquals("$1,454,100.34", I.longToCurrencyString(145410034));
    I.setLanguage("fr_FR");
    assertEquals("1&nbsp;454&nbsp;100,34 &euro;", I.longToCurrencyString(145410034));
    I.setLanguage("de_DE");
    assertEquals("1.454.100,34 &euro;", I.longToCurrencyString(145410034));
  }

  @Test
  public void currencies_can_be_formatted_from_bigdecimal() {
    BigDecimal amount = new BigDecimal("1454100.34");
    I.setLanguage("en");
    assertEquals("$1,454,100.34", I.numberToCurrencyString(amount));
    I.setLanguage("fr_FR");
    assertEquals("1&nbsp;454&nbsp;100,34 &euro;", I.numberToCurrencyString(amount));
    I.setLanguage("de_DE");
    assertEquals("1.454.100,34 &euro;", I.numberToCurrencyString(amount));
    I.setLanguage("en_AU");
    assertEquals("$1,454,100.34", I.numberToCurrencyString(amount));
  }

  @Test
  public void currencies_can_be_formatted_from_double() {
    double damount = 1454100.34;
    I.setLanguage("en");
    assertEquals("$1,454,100.34", I.numberToCurrencyString(damount));
    I.setLanguage("fr_FR");
    assertEquals("1&nbsp;454&nbsp;100,34 &euro;", I.numberToCurrencyString(damount));
    I.setLanguage("de_DE");
    assertEquals("1.454.100,34 &euro;", I.numberToCurrencyString(damount));
  }

  @Test
  public void negative_currencies_format_as_expected() {
    // Negative numbers
    I.setLanguage("en");
    assertEquals("-$1,454,100.34", I.longToCurrencyString(-145410034));
    I.setLanguage("fr_FR");
    assertEquals("-1&nbsp;454&nbsp;100,34 &euro;", I.longToCurrencyString(-145410034));
    I.setLanguage("de_DE");
    assertEquals("-1.454.100,34 &euro;", I.longToCurrencyString(-145410034));

    BigDecimal amount = new BigDecimal("-1454100.34");
    I.setLanguage("en");
    assertEquals("-$1,454,100.34", I.numberToCurrencyString(amount));
    I.setLanguage("fr_FR");
    assertEquals("-1&nbsp;454&nbsp;100,34 &euro;", I.numberToCurrencyString(amount));
    I.setLanguage("de_DE");
    assertEquals("-1.454.100,34 &euro;", I.numberToCurrencyString(amount));

    double damount = -1454100.34;
    I.setLanguage("en");
    assertEquals("-$1,454,100.34", I.numberToCurrencyString(damount));
    I.setLanguage("fr_FR");
    assertEquals("-1&nbsp;454&nbsp;100,34 &euro;", I.numberToCurrencyString(damount));
    I.setLanguage("de_DE");
    assertEquals("-1.454.100,34 &euro;", I.numberToCurrencyString(damount));

    // WITHOUT currency symbol
    amount = new BigDecimal("1454100.34");
    I.setLanguage("en");
    assertEquals("1,454,100.34", I.numberToCurrencyString(amount, false));
    I.setLanguage("fr_FR");
    assertEquals("1&nbsp;454&nbsp;100,34", I.numberToCurrencyString(amount, false));
    I.setLanguage("de_DE");
    assertEquals("1.454.100,34", I.numberToCurrencyString(amount, false));
    I.setLanguage("en_AU");
    assertEquals("1,454,100.34", I.numberToCurrencyString(amount, false));

    damount = 1454100.34;
    I.setLanguage("en");
    assertEquals("1,454,100.34", I.numberToCurrencyString(damount, false));
    I.setLanguage("fr_FR");
    assertEquals("1&nbsp;454&nbsp;100,34", I.numberToCurrencyString(damount, false));
    I.setLanguage("de_DE");
    assertEquals("1.454.100,34", I.numberToCurrencyString(damount, false));

    amount = new BigDecimal("-1454100.34");
    I.setLanguage("en");
    assertEquals("-1,454,100.34", I.numberToCurrencyString(amount, false));
    I.setLanguage("fr_FR");
    assertEquals("-1&nbsp;454&nbsp;100,34", I.numberToCurrencyString(amount, false));
    I.setLanguage("de_DE");
    assertEquals("-1.454.100,34", I.numberToCurrencyString(amount, false));

    damount = -1454100.34;
    I.setLanguage("en");
    assertEquals("-1,454,100.34", I.numberToCurrencyString(damount, false));
    I.setLanguage("fr_FR");
    assertEquals("-1&nbsp;454&nbsp;100,34", I.numberToCurrencyString(damount, false));
    I.setLanguage("de_DE");
    assertEquals("-1.454.100,34", I.numberToCurrencyString(damount, false));

  }

  @Test
  public void trailing_zeroes_are_added_as_needed() {
    // Trailing zeroes...
    double damount = 3459.2;
    I.setLanguage("en");
    assertEquals("$3,459.20", I.numberToCurrencyString(damount));
    I.setLanguage("fr_FR");
    assertEquals("3&nbsp;459,20 &euro;", I.numberToCurrencyString(damount));
    I.setLanguage("de_DE");
    assertEquals("3.459,20 &euro;", I.numberToCurrencyString(damount));
    I.setLanguage("en");
    assertEquals("3,459.20", I.numberToCurrencyString(damount, false));
    I.setLanguage("fr_FR");
    assertEquals("3&nbsp;459,20", I.numberToCurrencyString(damount, false));
    I.setLanguage("de_DE");
    assertEquals("3.459,20", I.numberToCurrencyString(damount, false));
  }

  @Test
  public void currency_strings_can_be_converted_to_fixed_point_longs() {
    I.setLanguage("en");
    assertEquals(153410034L, I.currencyStringToLong("$1,534,100.34", 0L));
    assertEquals(153410034L, I.currencyStringToLong("$1534100.34", 0L));
    assertEquals(153410034L, I.currencyStringToLong("1,534,100.34", 0L));
    assertEquals(153410034L, I.currencyStringToLong("1534100.34", 0L));
    I.setLanguage("de_DE");
    assertEquals(153410034L, I.currencyStringToLong("1.534.100,34 €", 0L));
    assertEquals(153410034L, I.currencyStringToLong("1534100,34 €", 0L));
    assertEquals(153410034L, I.currencyStringToLong("1.534.100,34", 0L));
    assertEquals(153410034L, I.currencyStringToLong("1534100,34", 0L));
    I.setLanguage("fr_FR");
    assertEquals(153410034L, I.currencyStringToLong("1534100,34 €", 0L));
    assertEquals(153410034L, I.currencyStringToLong("1 534 100,34 €", 0L));
    assertEquals(153410034L, I.currencyStringToLong("1 534 100,34", 0L));
    assertEquals(153410034L, I.currencyStringToLong("1534100,34", 0L));
    // non-breaking spaces ok from the user...
    assertEquals(153410034L, I.currencyStringToLong(unescapeHtml("1&nbsp;534&nbsp;100,34 &euro;"), 0L));
    assertEquals(153410034L, I.currencyStringToLong(unescapeHtml("1&nbsp;534&nbsp;100,34"), 0L));

    // Negative numbers
    I.setLanguage("en");
    assertEquals(-153410034L, I.currencyStringToLong("-1,534,100.34", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("-1534100.34", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("$-1,534,100.34", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("$-1534100.34", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("-$1,534,100.34", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("-$1534100.34", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("($1,534,100.34)", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("($1534100.34)", 0L));
    I.setLanguage("de_DE");
    assertEquals(-153410034L, I.currencyStringToLong("-1.534.100,34 €", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("-1534100,34 €", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("-1.534.100,34", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("-1534100,34", 0L));
    I.setLanguage("fr_FR");
    assertEquals(-153410034L, I.currencyStringToLong("-1534100,34 €", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("-1 534 100,34€", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("-1 534 100,34", 0L));
    assertEquals(-153410034L, I.currencyStringToLong("-1534100,34", 0L));

  }

  @Test
  public void currency_strings_can_be_converted_to_numbers() {
    I.setLanguage("en");
    assertEquals("10", I.currencyStringToNumber("", 10).toString());
    assertEquals("10", I.currencyStringToNumber(null, 10).toString());
    assertEquals("1534100.34", I.currencyStringToNumber("1,534,100.34", 0L).toString());
    assertEquals("1534100.34", I.currencyStringToNumber("1534100.34", 0L).toString());
    assertEquals("1534100.34", I.currencyStringToNumber("$1,534,100.34", 0L).toString());
    assertEquals("1534100.34", I.currencyStringToNumber("$1534100.34", 0L).toString());
    I.setLanguage("fr_FR");
    assertEquals("1534100.34", I.currencyStringToNumber("1 534 100,34 €", 0L).toString());
    assertEquals("1534100.34", I.currencyStringToNumber("1534100,34 €", 0L).toString());
    assertEquals("1534100.34", I.currencyStringToNumber("1 534 100,34", 0L).toString());
    assertEquals("1534100.34", I.currencyStringToNumber("1534100,34", 0L).toString());
    I.setLanguage("de_DE");
    assertEquals("1534100.34", I.currencyStringToNumber("1.534.100,34 €", 0L).toString());
    assertEquals("1534100.34", I.currencyStringToNumber("1534100,34 €", 0L).toString());
    assertEquals("1534100.34", I.currencyStringToNumber("1.534.100,34", 0L).toString());
    assertEquals("1534100.34", I.currencyStringToNumber("1534100,34", 0L).toString());

    // Negatives
    I.setLanguage("en");
    assertEquals("-1534100.34", I.currencyStringToNumber("-1,534,100.34", 0L).toString());
    assertEquals("-1534100.34", I.currencyStringToNumber("-1534100.34", 0L).toString());
    assertEquals("-1534100.34", I.currencyStringToNumber("-$1,534,100.34", 0L).toString());
    assertEquals("-1534100.34", I.currencyStringToNumber("-$1534100.34", 0L).toString());
    I.setLanguage("fr_FR");
    assertEquals("-1534100.34", I.currencyStringToNumber("-1 534 100,34€", 0L).toString());
    assertEquals("-1534100.34", I.currencyStringToNumber("-1534100,34 €", 0L).toString());
    assertEquals("-1534100.34", I.currencyStringToNumber("-1 534 100,34", 0L).toString());
    assertEquals("-1534100.34", I.currencyStringToNumber("-1534100,34", 0L).toString());
    I.setLanguage("de_DE");
    assertEquals("-1534100.34", I.currencyStringToNumber("-1.534.100,34 €", 0L).toString());
    assertEquals("-1534100.34", I.currencyStringToNumber("-1534100,34 €", 0L).toString());
    assertEquals("-1534100.34", I.currencyStringToNumber("-1.534.100,34", 0L).toString());
    assertEquals("-1534100.34", I.currencyStringToNumber("-1534100,34", 0L).toString());
  }

  @Test
  public void currency_strings_need_not_have_fractional_parts() {
    I.setLanguage("en");
    assertEquals("1534100", I.currencyStringToNumber("1,534,100", 0L).toString());
    assertEquals("1534100", I.currencyStringToNumber("1534100", 0L).toString());
    assertEquals("1534100", I.currencyStringToNumber("$1,534,100", 0L).toString());
    assertEquals("1534100", I.currencyStringToNumber("$1534100", 0L).toString());
    I.setLanguage("fr_FR");
    assertEquals("1534100", I.currencyStringToNumber("1 534 100 €", 0L).toString());
    assertEquals("1534100", I.currencyStringToNumber("1534100 €", 0L).toString());
    assertEquals("1534100", I.currencyStringToNumber("1 534 100", 0L).toString());
    assertEquals("1534100", I.currencyStringToNumber("1534100", 0L).toString());
  }

  @Test
  public void can_give_locale_specific_currency_format() {
    I.setLanguage("en");
    assertEquals("#,###.##", I.preferredCurrencyFormat());
    I.setLanguage("fr_FR");
    assertEquals("#\u00a0###,##", I.preferredCurrencyFormat());
    I.setLanguage("de_DE");
    assertEquals("#.###,##", I.preferredCurrencyFormat());
  }

  @Test
  public void can_give_locale_specific_number_format() {
    I.setLanguage("en");
    assertEquals("#,###.##", I.preferredNumberFormat(2));
    assertEquals("#,###", I.preferredNumberFormat(0));
    I.setLanguage("fr_FR");
    assertEquals("#\u00a0###,##", I.preferredNumberFormat(2));
    assertEquals("#\u00a0###", I.preferredNumberFormat(0));
    I.setLanguage("de_DE");
    assertEquals("#.###,##", I.preferredNumberFormat(2));
    assertEquals("#.###", I.preferredNumberFormat(0));
  }

  @Test
  public void can_convert_localized_strings_to_numbers() {
    I.setLanguage("en");
    assertEquals("100", I.stringToNumber("", 100).toString());
    assertEquals("100", I.stringToNumber(null, 100).toString());
    assertEquals("1534100.34", I.stringToNumber("1,534,100.34", 0L).toString());
    assertEquals("1534100.34", I.stringToNumber("1534100.34", 0L).toString());
    I.setLanguage("en_AU");
    assertEquals("100", I.stringToNumber("", 100).toString());
    assertEquals("100", I.stringToNumber(null, 100).toString());
    assertEquals("1534100.34", I.stringToNumber("1,534,100.34", 0L).toString());
    I.setLanguage("fr_FR");
    assertEquals("1534100.34", I.stringToNumber("1 534 100,34", 0L).toString());
    assertEquals("1534100.34", I.stringToNumber("1534100,34", 0L).toString());
    I.setLanguage("de_DE");
    assertEquals("1534100.34", I.stringToNumber("1.534.100,34", 0L).toString());
    assertEquals("1534100.34", I.stringToNumber("1534100,34", 0L).toString());
  }

  @Test
  public void can_convert_localized_strings_to_negative_numbers() {
    // Negative numbers
    I.setLanguage("en");
    assertEquals("-1534100.34", I.stringToNumber("-1,534,100.34", 0L).toString());
    assertEquals("-1534100.34", I.stringToNumber("-1534100.34", 0L).toString());
    I.setLanguage("fr_FR");
    assertEquals("-1534100.34", I.stringToNumber("-1 534 100,34", 0L).toString());
    assertEquals("-1534100.34", I.stringToNumber("-1534100,34", 0L).toString());
    I.setLanguage("de_DE");
    assertEquals("-1534100.34", I.stringToNumber("-1.534.100,34", 0L).toString());
    assertEquals("-1534100.34", I.stringToNumber("-1534100,34", 0L).toString());
  }

  @Test
  public void tolerates_extra_input_spaces_when_converting_strings_to_numbers() {
    I.setLanguage("en");
    assertEquals("1534100.34", I.stringToNumber(" 1,534,100.34  ", 0L).toString());
    assertEquals("1534100.34", I.stringToNumber("  1534100.34  ", 0L).toString());
    assertEquals("-1534100.34", I.stringToNumber("  - 1,534,100.34  ", 0L).toString());
    assertEquals("-1534100.34", I.stringToNumber(" - 1534100.34  ", 0L).toString());
    I.setLanguage("fr_FR");
    assertEquals("1534100.34", I.stringToNumber(" 1 534 100,34 ", 0L).toString());
    assertEquals("1534100.34", I.stringToNumber("  1534100,34 ", 0L).toString());
    assertEquals("-1534100.34", I.stringToNumber(" -1 534 100,34 ", 0L).toString());
    assertEquals("-1534100.34", I.stringToNumber("  -1534100,34", 0L).toString());
    I.setLanguage("de_DE");
    assertEquals("1534100.34", I.stringToNumber(" 1.534.100,34", 0L).toString());
    assertEquals("1534100.34", I.stringToNumber("   1534100,34", 0L).toString());
    assertEquals("-1534100.34", I.stringToNumber(" - 1.534.100,34", 0L).toString());
    assertEquals("-1534100.34", I.stringToNumber("-1534100,34   ", 0L).toString());
  }

  @Test
  public void converts_numbers_to_localized_strings() {
    I.setLanguage("en");
    assertEquals("0", I.numberToString(null));
    assertEquals("1,534,100.34", I.numberToString(1534100.34));
    assertEquals("1,534,100", I.numberToString(1534100));
    assertEquals("-1,534,100", I.numberToString(-1534100));
    I.setLanguage("fr_FR");
    assertEquals("1 534 100,34", I.numberToString(1534100.34));
    assertEquals("1 534 100", I.numberToString(1534100));
    assertEquals("-1 534 100", I.numberToString(-1534100));
    I.setLanguage("de_DE");
    assertEquals("1.534.100,34", I.numberToString(1534100.34));
    assertEquals("1.534.100", I.numberToString(1534100));
    assertEquals("-1.534.100", I.numberToString(-1534100));
  }

  @Test
  public void can_help_generate_locale_specific_references_to_localized_image_filenames() {
    I.setLanguage("en");
    assertEquals("img/image/submitButton.png", I.imageURL("img/image/submitButton.png"));
    I.setLanguage("fr_FR");
    assertEquals("img/image/submitButton_fr.png", I.imageURL("img/image/submitButton.png"));
    I.setLanguage("de_DE");
    assertEquals("img/image/submitButton_de.png", I.imageURL("img/image/submitButton.png"));
  }

  @Test
  public void custom_date_format_are_used_by_locale() {
    Date d = makeDate(5, 2, 2001);
    I.setLanguage("en");
    assertEquals("05/02/2001", I.dateToString(d, TU_STANDARD_DATE_TYPE));
    assertEquals("", I.dateToString(null, TU_STANDARD_DATE_TYPE));
    I.setLanguage("fr_FR");
    assertEquals("02/05/2001", I.dateToString(d, TU_STANDARD_DATE_TYPE));
    I.setLanguage("de_DE");
    assertEquals("02.05.2001", I.dateToString(d, TU_STANDARD_DATE_TYPE));
    I.setLanguage("en_AU");
    assertEquals("02/05/2001", I.dateToString(d, TU_STANDARD_DATE_TYPE));
  }

  @Test
  public void currency_symbols_can_be_omitted_from_currency_strings() {
    I.setLanguage("en");
    assertEquals("8,888.88", I.longToCurrencyString(888888, false));
    I.setLanguage("fr_FR");
    assertEquals("8&nbsp;888,88", I.longToCurrencyString(888888, false));
    I.setLanguage("de_DE");
    assertEquals("8.888,88", I.longToCurrencyString(888888, false));
    I.setLanguage("en");
    assertEquals("8,888.88", I.numberToCurrencyString(8888.88, false));
    I.setLanguage("fr_FR");
    assertEquals("8&nbsp;888,88", I.numberToCurrencyString(8888.88, false));
    I.setLanguage("de_DE");
    assertEquals("8.888,88", I.numberToCurrencyString(8888.88, false));
  }

  @Test
  public void plural_forms_are_supported() {
    I.setLanguage("en");
    assertEquals("There are 0 apples", I.tr_plural("There is {0} apple", "There are {0} apples", 0, 0));
    assertEquals("There is 1 apple", I.tr_plural("There is {0} apple", "There are {0} apples", 1, 1));
    assertEquals("There are 2 apples", I.tr_plural("There is {0} apple", "There are {0} apples", 2, 2));
  }

  @Test
  public void percentages_are_properly_formatted_by_locale() {
    I.setLanguage("en");
    assertEquals("88%", I.wholeNumberToPercentage(88));
    I.setLanguage("fr_FR");
    assertEquals("88 %", I.wholeNumberToPercentage(88));
    I.setLanguage("de_DE");
    assertEquals("88%", I.wholeNumberToPercentage(88));

    I.setLanguage("en");
    assertEquals("88%", I.fractionalNumberToPercentage(0.88));
    I.setLanguage("fr_FR");
    assertEquals("88 %", I.fractionalNumberToPercentage(0.88));
    I.setLanguage("de_DE");
    assertEquals("88%", I.fractionalNumberToPercentage(0.88));

  }

  @Test
  public void franctional_percentages_are_rounded_to_two_digits() {
    I.setLanguage("en");
    assertEquals("12.25%", I.fractionalNumberToPercentage(0.1225));
    I.setLanguage("fr_FR");
    assertEquals("4,25 %", I.fractionalNumberToPercentage(0.0425));
    I.setLanguage("de_DE");
    assertEquals("11,33%", I.fractionalNumberToPercentage(0.11333));
  }

  @Test
  public void integer_percentages_can_include_user_specified_fractional_digits() {
    I.setLanguage("en");
    assertEquals("88%", I.intToPercentage(88, 0));
    assertEquals("88.3%", I.intToPercentage(883, 1));
    assertEquals("88.35%", I.intToPercentage(8835, 2));
    I.setLanguage("fr_FR");
    assertEquals("88 %", I.intToPercentage(88, 0));
    assertEquals("88,3 %", I.intToPercentage(883, 1));
    assertEquals("88,35 %", I.intToPercentage(8835, 2));
    I.setLanguage("de_DE");
    assertEquals("88%", I.intToPercentage(88, 0));
    assertEquals("88,3%", I.intToPercentage(883, 1));
    assertEquals("88,35%", I.intToPercentage(8835, 2));
  }

  @Test
  public void currency_rounding_is_supported() {
    double d = 123984.3333333333;
    // rounding down
    I.setLanguage("en");
    assertEquals("123984.33", Double.toString(I.roundCurrency(d)));
    I.setLanguage("fr_FR");
    assertEquals("123984.33", Double.toString(I.roundCurrency(d)));
    I.setLanguage("de_DE");
    assertEquals("123984.33", Double.toString(I.roundCurrency(d)));

    // rounding up
    d = 123984.5050001;
    I.setLanguage("en");
    assertEquals("123984.51", Double.toString(I.roundCurrency(d)));
    I.setLanguage("fr_FR");
    assertEquals("123984.51", Double.toString(I.roundCurrency(d)));
    I.setLanguage("de_DE");
    assertEquals("123984.51", Double.toString(I.roundCurrency(d)));

    // rounding right at the border
    d = 123984.505;
    I.setLanguage("en");
    assertEquals("123984.51", Double.toString(I.roundCurrency(d)));
    I.setLanguage("fr_FR");
    assertEquals("123984.51", Double.toString(I.roundCurrency(d)));
    I.setLanguage("de_DE");
    assertEquals("123984.51", Double.toString(I.roundCurrency(d)));

    d = 10.155;
    I.setLanguage("en");
    assertEquals("10.16", Double.toString(I.roundCurrency(d)));
    I.setLanguage("fr_FR");
    assertEquals("10.16", Double.toString(I.roundCurrency(d)));
    I.setLanguage("de_DE");
    assertEquals("10.16", Double.toString(I.roundCurrency(d)));
  }

  @Test
  public void currency_symbol_can_be_obtained_for_locale() {
    I.setLanguage("en");
    assertEquals("$", I.currencySign());
    I.setLanguage("fr_FR");
    assertEquals("\u20ac", I.currencySign());
    I.setLanguage("de_DE");
    assertEquals("\u20ac", I.currencySign());
  }

  @Test
  public void full_name_helper_function_reassembles_name() {
    I.setLanguage("en");
    assertEquals("Sam Iam", I.fullName("Sam", "Iam"));
  }

  @Test
  public void unescaped_tr_does_not_escape() {
    assertEquals("&quot;", I.tr("\""));
    assertEquals("\"", I.tru("\""));
  }

  @Test
  public void unescaped_trf_does_not_escape() {
    assertEquals("&quot;", I.tr("\""));
    assertEquals("This is a \"", I.trfu("This is a {0}", "\""));
  }

  @Test
  public void timestamp_input_is_tolerant_of_various_formats_in_all_locales() {
    Date d = makeTimestamp(3, 4, 2011, 23, 37, 10);
    I.setLanguage("en");
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("3/4/11 11:37 PM", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("03/04/11 11:37 PM", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("Mar 4, 2011 11:37 PM", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("3/4/11 11:37 pm", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("03/04/11 11:37 pm", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("Mar 4, 2011 11:37 pm", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("3/4/11 23:37", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("03/04/11 23:37", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("Mar 4, 2011 23:37", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("03/04/11 11:37 pm", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("03/04/11 11:37 pm", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("2011-03-04 23:37:10", null), true));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("2011-03-04 23:37", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("2011-03-04 2337", null), false));
    I.setLanguage("fr_FR");
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("4/3/11 11:37 pm", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("04/03/11 11:37 pm", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("2011-03-04 23:37:10", null), true));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("2011-03-04 23:37", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("2011-03-04 2337", null), false));
    I.setLanguage("de_DE");
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("4.3.11 11:37 pm", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("04.03.11 11:37 pm", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("2011-03-04 23:37:10", null), true));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("2011-03-04 23:37", null), false));
    assertTrue(areEqualTimestamps(d, I.stringToTimestamp("2011-03-04 2337", null), false));
  }

  @Test
  public void timestamp_output_allows_inclusion_of_various_parts() {
    Date d = makeTimestamp(3, 4, 2011, 23, 37, 10);
    I.setLanguage("en");
    assertEquals("03/04/2011 11:37:10 PM", I.timestampToString(d));
    assertEquals("03/04/2011 11:37 PM", I.timestampToString(d, false, false));
    assertEquals("11:37 PM", I.timestampToString(d, true, false));
    assertEquals("11:37:10 PM", I.timestampToString(d, true, true));
    I.setLanguage("fr_FR");
    assertEquals("04/03/2011 11:37:10 PM", I.timestampToString(d));
    assertEquals("04/03/2011 11:37 PM", I.timestampToString(d, false, false));
    assertEquals("11:37 PM", I.timestampToString(d, true, false));
    assertEquals("11:37:10 PM", I.timestampToString(d, true, true));
    I.setLanguage("de_DE");
    assertEquals("04.03.2011 11:37:10 PM", I.timestampToString(d));
    assertEquals("04.03.2011 11:37 PM", I.timestampToString(d, false, false));
    assertEquals("11:37 PM", I.timestampToString(d, true, false));
    assertEquals("11:37:10 PM", I.timestampToString(d, true, true));
    assertEquals("11:37:10 PM PST", I.timestampToString(d, true, true, true));
    System.setProperty("user.timezone", "Australia/North");
    assertEquals("11:37:10 PM CST", I.timestampToString(d, true, true, true));
  }

  @Test
  public void compact_currency_format_omits_unnecessary_separators() {
    I.setLanguage("en");
    assertEquals("8888.88", I.longToCompactCurrencyString(888888));
    I.setLanguage("fr_FR");
    assertEquals("8888,88", I.longToCompactCurrencyString(888888));
    I.setLanguage("de_DE");
    assertEquals("8888,88", I.longToCompactCurrencyString(888888));
    I.setLanguage("en");
    assertEquals("8888.88", I.numberToCompactCurrencyString(8888.88));
    I.setLanguage("fr_FR");
    assertEquals("8888,88", I.numberToCompactCurrencyString(8888.88));
    I.setLanguage("de_DE");
    assertEquals("8888,88", I.numberToCompactCurrencyString(8888.88));
  }

  @Test
  public void compact_number_format_omits_unnecessary_separators() {
    I.setLanguage("en");
    assertEquals("8888.88", I.numberToCompactString(8888.88));
    I.setLanguage("fr_FR");
    assertEquals("8888,88", I.numberToCompactString(8888.88));
    I.setLanguage("de_DE");
    assertEquals("8888,88", I.numberToCompactString(8888.88));
  }

  @Test
  public void list_generator_combines_lists_with_and() {
    I.setLanguage("en");
    assertEquals("", I.localizedStringsAsList(null, true));
    assertEquals("", I.localizedStringsAsList(new String[0], true));
    assertEquals("A", I.localizedStringsAsList(new String[] { "A" }, true));
    // NOTE: for the test, I'm not using "localized" strings, because there is no need...it's a test!
    assertEquals("A, B, and C", I.localizedStringsAsList(new String[] { "A", "B", "C" }, true));
    assertEquals("A, B, C, and D", I.localizedStringsAsList(new String[] { "A", "B", "C", "D" }, true));
  }

  @Test
  public void list_generator_combines_lists_with_or() {
    I.setLanguage("en");
    assertEquals("", I.localizedStringsAsList(null, false));
    assertEquals("", I.localizedStringsAsList(new String[0], false));
    assertEquals("A", I.localizedStringsAsList(new String[] { "A" }, false));
    // NOTE: for the test, I'm not using "localized" strings, because there is no need...it's a test!
    assertEquals("A, B, or C", I.localizedStringsAsList(new String[] { "A", "B", "C" }, false));
    assertEquals("A, B, C, or D", I.localizedStringsAsList(new String[] { "A", "B", "C", "D" }, false));
  }

  @Test
  public void trj_family_escapes_for_javascript() {
    I.setLanguage("en");
    assertEquals("H\\u00E9llo", I.trj("Héllo"));
    assertEquals("Tony\\'s brother says \\\"Hi!\\\"", I.trj("Tony's brother says \"Hi!\""));

    assertEquals("H\\u00E9llo", I.trfj("{0}", "Héllo"));
    assertEquals("Tony\\'s brother says \\\"Hi!\\\"", I.trfj("Tony''s brother says {0}", "\"Hi!\""));
  }

  @Test
  public void knows_locale_specific_fractional_currency_digits() {
    I.setLanguage(Locale.US);
    assertEquals(2, I.getCurrencyFractionDigits());
    I.setLanguage(Locale.FRANCE);
    assertEquals(2, I.getCurrencyFractionDigits());
    I.setLanguage(Locale.GERMANY);
    assertEquals(2, I.getCurrencyFractionDigits());
    I.setLanguage(Locale.JAPAN);
    assertEquals(0, I.getCurrencyFractionDigits());
  }

  @Test
  public void can_convert_an_iso_timestamp_string_to_date_object() {
    Date someWeirdDate = new Date(1305982733L);
    assertEquals(122, I.ISOTimestampToDate("2009-04-03 12:59:33.122", new Date()).getTime() % 1000);

    assertEquals(someWeirdDate, I.ISOTimestampToDate(I.timestampToISOString(someWeirdDate), new Date()));
  }

  @Test
  public void can_obtain_locale_specific_day_names() {
    I.setLanguage("en");
    Calendar c = Calendar.getInstance(I.getCurrentLanguage().locale);
    c.set(Calendar.HOUR_OF_DAY, 12);
    c.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    assertEquals("Tuesday", I.dayOfWeek(c.getTime(), false));
    assertEquals("Tue", I.dayOfWeek(c.getTime(), true));
    I.setLanguage("de");
    assertEquals("Dienstag", I.dayOfWeek(c.getTime(), false));
    assertEquals("Di", I.dayOfWeek(c.getTime(), true));

    I.setLanguage("en");
    c.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
    Date d = c.getTime();
    assertEquals("Tuesday", I.dayOfWeek(d, 7, false));
    assertEquals("Monday", I.dayOfWeek(d, 20, false));
    assertEquals("Wednesday", I.dayOfWeek(d, 1, false));
    assertEquals("Monday", I.dayOfWeek(d, -1, false));
    assertEquals("Thursday", I.dayOfWeek(d, -5, false));
    assertEquals("Saturday", I.dayOfWeek(d, -10, false));
    assertEquals("Friday", I.dayOfWeek(d, -11, false));
    assertEquals("Thursday", I.dayOfWeek(d, -12, false));
    assertEquals("Thursday", I.dayOfWeek(d, -19, false));
  }

  @Test
  public void can_obtain_locale_specific_month_names() {
    I.setLanguage("en");
    assertEquals("February", I.monthName(Calendar.FEBRUARY, false));
    assertEquals("Feb", I.monthName(Calendar.FEBRUARY, true));
    assertEquals("September", I.monthName(Calendar.SEPTEMBER, false));
    assertEquals("March", I.monthName(Calendar.MARCH, false));
    I.setLanguage("de");
    assertEquals("Februar", I.monthName(Calendar.FEBRUARY, false));
    assertEquals("Feb", I.monthName(Calendar.FEBRUARY, true));
  }

  @Test
  public void can_make_iso_date_strings() {
    assertEquals("1992-03-05", I.dateToISOString(makeDate(3, 5, 1992)));
  }

  @Test
  public void can_format_dates_with_custom_format_by_locale() {
    Date d = makeDate(3, 5, 2013);
    I.setLanguage("en_AU");
    assertEquals("5/3", I.dateToString(d, NO_YEAR));
    I.setLanguage("en");
    assertEquals("03/05", I.dateToString(d, NO_YEAR));
    I.setLanguage("de");
    assertEquals("05.03", I.dateToString(d, NO_YEAR));
  }

  @Test
  public void the_null_date_always_formats_to_an_empty_string() {
    setupNullDate();
    assertEquals("", I.dateToString(myNullDate));
    assertEquals("", I.timestampToString(myNullDate));
    assertEquals("", I.dateToISOString(myNullDate));
    assertEquals("", I.timestampToISOString(myNullDate));
    restoreNullDate();
    assertEquals("", I.dateToString(null));
    assertEquals("", I.timestampToString(null));
  }

  @Test
  public void date_inputs_missing_the_year_map_to_this_year() {
    I.setLanguage(Locale.US);
    Calendar c = Calendar.getInstance();
    c.set(Calendar.MONTH, Calendar.MARCH);
    c.set(Calendar.DAY_OF_MONTH, 4);
    assertEquals(I.dateToISOString(c.getTime()), I.dateToISOString(I.stringToDate("3/4")));

    I.setLanguage(Locale.FRANCE);
    assertEquals(I.dateToISOString(c.getTime()), I.dateToISOString(I.stringToDate("4/3")));

    I.setLanguage(Locale.GERMANY);
    assertEquals(I.dateToISOString(c.getTime()), I.dateToISOString(I.stringToDate("4.3")));
  }

  @Test
  public void null_strings_as_dates_are_converted_to_the_null_date() {
    setupNullDate();
    assertEquals(myNullDate, I.stringToDate(null));
    restoreNullDate();
    assertEquals(null, I.stringToDate(null));

  }

  @Test
  public void accepts_am_pm_designation_on_timestamps() {
    Date expected = makeTimestamp(1, 4, 1986, 23, 24, 0);
    Date dtEnd = I.stringToTimestamp("1/4/1986 11:24 PM", null);
    assertNotNull(dtEnd);
    assertTrue(areEqualTimestamps(expected, dtEnd, false));
  }

  @Test
  public void iso_timestamp_conversion_is_accurate_to_ms() {
    Date someWeirdDate = new Date(1305982733L);
    assertEquals("1970-01-15 18:46:22.733", I.timestampToISOString(someWeirdDate));
    assertEquals(122, I.ISOTimestampToDate("2009-04-03 12:59:33.122", new Date()).getTime() % 1000);
  }

  private Date makeDate(int month, int day, int year) {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.YEAR, year);
    c.set(Calendar.MONTH, month - 1);
    c.set(Calendar.DAY_OF_MONTH, day);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  private Date makeTimestamp(int month, int day, int year, int hour, int min, int sec) {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.YEAR, year);
    c.set(Calendar.MONTH, month - 1);
    c.set(Calendar.DAY_OF_MONTH, day);
    c.set(Calendar.HOUR_OF_DAY, hour);
    c.set(Calendar.MINUTE, min);
    c.set(Calendar.SECOND, sec);
    c.set(Calendar.MILLISECOND, 837);
    return c.getTime();
  }

  private boolean areEqualTimestamps(Date a, Date b, boolean compareSeconds) {
    Calendar ca = Calendar.getInstance();
    Calendar cb = Calendar.getInstance();
    ca.setTime(a);
    cb.setTime(b);

    ca.set(Calendar.MILLISECOND, 0);
    cb.set(Calendar.MILLISECOND, 0);

    if (!compareSeconds) {
      ca.set(Calendar.SECOND, 0);
      cb.set(Calendar.SECOND, 0);
    }
    boolean rv = ca.getTime().getTime() == cb.getTime().getTime();
    if (!rv)
      assertEquals(a, b); // to get error message with comparison display
    return rv;
  }
}
