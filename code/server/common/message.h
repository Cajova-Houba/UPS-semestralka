#ifndef __SENET_MESSAGE_H
#define __SENET_MESSAGE_H

/* some common messages */
#define		OK_MESSAGE					"INFOK"
#define		OK_MESSAGE_LEN				5
#define		START_GAME_MESSAGE			"INFSTART_GAME"
#define		START_GAME_MESSAGE_LEN		13
#define 	END_GAME_MESSAGE			"INFEND_GAME"
#define 	END_GAME_MESSAGE_LEN		11
#define     START_TURN_MESSAGE_LEN      13

/* message types */
#define		MSG_TYPE_INF				"INF\0"
#define		MSG_TYPE_CMD				"CMD\0"
#define		MSG_TYPE_ERR				"ERR\0"
#define		MSG_TYPE_LEN				3


/*
 * Receives nick from socket and stores it to the buffer. 
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
 * Receives a message from the socket indicating the end of turn.
 * player1_turn_word and player2_turn_word are buffers for updated turn words.
 * Both turn words are expected to have length equal to TURN_WORD_LENGTH.
 *
 * Returns:
 * 	1 : Nick was received.
 *  0: Socket closed connection.
 *  Error from seneterror.h
 */
int recv_end_turn(int socket, char* player1_turn_word, char* player2_turn_word);


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
 * <0: Error occurred.
 * 
 */
int send_err_msg(int socket, int err_code);

/*
 * Sends START_GAME message with nicks of the 1st and 2nd player.
 * The message will look like this: INFSTART_GAME<player1>,<player2>;.
 *
 *
 * Returns:
 * 1: Message was sent.
 * 0: Socket closed the connection.
 * <0: Error occurred.
 */
int send_start_game_msg(int sock, char* player1, char* player2);

/*
 * Sends END_GAME message with nick of the winner.
 * The message will look like this: INFEND_GAME<winner>;
 *
 * Returns:
 * 1: Message was sent.
 * 2: Socket closed the connection.
 * <0: Error occurred.
 */
int send_end_game_msg(int sock, char* winner);

/*
 * Sends a start turn message to the socket.
 * The message will look like this: CMD<p1turnword><p2turnword>
 *
 * Returns:
 * 1: Message was sent.
 * 2: Socket closed the connection.
 * <0: Error occurred.
 */
int send_start_turn_msg(int sock, char* player1_turn_word, char* player2_turn_word);
#endif


