package com.teamunify.i18n.escape;

public interface EscapeFunction {
  public String escape(String s);
  
  public static EscapeFunction NoEscape = new EscapeFunction() {
    public String escape(String s) {
      return s;
    }
  };

  public static EscapeFunction EscapeHTML = new HTMLEscapeFunction();
}
