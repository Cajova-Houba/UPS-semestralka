#ifndef __SENET_ERROR_H
#define __SENET_ERROR_H

typedef enum {
	GENERAL_ERR = -50,
	ERR_MSG,
	ERR_MSG_TYPE,
	ERR_MSG_CONTENT,
	ERR_NICKNAME,
	ERR_NICK_EXISTS,
	ERR_NICK_LEN,
	ERR_SERVER_FULL,
	ERR_TURN
} senetError;

/*
 * Possible states returned by functions.
 */
typedef enum {
	P2_WINS = -100,
	P1_WINS,
	STOP_GAME_LOOP,
	WAITING_P2,
	WAITING_P1,
	GAME_NOT_WAITING,
	TOO_MAY_ATTEMPTS,
	MSG_TIMEOUT,
	CLOSED_CONNECTION,
	OK
} state;

#endif
