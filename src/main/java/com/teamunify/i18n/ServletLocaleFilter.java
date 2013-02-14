package com.teamunify.i18n;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Reads the browser's preferred locale, checks the session, and sets the language based upon it.
 * 
 * TODO: TuProp defaults.
 * 
 * @author tonykay
 */
public abstract class ServletLocaleFilter implements Filter {
  private static Logger log = Logger.getLogger(ServletLocaleFilter.class.getName());
  public void destroy() {}

  /**
   * Returns the default locale (e.g. en_US). Override to specify your default.
   * 
   * <p>Make sure this returns something of the form lang_country, where country is optional. Both codes should 
   * be from the same ISO standards that Java supports in Locale</p>
   * 
   * TODO: Get this from a web.xml parameter
   * @return By default returns "en"
   */
  public String getDefaultLocale() { return "en"; }
  
  /**
   * Override this method to provide code that determined the locale that should be used for this request.
   * @param req
   * @return
   */
  public abstract String getLocale(ServletRequest req);
  
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                           ServletException {
    String lang = getLocale(request);
    if (lang.isEmpty())
      lang = getDefaultLocale();
    if (!I.supports(lang)) {
      log.warning("Request for unsupported language " + lang);
      lang = getDefaultLocale();
    }

    I.setLanguage(lang); //set current thread & session for the requests
    chain.doFilter(request, response); // Do actual request
  }

  public void init(FilterConfig arg0) throws ServletException {}
}
