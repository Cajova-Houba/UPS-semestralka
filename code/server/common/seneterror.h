#ifndef __SENET_ERROR_H
#define __SENET_ERROR_H

typedef enum {
	GENERAL_ERR = -1,
	ERR_MSG = 1,
	ERR_MSG_TYPE,
	ERR_MSG_CONTENT,
	ERR_NICKNAME,
	ERR_NICK_EXISTS,
	ERR_SERVER_FULL,
	ERR_TURN
} senetError;

/*
 * Possible states returned by functions.
 */
typedef enum {
	P2_WINS = -10,
	P1_WINS = -9,
	STOP_GAME_LOOP = -8,
	WAITING_P2 = -7,
	WAITING_P1 = -6,
	GAME_NOT_WAITING = -5,
	TOO_MAY_ATTEMPTS = -4,
	MSG_TIMEOUT = -3,
	OK = 1
} state;

#endif
