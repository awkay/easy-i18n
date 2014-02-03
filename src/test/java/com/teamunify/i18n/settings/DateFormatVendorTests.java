package com.teamunify.i18n.settings;

import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.Assert.*;

public class DateFormatVendorTests {
  final int badFormatID = DateFormatVendor.DEFAULT_DATE_FORMAT_ID - 3;
  final int customFormatID = DateFormatVendor.DEFAULT_DATE_FORMAT_ID + 2;
  final int unknownFormat = 329487234;
  DateFormatVendor vendor = null;
  final SimpleDateFormat mdy = new SimpleDateFormat("mm-dd-yy");
  final SimpleDateFormat dmy = new SimpleDateFormat("dd-mm-yy");

  @Before
  public void setup() {
    vendor = new DateFormatVendor();
  }

  @Test
  public void vendor_returns_built_in_formatter_for_short_medium_and_long_in_correct_locale() {
    assertEquals(vendor.getFormatFor(DateFormat.SHORT, Locale.US, unknownFormat), DateFormat.getDateInstance(DateFormat.SHORT, Locale.US));
    assertEquals(vendor.getFormatFor(DateFormat.MEDIUM, Locale.FRANCE, unknownFormat), DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.FRANCE));
    assertEquals(vendor.getFormatFor(DateFormat.LONG, Locale.UK, unknownFormat), DateFormat.getDateInstance(DateFormat.LONG, Locale.UK));
  }

  @Test
  public void vendor_returns_short_formatter_for_correct_locale_when_asked_for_unknown() {
    assertSameFormat(vendor.getFormatFor(unknownFormat, Locale.UK, unknownFormat), DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK));
  }

  @Test(expected = IllegalArgumentException.class)
  public void vendor_refuses_to_register_formats_with_id_less_than_DEFAULT_DATE_FORMAT() {
    vendor.registerFormat(badFormatID, Locale.UK, mdy, false);
  }

  @Test
  public void vendor_returns_alternate_when_primary_is_not_defined() {
    assertSameFormat(vendor.getFormatFor(unknownFormat, Locale.UK, DateFormat.LONG), DateFormat.getDateInstance(DateFormat.LONG, Locale.UK));
  }

  @Test
  public void vendor_override_date_format_registration_under_same_key() {
    vendor.registerFormat(customFormatID, Locale.UK, mdy, false);
    vendor.registerFormat(customFormatID, Locale.UK, dmy, false);

    assertSameFormat(vendor.getFormatFor(customFormatID, Locale.UK, DateFormat.LONG), dmy);
  }

  @Test
  public void vendor_assumes_default_format_is_short_in_the_correct_locale_if_unset() {
    assertSameFormat(vendor.getFormatFor(DateFormatVendor.DEFAULT_DATE_FORMAT_ID, Locale.UK, DateFormat.LONG), DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK));
  }

  @Test
  public void vendor_lookup_automatically_degrades_to_language_only_if_registered_format_not_found_with_country() {
    vendor.registerFormat(customFormatID, Locale.ENGLISH, dmy, false);

    assertSameFormat(vendor.getFormatFor(customFormatID, Locale.UK, DateFormat.LONG), dmy);
  }

  @Test
  public void vendor_does_not_use_country_specific_format_for_language_only_requests() {
    vendor.registerFormat(customFormatID, Locale.UK, dmy, false);

    assertNotSameFormat(vendor.getFormatFor(customFormatID, Locale.ENGLISH, DateFormat.SHORT), dmy);
  }

  @Test
  public void vendor_only_includes_a_format_for_input_parsing_if_specified() {
    vendor.registerFormat(customFormatID, Locale.UK, dmy, false);
    for (DateFormat f : vendor.getInputFormats(Locale.UK))
      assertFalse(f == dmy);

    vendor.registerFormat(customFormatID, Locale.UK, dmy, true);
    boolean found = false;
    for (DateFormat f : vendor.getInputFormats(Locale.UK))
      if (f == dmy)
        found = true;

    assertTrue(found);
  }

  @Test
  public void vendor_can_unregister_custom_formats() {
    vendor.registerFormat(customFormatID, Locale.ENGLISH, dmy, false);
    vendor.unregisterFormat(customFormatID, Locale.ENGLISH);

    assertNotSameFormat(vendor.getFormatFor(customFormatID, Locale.UK, DateFormat.LONG), dmy);
  }

  /**
   * Helper for checking formats.
   */
  private void assertSameFormat(DateFormat f1, DateFormat f2) {
    assertEquals(((SimpleDateFormat) f1).toLocalizedPattern(), ((SimpleDateFormat) f2).toLocalizedPattern());
  }

  private void assertNotSameFormat(DateFormat f1, DateFormat f2) {
    assertNotSame(((SimpleDateFormat) f1).toLocalizedPattern(), ((SimpleDateFormat) f2).toLocalizedPattern());
  }
}
