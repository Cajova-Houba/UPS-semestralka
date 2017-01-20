
#include "message.h"

/*
 * Recieves nick from socket and stores it to the buffer. 
 * The buffer should have adequate size.
 * 
 * The nick is checked only for length by this method.
 * 
 * Returns:
 *  -2 : Socket timed out.
 * 	1 : Nick was received.
 *  0: Socket closed connection.
 *  Error from seneterror.h
 */
int recv_nick(int socket, char* buffer) {
	char nbuffer[4]; // 4 is max length of msg type + '\0'
	int nick_len = 0;
	int recv_status = 0;
	
	recv_status = recv_bytes_timeout(socket, nbuffer, MSG_TYPE_LEN, MAX_SOCKET_TIMEOUT);
	if(recv_status != MSG_TYPE_LEN) {
		serror(MESSAGE_NAME,"Error while receiving nick message.\n");
		return recv_status;
	}
	
	/* the type must be CMD */
	nbuffer[3] = '\0';
	if(strcmp(nbuffer, MSG_TYPE_CMD) != 0) {
		return ERR_MSG_TYPE;
	}
	
	/* receive one more byte to gain the length of the nick */
	recv_status = recv_bytes(socket, nbuffer, 1);
	if(recv_status != 1) {
		serror(MESSAGE_NAME, "Error while receiving the nick length.\n");
		return recv_status;
	}
	nick_len = (int)nbuffer[0];
	if(nick_len < MIN_NICK_LENGTH || nick_len > MAX_NICK_LENGTH) {
		serror(MESSAGE_NAME, "Nick has wrong length.\n");
		return ERR_MSG_CONTENT;
	}
	
	/* receive the rest of the nick */
	recv_status = recv_bytes(socket, buffer, nick_len);
	if(recv_status != nick_len) {
		serror(MESSAGE_NAME, "Error while receiving the rest of the nick.\n");
		return recv_status;
	}
	
	buffer[nick_len] = '\0';
	
	
	return 1;
}

/*
 * Receives a message from the socket indicating the end of turn.
 * player1_turn_word and player2_turn_word are buffers for updated turn words.
 * Both turn words are expected to have length equal to TURN_WORD_LENGTH.
 *
 * Returns:
 *  -2 : timed out
 *  2 : Client quit.
 * 	1 : Both turn words received.
 *  0: Socket closed connection.
 *  Error from seneterror.h
 */
int recv_end_turn(int socket, char* player1_turn_word, char* player2_turn_word) {
    char nbuffer[5]; // 5 is max length of EXIT message + '\0'
    int recv_status = 0;
	int i;

//    sdebug(MESSAGE_NAME, "Waiting for end turn message.\n");
    recv_status = recv_bytes_timeout(socket, nbuffer, MSG_TYPE_LEN, MAX_NICK_WAITING_TIMEOUT);
    if(recv_status == MSG_TIMEOUT) {
        return MSG_TIMEOUT;
    } else if(recv_status != MSG_TYPE_LEN) {
        serror(MESSAGE_NAME,"Error while receiving end turn message.\n");
        return recv_status;
    }

    /* the type must be INF */
    nbuffer[3] = '\0';
    if(strcmp(nbuffer, MSG_TYPE_INF) != 0) {
        serror(MESSAGE_NAME, "Wrong message type received, expecting INF.\n");
        return ERR_MSG_TYPE;
    }

	/* try to receive first 4 bytes and check exit message */
	recv_status = recv_bytes_timeout(socket, nbuffer, EXIT_MSG_LEN, MAX_NICK_WAITING_TIMEOUT);
	if (recv_status == EXIT_MSG_LEN && strcmp(nbuffer, EXIT_MSG) == 0) {
		sdebug(MESSAGE_NAME, "EXIT message received.\n");
		return 2;
	} else {
		// received message is a turn word, copy 4 bytes
		for(i = 0; i < EXIT_MSG_LEN; i++) {
			player1_turn_word[i] = nbuffer[i];
		}
	}

    /* receive the remaining 1 byte of the first turn word */
    recv_status = recv_bytes(socket, &player1_turn_word[TURN_WORD_LENGTH-1], 1);
    if (recv_status == MSG_TIMEOUT) {
        return MSG_TIMEOUT;
    } else if(recv_status != 1) {
        serror(MESSAGE_NAME, "Error while receiving first turn word of end turn message.\n");
        return ERR_MSG_CONTENT;
    }

    /* second 5 bytes are 2 turn word */
    recv_status = recv_bytes(socket, player2_turn_word, TURN_WORD_LENGTH);
    if (recv_status == MSG_TIMEOUT) {
        return MSG_TIMEOUT;
    } else if(recv_status != TURN_WORD_LENGTH) {
        serror(MESSAGE_NAME, "Error while receiving second turn word of end turn message.");
        return ERR_MSG_CONTENT;
    }

    return 1;
}

