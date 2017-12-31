# ZSCReading

Source code reading app for Android. This app assumes target source codes are archived as one zip file.

This app first indexing whole zip contents, then perform regular expression query.
This app is Android port of [Code Search](https://github.com/google/codesearch) + GUI client.

## ZSCReading is built using open source software:

- io.reactivex.rxjava2:rxjava
- io.reactivex.rxjava2:rxandroid
- [re2j-td](https://github.com/sopel39/re2j-td) we forked to use byte array and run on Android
- JavaPrettify

Also, we use codeserach logic.
