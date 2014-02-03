package com.teamunify.i18n.webapp;

import java.io.IOException;
import java.util.Locale;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.teamunify.i18n.I;

/**
 * Reads the browser's preferred locale, checks the session, and sets the language based upon it.
 * <p/>
 * TODO: TuProp defaults.
 *
 * @author tonykay
 */
public abstract class ServletLocaleFilter implements Filter {
  private static Logger log = LoggerFactory.getLogger(ServletLocaleFilter.class);

  public void destroy() {
  }

  /**
   * Returns the default locale (e.g. en_US). Override to specify your default.
   * <p/>
   * <p>Make sure this returns something of the form lang_country, where country is optional. Both codes should
   * be from the same ISO standards that Java supports in Locale</p>
   *
   * @return By default returns Locale.US
   */
  public Locale getDefaultLocale() {
    return Locale.US;
  }

  /**
   * Override this method to provide code that determined the locale that should be used for this request.
   *
   * @param req
   * @return
   */
  public abstract Locale getLocale(ServletRequest req);

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                                   ServletException {
    Locale l = getLocale(request);
    if (l == null)
      l = getDefaultLocale();
    if (l == null)
      l = Locale.getDefault();
    if (!I.supports(l))
      log.warn("Request for unsupported locale " + l);

    I.setLanguage(l); //set current thread & session for the requests
    chain.doFilter(request, response); // Do actual request
  }

  public void init(FilterConfig arg0) throws ServletException {
  }
}
