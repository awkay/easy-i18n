package com.teamunify.i18n.settings;

import java.util.Locale;

public class GlobalLanguageSettingsProvider implements LanguageSettingsProvider {
  volatile LanguageSetting lang;

  public LanguageSetting vend() {
    return lang;
  }

  public void setLocale(Locale l) {
    lang = new LanguageSetting(l);
  }
}
