## Translations
- Clone MessagesBundle_en_US.properties
- Rename to MessagesBundle_(your language and country code).properties
- Replace strings with appropriate translations

## Notes
Ignore string formatting entries e.g. `%s`,`%d`,`%f` etc, these will be filled in by ServerSync with various data from the program.

e.g. 

`List of mods: %s` would become:
```text
List of mods: {
  modA,
  modB
}
```

`Day of the week: %d` would become:
```text
Day of the week: 4
```


http://lh.2xlibre.net/locales/
