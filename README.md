# Android_1
Some Android APIs testing.

This is intended as a template project for some tests:
1. cryptographically strong random number generator (RNG)
2. compute SHA256 digest.
3. HMAC tag compute.
4. Some Datagrams tests..
5. test_ScheduledThreadPoolExecutor and logging static method from background thread.. 
6. Some enum type tests.
7. java.util.Timer class test.
8. android.os.PowerManager stuff (WAKE_LOCK).
9. resume this app from notification.
10. TCPclient1. (please see note_1) - undefined long term TCP connection over GPRS (or whatever).
11. AlarmManager1.
12. Intents1. (action intents constants).
13. timerToasts.

note_1: TCPclient1 (on Android phone) requires my repo [Erlang_1](https://github.com/Vladi2017/Erlang_1) as tcpServer. In this regard the client implements a FSM (Finite State Machine) graph in order to deal with various events arriving at the phone level (cellular network calls, loose RF signal, recover RF signal, etc), the most demanding to maintain the connection being over GPRS where data services are interrupted during voice calls. The server behavior can be controlled from the client side (e.g. keepAliveInterval). Overall this concept (long term TCP level socket channel connection) could be useful as support for an outband signalling scheme built on it.

![Android app menu](/Screenshot_20221113-213757.png)
