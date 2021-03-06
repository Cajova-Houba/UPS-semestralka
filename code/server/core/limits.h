//
// Created by zdenda on 24.1.17.
//

#ifndef SERVER_LIMITS_H
#define SERVER_LIMITS_H

/*
 * Max number of games.
 */
#define MAX_GAMES               5

/*
 * Max number of connections sending their nicknames.
 */
#define MAX_CONNECTIONS			10

/*
 * Max number of timer threads.
 */
#define MAX_TIMER_THREADS       2

/*
 * Max time[ms] to receive response to alive message.
 */
#define ALIVE_TIMEOUT           100000

/*
 * Timeout[ms] for messages while the thread is waiting.
 * This have to be small, because it's also used as a
 * 'thread.sleep' in active waiting.
 */
#define WAITING_TIMEOUT         500

/*
 * Max number of attempts for player to send a correct nick.
 */
#define MAX_NICK_ATTEMPTS       3

/*
 * Nick is expected to be delivered after connection in this time[ms].
 */
#define MAX_NICK_TIMEOUT      10000

/*
 * Waiting for turn - 2 minutes and 5 seconds (in client it's just 2 minutes), after that it is assumed, that player
 * has disconnected and the waiting process starts.
 * [ms]
 */
#define MAX_TURN_WAITING_TIMEOUT     125000

#endif //SERVER_LIMITS_H
