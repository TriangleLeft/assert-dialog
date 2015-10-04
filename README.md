#AssertDialog
C#-like assert dialog. Shows modal dialog when assertion fails blocking current thread. (So you can attach a debugger, or decide to stop application)

You can either use it as same as Java's "assert" throwing AssertionError, or if you don't want to potentially block your team, as a dialog, allowing to skip asserts.

![](misc/assert.png)

Min SDK version - 14

Usage
-----

Before you start using it, you have to init it with operation mode and context. Preferable place to do that, is your applicaion onCreate() method.

There are three modes:

AssertMode.DIALOG - shows modal dialog (current thread execution is paused), with two options: stop application, or continue execution. Writes assert message with stacktrace to Log.wtf

AssertMode.LOG - only writes assert message with stacktrace to Log.wtf.

AssertMode.THROW - throws AssertionException 

```java
    @Override
    public void onCreate() {
        super.onCreate();
        AssertDialog.init(AssertMode.DIALOG, getApplicationContext());
    }
```

After that, you can use it as JUnit Asserts
```java
    private void updateUser(int userId)  {
        AssertDialog.assertTrue(userId > 0, "Trying to update user with id <= 0");
        AssertDialog.assertTrue(isUserExists(userId));
        ....
    }
```

License
-------

Copyright 2015 Aleksey Kurnosenko

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
