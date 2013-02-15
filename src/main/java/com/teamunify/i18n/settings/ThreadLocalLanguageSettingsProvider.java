package com.teamunify.i18n.settings;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A language settings provider that can manage language settings on a per-thread basis. Usually what you want if you
 * are writing a webapp (each request serves a specific user on a thread).
 * 
 * <p>
 * Of course, you'll need to call I.setLanguage at the start of each request, which will update the provider's
 * thread-local idea of the current language for that thread.
 * 
 * @author tonykay
 * 
 */
public class ThreadLocalLanguageSettingsProvider implements LanguageSettingsProvider {
  private static Logger log = LoggerFactory.getLogger(ThreadLocalLanguageSetting.class);
  ThreadLocalLanguageSetting currentLanguage = new ThreadLocalLanguageSetting();

  public LanguageSetting vend() {
    return currentLanguage.get();
  }

  public void setLocale(Locale l) {
    LanguageSetting setting = new LanguageSetting(l);
    log.debug("setting language bundle to " + setting.translation.getClass().getName());
    currentLanguage.set(setting);
  }
}
