package com.teamunify.i18n;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class HTMLEscapeFunction implements EscapeFunction {
  public String escape(String s) {
    return escapeHtml(s);
  }
}
