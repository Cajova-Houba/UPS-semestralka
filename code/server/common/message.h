#ifndef __SENET_MESSAGE_H
#define __SENET_MESSAGE_H

/* some common messages */
#define		OK_MESSAGE					"INFOK\0"
#define		OK_MESSAGE_LEN				5
#define		START_GAME_MESSAGE			"INFSTART_GAME\0"
#define		START_GAME_MESSAGE_LEN		13
#define 	END_GAME_MESSAGE			"INFEND_GAME\0"
#define 	END_GAME_MESSAGE_LEN		11

/* message types */
#define		MSG_TYPE_INF				"INF\0";
#define		MSG_TYPE_CMD				"CMD\0";
#define		MSG_TYPE_ERR				"ERR\0";
#define		MSG_TYPE_LEN				3;



/*
 * Recieves nick from socket and stores it to the buffer. 
 * The buffer should have adequate size.
 * 
 * The nick is checked only for length by this method.
 * 
 * Returns:
 * 	0 : Nick was received.
 *  -1: Socket closed connection.
 *  Error from seneterror.h
 */
int recv_nick(int socket, char* buffer);

#endif


