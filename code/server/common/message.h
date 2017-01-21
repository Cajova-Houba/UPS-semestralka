#ifndef __SENET_MESSAGE_H
#define __SENET_MESSAGE_H

/* some common messages */
#define		OK_MESSAGE					"OK"
#define		OK_MESSAGE_LEN				2
#define		START_GAME_MESSAGE			"INFSTART_GAME"
#define		START_GAME_MESSAGE_LEN		13
#define 	END_GAME_MESSAGE			"INFEND_GAME"
#define 	END_GAME_MESSAGE_LEN		11
#define     START_TURN_MESSAGE_LEN      23
#define     WAIT_FOR_PLAYER_MESSAGE     "INFWAITING"
#define     WAIT_FOR_PLAYER_MESSAGE_LEN 10
#define     EXIT_MSG                    "EXIT"
#define     EXIT_MSG_LEN                4
#define     ALIVE_MSG                   "ALIVE"
#define     ALIVE_MSG_LEN               5

/* message types */
#define		MSG_TYPE_INF				"INF\0"
#define		MSG_TYPE_CMD				"CMD\0"
#define		MSG_TYPE_ERR				"ERR\0"
#define		MSG_TYPE_LEN				3

#define     MAX_CONTENT_SIZE            255

#include <string.h>

#include <stdio.h>
#include "common.h"
#include "seneterror.h"
#include "slog.h"
#include "nick_val.h"


#include "../core/player.h"

typedef enum {
    NO_TYPE = -1,
    ERR_TYPE = 1,
    CMD_TYPE,
    INF_TYPE
} Message_type;

typedef struct {
    int message_type_int;
    char message_type[MSG_TYPE_LEN+1];
    char content[MAX_CONTENT_SIZE];
    senetError error_code;
} Message;

/*
 * Checks if the message is alive message.
 * Returns ok if it is or 0 if it isn't.
 */
int is_alive(Message* message);

/*
 * Checks if the message is nick.
 * Returns ok if it is or 0 if it isn't.
 */
int is_nick(Message* message);

/*
 * Receives message from socket.
 *
 * Returns:
 * OK: Message was received.
 * CLOSED_CONNECTION: Connection is closed.
 * MSG_TIMEOUT: Socket timet out.
 * or error from senneterror.h
 */
int recv_message(int socket, Message* message, int timeout);

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
 * Receives nick from socket and stores it to the buffer.
 * The buffer should have adequate size = MAX_NICK_LENGTH+1.
 *
 * Nick length is expected to be 1 char from '3' - '8'.
 *
 * The nick is checked only for length by this method.
 * The nick will end with \0.
 *
 * Returns:
 * 	1 : Nick was received.
 *  0: Socket closed connection.
 *  Error from seneterror.h
 */
int recv_nick_alphanum(int socket, char* buffer);

/*
 * Receives a message from the socket indicating the end of turn.
 * player1_turn_word and player2_turn_word are buffers for updated turn words.
 * Both turn words are expected to have length equal to TURN_WORD_LENGTH.
 *
 * Returns:
 *  2 : Client quit.
 * 	1 : Nick was received.
 *  0: Socket closed connection.
 *  Error from seneterror.h
 */
int recv_end_turn(int socket, char* player1_turn_word, char* player2_turn_word);

/*
 * Receives a message from the socket indicating the end of turn.
 * player1_turn_word and player2_turn_word are buffers for updated turn words.
 * Both turn words are expected to have length equal to TURN_WORD_LENGTH.
 *
 * Returns:
 * OK: both turn words received.
 * CLOSED_CONNECTION: connection to socket is closed.
 * MSG_TIMEOUT: timeout
 * or error from seneterror.h
 */
int recv_end_turn_alphanum(int socket, char* player1_turn_word, char* player2_turn_word);

/*
 * Receives OK message from socket.
 *
 * Returns:
 *  1 : Ok was received.
 *  0 : Socket closed connection.
 *  MSG_TIMEOUT: Timeout.
 */
int recv_ok_msg(int socket);

/*
 * Send OK_MESSAGE to socket. 
 * 
 * Returns:
 * OK: message was sent.
 * ERR_MSG: error while sendig message.
 * CLOSED_CONNECTION: connection closed.
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

/*
 * Sends a message to the socket that the game is currently waiting for a player with
 * specified nick.
 *
 * Returns:
 * 1: Message was sent.
 * 2: Socket closed the connection.
 * <0: Error occurred.
 */
int send_waiting_for_player_msg(int sock, char* nick);
#endif


