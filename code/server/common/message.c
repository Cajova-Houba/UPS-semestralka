
#include "message.h"

/*
 * A matrix of possible message type chars.
 * [msg tye char index][msg type][possible char index]
 */
char POSSIBLE_MSG_TYPE_CHARS[3][3][2] = {
        {{'C','c'},{'I','i'},{'E','e'}},
        {{'M','m'},{'N','n'},{'R','r'}},
        {{'D','d'},{'F','f'},{'R','r'}}
};

char POSSIBLE_EXIT_MSG_CHARS[4][2] = {
        {'E','e'},
        {'X','x'},
        {'I','i'},
        {'T','t'},
};

char POSSIBLE_ALIVE_MSG_CHARS[5][2] = {
        {'A','a'},
        {'L','l'},
        {'I','i'},
        {'V','v'},
        {'E','e'}
};

/*
 * Checks if the message is alive message.
 * Returns ok if it is or 0 if it isn't.
 */
int is_alive(Message* message) {
    if (message == NULL) {
        return 0;
    }

    if (message->message_type_int == INF_TYPE && strcmp(message->content, ALIVE_MSG) == 0) {
        return OK;
    }

    return 0;
}
/*
 * Checks if the message is nick.
 * Returns ok if it is or 0 if it isn't.
 */
int is_nick(Message* message) {
    if (message == NULL) {
        return 0;
    }

    if (message->message_type_int == CMD_TYPE) {
        return OK;
    }

    return 0;
}

/*
 * Checks if the message is end turn.
 * Returns ok if it is or 0 if it isn't.
 */
int is_end_turn(Message* message) {
    if (message == NULL) {
        return 0;
    }

    if (message->message_type_int == INF_TYPE && strlen(message->content) == 2*TURN_WORD_LENGTH) {
        return OK;
    }

    return 0;
}

/*
 * Checks if the message is exit message.
 * Returns ok if it is or 0 if it isn't.
 */
int is_exit(Message* message) {
    if (message == NULL) {
        return 0;
    }

    if (message->message_type_int == INF_TYPE && strcmp(message->content, EXIT_MSG) == 0) {
        return OK;
    }

    return 0;
}



/*
 * Checks the message type and returns Message_type
 */
int get_msg_type(char* msg_type) {
    if(strcmp(msg_type, MSG_TYPE_CMD) == 0) {
        return CMD_TYPE;
    } else if(strcmp(msg_type, MSG_TYPE_ERR) == 0) {
        return ERR_TYPE;
    } else if(strcmp(msg_type, MSG_TYPE_INF) == 0) {
        return INF_TYPE;
    } else {
        return NO_TYPE;
    }
}

/*
 * Receives first three bytes from socket and checks if it's a
 * correct message type. If the type is received correctly,
 * it will be copied to the message structure.
 *
 * Returns:
 * OK: received msg type is correct
 * error from seneterror.h
 */
int recv_msg_type(int socket, int timeout, Message* message) {
    int msg_status = 0;
    char buffer[MAX_CONTENT_SIZE];
    int i = 0;
    int j = 0;
    int k = 0;
    int char_ok = 0;
    int m_type = -1;

    // get message type, by byte
    while(i < MSG_TYPE_LEN) {
        msg_status = recv_bytes_timeout(socket, &buffer[i], 1, timeout);
        if(msg_status < 0) {
            return msg_status;
        }



        switch (m_type) {
            // determine the message type by first char
            case -1:
                // filter out some white space chars
                if (buffer[i] == '\n' || buffer[i] == ' ') {
                    continue;
                }

                k = 0;
                while(k < 3 && m_type == -1){
                    j = 0;
                    while(j < 2 && m_type == -1) {
                        if(POSSIBLE_MSG_TYPE_CHARS[i][k][j] == buffer[i]) {
                            m_type = k;
                        }
                        j++;
                    }
                    k++;
                }

                // type still undecided
                if(m_type == -1) {
                    return ERR_MSG_TYPE;
                }
                break;


            // now we know the type and we can check other received chars
            default:
                char_ok = 0;
                for(j = 0; j < 2; j++) {
                    if(POSSIBLE_MSG_TYPE_CHARS[i][m_type][j] == buffer[i]) {
                        char_ok = 1;
                        break;
                    }
                }

                if(char_ok != 1) {
                    return ERR_MSG_TYPE;
                }
        }
        i++;
    }

    // copy retrieved message type to output buffer
    strcpy(message->message_type, buffer);

    return OK;
}

