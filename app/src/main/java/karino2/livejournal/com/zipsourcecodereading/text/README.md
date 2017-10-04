# LongTextView

This folder contains implementation of LongTextView.

Standard TextView is designed for short text, and supporting warap_content make layout heavy task.

For source code reading, source code is basically long.
And size of TextView is specified by parent, and do not need to layout by themselves.
Also, showing only visible area is important for fast syntax coloring.

This LongTextView is designed for Long source code file reading only.

This impolementation is similar to https://github.com/githubth/jota-text-editor design (which is also similar to Android TextView), but there implementation make TextView class similar to Android TextView class, and it makes implementation complex.
I only support read-only textview with text always SpannableString, because this is the only usecase for us.
So our Layout is only StaticLayout, I merge Layout class and StaticLayout class.

Also, I do not support emoji, bidi (RTL), hint.
This make our code much smaller.