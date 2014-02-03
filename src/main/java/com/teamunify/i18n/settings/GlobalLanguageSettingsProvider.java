package com.teamunify.i18n.settings;

import java.util.Locale;

/**
 * A class that can set a system-wide language settings provider. Useful for regular Java apps where all threads
 * share a single locale that is set by the user (via a menu, etc).
 */
public class GlobalLanguageSettingsProvider implements LanguageSettingsProvider {
  volatile LanguageSetting lang;

  public LanguageSetting vend() {
    return lang;
  }

  public void setLocale(Locale l) {
    lang = new LanguageSetting(l);
  }
}
