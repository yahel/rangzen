# Rangzen

Blackout-resistant mobile communication.

## Building Rangzen with Buck

To build Rangzen, you'll need Buck: https://facebook.github.io/buck/.
Buck is easy to setup, but only works on OS X and Linux natively.

`https://facebook.github.io/buck/setup/quick_start.html` explains how to start,
but the basic steps are:

    git clone https://github.com/facebook/buck.git
    cd buck
    ant
    sudo ln -s ${PWD}/bin/buck /usr/bin/buck

While there, you may also want to set up buckd:

    sudo ln -s ${PWD}/bin/buckd /usr/bin/buckd

Once you've setup buck, you just need to add a local.properties file to the root
directory of the repo that points to your Android SDK. For example, mine is a 
one line file with the line:

    sdk.dir=/Users/lerner/dev/adt/sdk/

Once that's there, you should be able to `buck build experimentalApp` to build
the app or `buck install experimentalApp` to install it on any Android devices
you have plugged in (it handles the adb installing process).

## Hardcoding friends

Friends are added by calling .addFriendBytes() on an instance of FriendStore.
You might hardcode some friends when RangzenService (the background service
that periodically looks for other phones to contact) starts up, for example.
For an example of adding some arbitrary friends, see the test `tests/org/denovogroup/rangzen/FriendStoreTest.java`.

