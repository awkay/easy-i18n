WARNING: This library needs a new maintainer.  I do not intend to do any further updates or releases.  If you want to release a maintained fork I'll be glad to point this at your repository.  You can rename it (and packages) however you like.

# Easy I18N

This is a Java Library that is intended to make it much easier to 
create internationalized programs. It includes the following features:

- Integration with GNU Gettext
  - Use real strings in your code
  - Include translation context to help your translators
  - Have a real string come out when there is a missing translation
  - Use gettext utilities to extract, merge, manage, and upate them
  - Use GNU utilities to edit the resulting translation files
  - Easy (and superior) support for plurals
- Easier interfacing with Java facilities for //input and output// of:
  - Date and Timestamp
  - Money
  - Numerics
- Numerous methods to support message formatting
- Sample scripts for doing the extraction/merge step

## Building

This library uses Maven. You can import it into an IDE as a Maven project,
or build it from the command line:

    mvn package

[Please see the wiki for full docs.](https://github.com/awkay/easy-i18n/wiki)
