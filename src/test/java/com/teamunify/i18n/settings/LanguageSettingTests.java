package com.teamunify.i18n.settings;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

/**
 * These tests rely on pre-compiled message files in src/test/resources/com/teamunify/i18n
 */
public class LanguageSettingTests {
  public final LanguageSetting someSetting = new LanguageSetting(Locale.ENGLISH);

  @Test
  public void returns_empty_language_bundle_when_no_bundle_exists() {
    assertEquals(someSetting.findBestTranslation(LanguageSetting.translationPackage, Locale.ENGLISH), LanguageSetting.emptyLanguageBundle);
  }
}
