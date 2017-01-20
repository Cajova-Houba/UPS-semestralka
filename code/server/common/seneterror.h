#ifndef __SENET_ERROR_H
#define __SENET_ERROR_H

typedef enum {
	TOO_MAY_ATTEMPTS = -4,
	MSG_TIMEOUT = -3,
	GENERAL_ERR = -1,
	ERR_MSG = 1,
	ERR_MSG_TYPE,
	ERR_MSG_CONTENT,
	ERR_NICKNAME,
	ERR_NICK_EXISTS,
	ERR_SERVER_FULL,
	ERR_TURN
} senetError;

#endif
