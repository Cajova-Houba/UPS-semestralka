#ifndef __SENET_MESSAGE_H
#define __SENET_MESSAGE_H

/* some common messages */
#define		OK_MESSAGE					"INFOK"
#define		OK_MESSAGE_LEN				5
#define		START_GAME_MESSAGE			"INFSTART_GAME"
#define		START_GAME_MESSAGE_LEN		13
#define 	END_GAME_MESSAGE			"INFEND_GAME"
#define 	END_GAME_MESSAGE_LEN		11

/* message types */
#define		MSG_TYPE_INF				"INF\0"
#define		MSG_TYPE_CMD				"CMD\0"
#define		MSG_TYPE_ERR				"ERR\0"
#define		MSG_TYPE_LEN				3


/*
 * Recieves nick from socket and stores it to the buffer. 
 * The buffer should have adequate size = MAX_NICK_LENGTH+1.
 * 
 * The nick is checked only for length by this method.
 * The nick will end with \0.
 * 
 * Returns:
 * 	1 : Nick was received.
 *  0: Socket closed connection.
 *  Error from seneterror.h
 */
int recv_nick(int socket, char* buffer);

/*
 * Send OK_MESSAGE to socket. 
 * 
 * Returns:
 * 1 : OK message was sent.
 * 0 : Socket closed the connection.
 * <0: Error occured.
 * 
 */
int send_ok_msg(int socket);

/*
 * Sends ERRX message, where x is the error code
 * 
 * Returns:
 * 1 : ERR message was sent.
 * 0 : Socket closed the connection.
 * <0: Error occured.
 * 
 */
int send_err_msg(int socket, int err_code);

#endif


