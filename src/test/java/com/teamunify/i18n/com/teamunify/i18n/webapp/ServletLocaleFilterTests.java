package com.teamunify.i18n.com.teamunify.i18n.webapp;

import com.teamunify.i18n.I;
import com.teamunify.i18n.webapp.AbstractLocaleFilter;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

import javax.servlet.*;
import java.io.IOException;
import java.util.Locale;

public class ServletLocaleFilterTests {
  private static Locale computedLocale;
  private static Locale defaultLocale;

  Filter f = new AbstractLocaleFilter() {
    @Override
    public Locale getLocale(ServletRequest req) {
      return computedLocale;
    }

    @Override
    public Locale getDefaultLocale() {
      return defaultLocale;
    }
  };
  private ServletRequest req;
  private FilterChain chain;
  private ServletResponse resp;

  @Before
  public void setup() {
    computedLocale = Locale.US;
    defaultLocale = Locale.US;
    req = mock(ServletRequest.class);
    resp = mock(ServletResponse.class);
    chain = mock(FilterChain.class);
  }

  @Test
  public void chains_the_filters() throws IOException, ServletException {
    f.doFilter(req, resp, chain);

    verify(chain).doFilter(eq(req), eq(resp));
  }

  @Test
  public void uses_the_computed_locale_if_available() throws IOException, ServletException {
    computedLocale = Locale.CHINA;

    f.doFilter(req, resp, chain);

    assertEquals(I.getCurrentLanguage().locale, computedLocale);
  }

  @Test
  public void uses_the_filter_default_locale_if_compute_fails() throws IOException, ServletException {
    computedLocale = null;
    defaultLocale = Locale.KOREA;

    f.doFilter(req, resp, chain);

    assertEquals(I.getCurrentLanguage().locale, defaultLocale);
  }

  @Test
  public void uses_the_system_locale_if_no_computed_or_default_available() throws IOException, ServletException {
    computedLocale = null;
    defaultLocale = null;

    f.doFilter(req, resp, chain);

    assertEquals(I.getCurrentLanguage().locale, Locale.getDefault());
  }
}