/*
 * Receives CMD message from socket. Message type is already expected to be taken
 * out from tcp stack. The received nick is copied to the message content (without length).
 *
 * Returns:
 * OK: Message was received.
 * CLOSED_CONNECTION
 * MSG_TIMEOUT
 * or error from senneterror.h (ERR_MSG_CONTENT,ERR_NICK_LENGTH)
 */
int recv_cmd_message(int socket, Message* message, int timeout) {
    char buffer[10];
    int nick_len = 0;
    int recv_status = 0;
    int i = 0;
    message->message_type_int = CMD_TYPE;

    // only possible CMD message to be received is nick

    // receive one more byte = length of the nick
    recv_status = recv_bytes_timeout(socket, buffer, 1, timeout);
    switch (recv_status) {
        case MSG_TIMEOUT:
        case CLOSED_CONNECTION:
        case ERR_MSG:
            return recv_status;
    }
    nick_len = buffer[0] - '0';
    if(nick_len < MIN_NICK_LENGTH || nick_len > MAX_NICK_LENGTH) {
        serror(MESSAGE_NAME, "Nick has wrong length.\n");
        return ERR_NICK_LEN;
    }

    // receive the rest of the nick
    while(i < nick_len) {
        recv_status = recv_bytes_timeout(socket, &buffer[i], 1, timeout);
        switch (recv_status) {
            case MSG_TIMEOUT:
            case CLOSED_CONNECTION:
            case ERR_MSG:
                return recv_status;
        }

        // check invalid chars
        if(buffer[i] < '0'  ||
           (buffer[i] > '9' && buffer[i] < 'A') ||
           (buffer[i] > 'Z' && buffer[i] < 'a') ||
           (buffer[i] > 'z')) {
            return ERR_MSG_CONTENT;
        }

        i++;
    }

    buffer[nick_len] = '\0';
    strcpy(message->content, buffer);

    return OK;
}

/*
 * Receives the error message and stores the error code (2 chars) in message
 * content. The message type is expected to be already taken out from tcp stack.
 * If the error code isn't recognized, GENERAL_ERROR is used.
 *
 * Returns:
 * OK: err message was received.
 * MSG_TIMEOUT
 * CLOSED_CONNECTION
 * or error from seneterror.h (ERR_MSG_CONTENT).
 */
int recv_err_message(int socket, Message* message, int timeout) {
    char buffer[10];
    int recv_status = 0;
    int err_code = 0;
    message->message_type_int = ERR_TYPE;

    // receive two bytes of error code
    recv_status = recv_bytes_timeout(socket, buffer, 2, timeout);
    switch (recv_status) {
        case MSG_TIMEOUT:
        case CLOSED_CONNECTION:
        case ERR_MSG:
            return recv_status;
    }
    buffer[2] = '\0';

    // try to convert the err code
    errno = 0;
    err_code = (int)strtol(buffer, NULL, 10);
    if(errno != 0) {
        // error during conversion occured
        return ERR_MSG_CONTENT;
    }
    strcpy(message->content, buffer);
    if((-err_code) < GENERAL_ERR || (-err_code) > ERR_LAST - 1) {
        message->error_code = GENERAL_ERR;
    } else {
        message->error_code = (senetError)-err_code;
    }

    return OK;
}

/*
 * Receives INF message and stores it to the message content. The message type is
 * expected to be already taken out from the tcp stack.
 *
 * Returns:
 * OK: inf message was received.
 * MSG_TIMEOUT
 * CLOSED_CONNECTION
 * or error from seneterror.h (ERR_MSG_CONTENT).
 */
