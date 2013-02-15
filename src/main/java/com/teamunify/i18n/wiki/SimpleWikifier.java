package com.teamunify.i18n.wiki;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convert simple wiki markup into HTML markup.
 * 
 * The support in this class honors:
 * 
 * <ul>
 * <li>bold (surround with **)
 * <li>italics (surround with //)
 * <li>underline (surround with __)
 * <li>linebreak (_br_)
 * <li>links ([[URL|text]]).
 * </ul>
 */
public class SimpleWikifier implements Wikifier {
  private static Pattern boldPattern = Pattern.compile("\\*\\*([^*/_]*)\\*\\*");
  private static Pattern italicPattern = Pattern.compile("//([^/*_]*)//");
  private static Pattern underlinePattern = Pattern.compile("__([^*/_]*)__");
  private static Pattern linebreak = Pattern.compile("_br_");
  private static Pattern redfont = Pattern.compile("_r_([^*/_]*)_r_");
  private static Pattern linkPattern = Pattern.compile("\\[\\[([^|]*)\\|([^]]*)\\]\\]");

  public String wikified(String msg) {
    Matcher m = boldPattern.matcher(msg);
    if (m.find())
      msg = m.replaceAll("<b>$1</b>");
    m = italicPattern.matcher(msg);
    if (m.find())
      msg = m.replaceAll("<i>$1</i>");
    m = underlinePattern.matcher(msg);
    if (m.find())
      msg = m.replaceAll("<u>$1</u>");
    m = redfont.matcher(msg);
    if (m.find())
      msg = m.replaceAll("<font color=red>$1</font>");
    m = linebreak.matcher(msg);
    if (m.find())
      msg = m.replaceAll("<br>");
    m = linkPattern.matcher(msg);
    if (m.find())
      msg = m.replaceAll("<a href=\"$1\">$2</a>");
    return msg;
  }
}
