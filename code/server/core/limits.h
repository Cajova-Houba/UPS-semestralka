//
// Created by zdenda on 24.1.17.
//

#ifndef SERVER_LIMITS_H
#define SERVER_LIMITS_H

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
 * Max time for a player to reconnect. In seconds.
 */
#define DEF_TIMEOUT             10

/*
 * Max time to receive response to alive message.
 */
#define ALIVE_TIMEOUT           10000

/*
 * Timeout for messages while the thread is waiting
 */
#define WAITING_TIMEOUT         1000

/*
 * Max number of attempts for player to send a correct nick.
 */
#define MAX_NICK_ATTEMPTS       3

#endif //SERVER_LIMITS_H