int recv_inf_message(int socket, Message* message, int timeout) {
    char buffer[255];
    char log_msg[255];
    int recv_status = 0;
    int char_ok = 0;
    int i = 0;
    int k = 0;

    message->message_type_int = INF_TYPE;

    // receive first byte and determine if it's alive, exit or end turn message
    recv_status = recv_bytes_timeout(socket, buffer, 1, timeout);
    switch (recv_status) {
        case MSG_TIMEOUT:
        case CLOSED_CONNECTION:
        case ERR_MSG:
            return recv_status;
    }

    switch (buffer[0]) {
        case 'E':
        case 'e':
            // exit message
            // receive byte per byte and check it
            i = 1;
            while(i < EXIT_MSG_LEN) {
                char_ok = 0;
                recv_status = recv_bytes_timeout(socket, &buffer[i], 1, timeout);
                if (recv_status != 1) {
                    return recv_status;
                }
                for(k = 0; k < 2; k++) {
                    if(POSSIBLE_EXIT_MSG_CHARS[i][k] == buffer[i]) {
                        char_ok = 1;
                        break;
                    }
                }

                if(char_ok != 1) {
                    sprintf(log_msg, "Bad char at position %d detected while receiving exit message.\n", i);
                    serror(MESSAGE_NAME, log_msg);
                    return ERR_MSG_CONTENT;
                }

                i++;
            }

            buffer[EXIT_MSG_LEN] = '\0';
            strcpy(message->content, EXIT_MSG);
            return OK;

        case 'A':
        case 'a':
            // alive message
            // receive byte per byte and check it
            i = 1;
            while(i < ALIVE_MSG_LEN) {
                char_ok = 0;
                recv_status = recv_bytes_timeout(socket, &buffer[i], 1, timeout);
                if (recv_status != 1) {
                    return recv_status;
                }
                for(k = 0; k < 2; k++) {
                    if(POSSIBLE_ALIVE_MSG_CHARS[i][k] == buffer[i]) {
                        char_ok = 1;
                        break;
                    }
                }

                if(char_ok != 1) {
                    sprintf(log_msg, "Bad char at position %d detected while receiving alive message.\n", i);
                    serror(MESSAGE_NAME, log_msg);
                    return ERR_MSG_CONTENT;
                }

                i++;
            }

            buffer[ALIVE_MSG_LEN] = '\0';
            strcpy(message->content, ALIVE_MSG);
            return OK;
    }

    // check if the received byte is number. If it's a number
    // receive remaining 9 bytes of data.
    if(buffer[0] >= '0' && buffer[0] <= '9') {
        // receive turn words
        recv_status = recv_bytes_timeout(socket, &buffer[1], 2*TURN_WORD_LENGTH-1, timeout);
        if(recv_status != 2*TURN_WORD_LENGTH-1) {
            return recv_status;
        }

        buffer[2*TURN_WORD_LENGTH] = '\0';
        strcpy(message->content, buffer);
        return OK;
    } else {
        return ERR_MSG_CONTENT;
    }


}


/*
 * Receives message from socket.
 *
 * Returns:
 * OK: Message was received.
 * CLOSED_CONNECTION: Connection is closed.
 * MSG_TIMEOUT: Socket timet out.
 * or error from senneterror.h
 *
 * TODO: finish this
 */
int recv_message(int socket, Message* message, int timeout) {
    int msg_status = 0;
    char buffer[MAX_CONTENT_SIZE];

    // get message type
    message->message_type_int = NO_TYPE;
    msg_status = recv_msg_type(socket, timeout, message);
    if(msg_status != OK) {
        return msg_status;
    }

    // get content
    if(message->message_type[0] == 'C' || message->message_type[0] == 'c') {
        // receive cmd message
        return recv_cmd_message(socket, message, timeout);
    } else if(message->message_type[0] == 'E' || message->message_type[0] == 'e') {
        // receive err message
        return recv_err_message(socket, message, timeout);
    } else if(message->message_type[0] == 'I' || message->message_type[0] == 'i') {
        // receive inf message
        return recv_inf_message(socket, message, timeout);
    } else {
        return ERR_MSG_TYPE;
    }
}

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
	
	recv_status = recv_bytes_timeout(socket, nbuffer, MSG_TYPE_LEN, MAX_NICK_TIMEOUT);
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
 * Receives nick from socket and stores it to the buffer.
 * The buffer should have adequate size = MAX_NICK_LENGTH+1.
 *
 * Nick length is expected to be 1 char from '3' - '8'.
 *
 * The nick is checked only for length by this method.
 * The nick will end with \0.
 *
 * Returns:
 * 	OK : Nick was received.
 *  CLOSED_CONNECTION: Socket closed connection.
 *  MSG_TIMEOUT: Timeout.
 *  or error from seneterror.h
 */
