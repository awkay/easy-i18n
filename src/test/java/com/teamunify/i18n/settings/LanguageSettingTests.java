package com.teamunify.i18n.settings;

import com.teamunify.i18n.I;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

/**
 * These tests rely on pre-compiled message files in src/test/resources/com/teamunify/i18n
 */
public class LanguageSettingTests {
  static {
    LanguageSetting.translationPackage = "i18n.msgs";
    I.setLanguageSettingsProvider(new GlobalLanguageSettingsProvider());
  }

  public final LanguageSetting someSetting = new LanguageSetting(Locale.ENGLISH);

  @Test
  public void uses_empty_language_bundle_when_no_bundle_exists() {
    assertEquals(someSetting.findBestTranslation(LanguageSetting.translationPackage, Locale.CHINESE), LanguageSetting.emptyLanguageBundle);
  }

  @Test
  public void uses_exact_language_bundle_on_exact_locale_match() {
    I.setLanguage(Locale.CANADA_FRENCH);
    assertEquals("Supprimer", I.tru("Remove"));
  }

  @Test
  public void uses_language_bundle_when_no_country_specific_bundle_exists() {
    I.setLanguage(Locale.FRANCE);
    assertEquals("Éliminer", I.tru("Remove"));
  }
}
