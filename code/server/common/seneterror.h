#ifndef __SENET_ERROR_H
#define __SENET_ERROR_H

typedef enum {
	GENERAL_ERR = -1,
	ERR_MSG = 1,
	ERR_MSG_TYPE,
	ERR_MSG_CONTENT,
	ERR_NICKNAME,
	ERR_NICKNAME_EXIST,
	ERR_TURN
} senetError;

#endif
