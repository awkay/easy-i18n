package com.teamunify.i18n.settings;

import java.util.Locale;

/**
 * A factory that creates LanguageSetting objects for use by the translation functions. Implementations of this can
 * provide anything from thread-local settings, to global settings.
 * 
 * <p>
 * The basic functionality is as follows:
 * <ol>
 * <li>You create an instance of a provider</li>
 * <li>You give that provider to the I.setLanguageSettingsProvider</li>
 * <li>Any time the target language is set via I.setLanguage, the request is sent to the setting provider</li>
 * <li>Translation functions then ask the provide to vend a LangaugeSetting whenever a translation is requested.</li>
 * </ol>
 * 
 * <p>
 * Implementations should therefore do a minimal amount of work during vend(). Normally, a provider will be using a
 * instance variable (global setting for the app's locale) or thread-local variable to hold the language setting
 * (webapp), so it is very fast.
 * 
 * @author tonykay
 * 
 */
public interface LanguageSettingsProvider {
  /**
   * Get the current language settings
   * 
   * @return
   */
  public LanguageSetting vend();

  /**
   * Set the locale using a Locale
   */
  public void setLocale(Locale l);
}