int recv_nick_alphanum(int socket, char* buffer) {
	char nbuffer[4]; // 4 is max length of msg type + '\0'
	int nick_len = 0;
	int recv_status = 0;

	// get message type
	recv_status = recv_bytes_timeout(socket, nbuffer, MSG_TYPE_LEN, MAX_NICK_TIMEOUT);
	switch (recv_status) {
		case CLOSED_CONNECTION:
			serror(MESSAGE_NAME, "Connection closed, cannot receive nick.\n");
			return CLOSED_CONNECTION;
		case ERR_MSG:
			serror(MESSAGE_NAME,"Error while receiving nick message.\n");
			return ERR_MSG;
		case MSG_TIMEOUT:
			serror(MESSAGE_NAME,"Timed out while waiting for nick message.\n");
			return MSG_TIMEOUT;
	}

	// the type must be CMD
	nbuffer[3] = '\0';
	if(strcmp(nbuffer, MSG_TYPE_CMD) != 0) {
		return ERR_MSG_TYPE;
	}

	// receive one more byte to gain the length of the nick
	recv_status = recv_bytes(socket, nbuffer, 1);
	if(recv_status != 1) {
		serror(MESSAGE_NAME, "Error while receiving the nick length.\n");
		return ERR_MSG;
	}
	nick_len = nbuffer[0] - '0';
	if(nick_len < MIN_NICK_LENGTH || nick_len > MAX_NICK_LENGTH) {
		serror(MESSAGE_NAME, "Nick has wrong length.\n");
		return ERR_MSG_CONTENT;
	}

	// receive the rest of the nick
	recv_status = recv_bytes(socket, buffer, nick_len);
	if(recv_status != nick_len) {
		serror(MESSAGE_NAME, "Error while receiving the rest of the nick.\n");
		return recv_status;
	}

	buffer[nick_len] = '\0';


	return OK;
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
    recv_status = recv_bytes_timeout(socket, nbuffer, MSG_TYPE_LEN, MAX_TURN_WAITING_TIMEOUT);
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
	recv_status = recv_bytes_timeout(socket, nbuffer, EXIT_MSG_LEN, MAX_TURN_WAITING_TIMEOUT);
	if (recv_status == EXIT_MSG_LEN && strcmp(nbuffer, EXIT_MSG) == 0) {
		sdebug(MESSAGE_NAME, "EXIT message received.\n");
		return 2;
	} else {
		// received message is a turn word, copy 4 bytes
		for(i = 0; i < EXIT_MSG_LEN; i++) {
			player1_turn_word[i] = nbuffer[i];
		}
	}

    /* receive the remaining 6 byte of the first turn word */
    recv_status = recv_bytes(socket, &player1_turn_word[TURN_WORD_LENGTH-1], 1);
    if (recv_status == MSG_TIMEOUT) {
        return MSG_TIMEOUT;
    } else if(recv_status != 1) {
        serror(MESSAGE_NAME, "Error while receiving first turn word of end turn message.\n");
        return ERR_MSG_CONTENT;
    }

    /* second 10 bytes are 2 turn word */
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
int recv_end_turn_alphanum(int socket, char* player1_turn_word, char* player2_turn_word) {
    return ERR_MSG;
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

	sdebug(MESSAGE_NAME, "Waiting for end turn message.\n");
	recv_status = recv_bytes_timeout(socket, nbuffer, MSG_TYPE_LEN, MAX_NICK_TIMEOUT);
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
	recv_status = recv_bytes_timeout(socket, nbuffer, EXIT_MSG_LEN, MAX_NICK_TIMEOUT);
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
 * OK: message was sent.
 * ERR_MSG: error while sendig message.
 * CLOSED_CONNECTION: connection closed.
 * 
 */
int send_ok_msg(int socket) {
    int send_status = 0;
    char buffer[50];
    sprintf(buffer,"%s%s\n\0", MSG_TYPE_INF, OK_MESSAGE);
    send_status = send_txt(socket, buffer);
    if(send_status != MSG_TYPE_LEN+OK_MESSAGE_LEN+1) {
        return send_status;
    } else {
        return OK;
    }
}

/*
 * Sends ERRXX message, where XX is the error code
 * 
 * Returns:
 * OK: Err message sent.
 * ERR_MSG: Error while sending message.
 * CLOSED_CONNECTION: Connection closed.
 */
int send_err_msg(int socket, int err_code) {
	char buff[50];
    int state = 0;
    if(err_code < 0) {
        err_code = -err_code;
    }
    sprintf(buff, "%s%02d\n\0",MSG_TYPE_ERR, err_code);

    state = send_txt(socket, buff);
	if(state > 0) {
        return OK;
    } else {
        return state;
    }
}

/*
 * Sends START_GAME message with nicks of the 1st and 2nd player.
 * The message will look like this: INFSTART_GAME<player1>,<player2>;.
 *
 * Returns:
 * TODO: change to OK/ERR
 * 1: Message was sent.
 * 0: Socket closed the connection.
 * <0: Error occurred.
 */
int send_start_game_msg(int sock, char* player1, char* player2) {
	//the length of the message will not be bigger than 31.
	char buff[40];
	strcpy(buff, START_GAME_MESSAGE);
	sprintf(&buff[START_GAME_MESSAGE_LEN], "%s,%s;\n", player1, player2);

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
 * TODO: change to ok
 * Returns:
 * 13: Message was sent.
 * 2: Socket closed the connection.
 * <0: Error occurred.
 */
int send_start_turn_msg(int sock, char* player1_turn_word, char* player2_turn_word) {
    char buff[50];
    sprintf(buff, "CMD%s%s\n\0", player1_turn_word, player2_turn_word);

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
