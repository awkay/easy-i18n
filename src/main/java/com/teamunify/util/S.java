package com.teamunify.util;

/**
 * String functions for embedding strings in strings.
 * 
 * @author tony kay
 */
public class S {
  /**
   * Surround the given value with single-quotes. If the value contains a single-quote, it will be escaped in a manner
   * compatible with JavaScript. E.g. Tony's becomes Tony\'s.
   * 
   * @param value
   *          The value to quote
   */
  public static String q(String value) {
    value = value.replace("'", "\'");
    return String.format("'%s'", value);
  }

  /**
   * Surround the given value with double-quotes. If the value contains a double-quote, it will be escaped in a manner
   * compatible with JavaScript. E.g. "Tony" becomes "\"Tony\"".
   * 
   * @param value
   *          The value to quote
   */
  public static String qq(String value) {
    value = value.replace("\"", "\\\"");
    return String.format("\"%s\"", value);
  }
}