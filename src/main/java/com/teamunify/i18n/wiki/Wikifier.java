package com.teamunify.i18n.wiki;

/**
 * Defines an interface for objects that can turn wiki markup into your desired output (e.g. HTML)
 * 
 * @author tonykay
 */
public interface Wikifier {
  /**
   * Pass in a string with Wiki markup
   * @param s The wiki text
   * @return The desired output
   */
  public String wikified(String s);

}