/*
 * Receives OK message from socket.
 *
 * Returns:
 *  1 : Ok was received.
 *  0 : Socket closed connection.
 *  MSG_TIMEOUT: Timeout.
 */
int recv_ok_msg(int socket) {
	char nbuffer[3]; // 3 is max length of OK message + '\0'
	int recv_status = 0;
	int i;

	sdebug(MESSAGE_NAME, "Waiting for end turn message.\n");
	recv_status = recv_bytes_timeout(socket, nbuffer, MSG_TYPE_LEN, MAX_SOCKET_TIMEOUT);
	if(recv_status != MSG_TYPE_LEN) {
		serror(MESSAGE_NAME,"Error while receiving ok message type.\n");
		return recv_status;
	}

	/* the type must be INF */
	nbuffer[3] = '\0';
	if(strcmp(nbuffer, MSG_TYPE_INF) != 0) {
		serror(MESSAGE_NAME, "Wrong message type received, expecting INF.\n");
		return ERR_MSG_TYPE;
	}

	/* try to receive the rest of the OK message */
	recv_status = recv_bytes_timeout(socket, nbuffer, EXIT_MSG_LEN, MAX_SOCKET_TIMEOUT);
    nbuffer[2] = '\0';
	if (recv_status == OK_MESSAGE_LEN && strcmp(nbuffer, OK_MESSAGE) == 0) {
		return 1;
	} else {
		return recv_status;
	}
}

/*
 * Send OK_MESSAGE to socket. 
 * 
 * Returns:
 * 1: OK message was sent.
 * 0: Error occured.
 * 2: Socket closed the connection.
 * 
 */
int send_ok_msg(int socket) {
    char buffer[MSG_TYPE_LEN+OK_MESSAGE_LEN+1];
    strcpy(buffer, MSG_TYPE_INF);
    strcpy(&buffer[MSG_TYPE_LEN], OK_MESSAGE);
    buffer[MSG_TYPE_LEN+OK_MESSAGE_LEN] = '\0';
	return send_txt(socket, buffer);
}

/*
 * Sends ERRX message, where x is the error code
 * 
 * Returns:
 * 1 : ERR message was sent.
 * 0 : Socket closed the connection.
 * <0: Error occured.
 * 
 */
int send_err_msg(int socket, int err_code) {
	char buff[5];
	strcpy(buff, MSG_TYPE_ERR);
	buff[3] = (char)err_code;
    buff[4] = '\0';
	
	return send_txt(socket, buff);
}

/*
 * Sends START_GAME message with nicks of the 1st and 2nd player.
 * The message will look like this: INFSTART_GAME<player1>,<player2>;.
 *
 * Returns:
 * 1: Message was sent.
 * 0: Socket closed the connection.
 * <0: Error occurred.
 */
int send_start_game_msg(int sock, char* player1, char* player2) {
	//the length of the message will not be bigger than 31.
	char buff[40];
	strcpy(buff, START_GAME_MESSAGE);
	sprintf(&buff[START_GAME_MESSAGE_LEN], "%s,%s;", player1, player2);

	return send_txt(sock, buff);
}

/*
 * Sends END_GAME message with nick of the winner.
 * The message will look like this: INFEND_GAME<winner>;
 *
 * Returns:
 * 1: Message was sent.
 * 2: Socket closed the connection.
 * <0: Error occurred.
 */
int send_end_game_msg(int sock, char* winner) {
    //the length of the message will not be bigger than 19.
    char buff[20];
    strcpy(buff, END_GAME_MESSAGE);
    sprintf(&buff[END_GAME_MESSAGE_LEN], "%s;", winner);

    return send_txt(sock, buff);
}

/*
 * Sends a start turn message to the socket.
 * The message will look like this: CMD<p1turnword><p2turnword>
 *
 * Returns:
 * 13: Message was sent.
 * 2: Socket closed the connection.
 * <0: Error occurred.
 */
int send_start_turn_msg(int sock, char* player1_turn_word, char* player2_turn_word) {
    char buff[START_TURN_MESSAGE_LEN+1];
    sprintf(buff, "CMD%s%s", player1_turn_word, player2_turn_word);

    return send_txt(sock, buff);
}

/*
 * Sends a message to the socket that the game is currently waiting for a player with
 * specified nick.
 *
 * Returns:
 * 1: Message was sent.
 * 2: Socket closed the connection.
 * <0: Error occurred.
 */
int send_waiting_for_player_msg(int sock, char* nick) {
    //the length of the message will not be bigger that 19;
    char buff[20];
    strcpy(buff, WAIT_FOR_PLAYER_MESSAGE);
    sprintf(&buff[WAIT_FOR_PLAYER_MESSAGE_LEN], "%s;", nick);

    return send_txt(sock, buff);
}
