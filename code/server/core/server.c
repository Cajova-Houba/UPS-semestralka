#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

#include <pthread.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>

#include "../common/common.h"
#include "../common/nick_val.h"

#include "../common/message.h"
#include "parallel.h"
#include "game.h"
#include "limits.h"


/*
 * Possible parameter names.
 */
#define HELP_PARAM_NAME         "help"
#define PORT_PARM_NAME          "port"
#define IP_PARAM_NAME           "ip"


/*
 * Structure which will be passed as an argument to a new thread.
 */
typedef struct {
	int thread_number;
	int socket;
    uint32_t addr;
    int port;
} thread_arg;

/*
 * Incoming connections. Two players will be choose.
 */ 
pthread_t connections[MAX_CONNECTIONS];

/*
 * Timer threads for disconnected players.
 */
pthread_t timer_threads[MAX_TIMER_THREADS];


/*
 * Index in the connections/timer_threads array. Thread which is to be joined
 * will set this index and wake the cleaning thread.
 * 
 * If the cleaner_index is set to -100, cleaning loop will stop and the
 * cleaning thread will end.
 */ 
int cleaner_index;

/*
 * Game instance.
 */
//Game_struct game;

Game_struct games[MAX_GAMES];

/*
 * Returns OK if the game id is ok.
 */
int is_game_id_ok(int game_id) {
    if(game_id >= 0 && game_id < MAX_GAMES) {
        return OK;
    } else {
        return 0;
    }
}

/*
 * =====================================================================
 * FUNCTIONS FOR CRITICAL SECTION
 * =====================================================================
 */ 

/*
 * Handles the criticval section and calls check_winning_conditions() from
 * game.h.
 */
void server_check_winning_conditions(int my_game_id, int* winner) {
    pthread_mutex_lock(&mutex_game);

    if(is_game_id_ok(my_game_id) == OK) {
        *winner = check_winning_conditions(games[my_game_id].players[PLAYER_1].turn_word, games[my_game_id].players[PLAYER_1].turn_word);
    }

    pthread_mutex_unlock(&mutex_game);
}

/*
 * Handles the critical seciton and updates the players stones.
 */
void server_update_players_stones(int my_game_id, char* p1_stones, char* p2_stones) {
    pthread_mutex_lock(&mutex_game);

    if (is_game_id_ok(my_game_id) == OK) {
        update_players_stones(&games[my_game_id].players[PLAYER_1], p1_stones);
        update_players_stones(&games[my_game_id].players[PLAYER_2], p2_stones);
    }

    pthread_mutex_unlock(&mutex_game);
}

/*
 * Handles the critical section and validates the turn.
 */
void server_validate_turn(int my_game_id, int turn, char* p1_new_tw, char* p2_new_tw, int* result) {
    pthread_mutex_lock(&mutex_game);

    if(is_game_id_ok(my_game_id) == OK) {
        *result = validate_turn(games[my_game_id].players[PLAYER_1].turn_word, games[my_game_id].players[PLAYER_2].turn_word, p1_new_tw, p2_new_tw, turn);
    }

    pthread_mutex_unlock(&mutex_game);
}

/*
 * Handles the critical section and prints player to the buffer.
 */
void server_print_player(int game_id, int player_id, char* buffer, int new_line) {
    pthread_mutex_lock(&mutex_get_player);

    if(is_game_id_ok(game_id) == OK && (player_id == PLAYER_1 || player_id == PLAYER_2)) {
        print_player(&(games[game_id].players[player_id]), buffer, new_line);
    }

    pthread_mutex_unlock(&mutex_get_player);
}

/*
 * Handles the critical section and calls leave_game().
 */
void server_leave_from_game(int my_game_id, int my_player) {
    pthread_mutex_lock(&mutex_game);

    if(is_game_id_ok(my_game_id) == OK) {
        leave_from_game(&games[my_game_id], my_player);
    }

    pthread_mutex_unlock(&mutex_game);
}

/*
 * Stores the first free game found to the game variable.
 * If no ree game is found, NO_GAME_SLOT_FREE is stored to game_id.
 *
 * If the free game is found, the player is also initialized.
 * If both players are initialized on returned game, it is assumed that the
 * player thread which called this function is player 2.
 *
 * my_player is initialized to either 0 or 1 if the free game is found.
 *
 * Game is free if it has FREE flag set to 1 and has 1 player at max.
 */
void get_free_game_bind_player(int* game_id, int* my_player, char* nick, uint32_t addr, int port, int socket) {
    int i = 0;
    int game_found = -1;
    pthread_mutex_lock(&mutex_game);

    for(i = 0; i < MAX_GAMES; i++) {
        if(is_game_free(&games[i]) == OK) {
            game_found = i;
            break;
        }
    }

    if(game_found == -1) {
        *my_player = NO_GAME_SLOT_FREE;
    } else {
        if(is_player_free(&(games[game_found].players[PLAYER_1])) == OK) {
            // initialize as the first player
            initialize_player(&(games[game_found].players[PLAYER_1]), 0, nick, socket, 1, addr, port);
            *my_player = PLAYER_1;
        } else {
            // initialize as the second player
            initialize_player(&(games[game_found].players[PLAYER_2]), 1, nick, socket, 0, addr, port);
            *my_player = PLAYER_2;
        }

        *game_id = game_found;
    }

    pthread_mutex_unlock(&mutex_game);
}

/*
 * Resets the game so that it can be reused.
 */
void server_reset_game(int game_id) {
    pthread_mutex_lock(&mutex_game);

    if(is_game_id_ok(game_id) == OK) {
        if(is_game_free(&games[game_id]) != OK) {
            reset_game(&games[game_id]);
        }
    }


    pthread_mutex_unlock(&mutex_game);
}

/*
 * Handles the critical section and calls init_new_game.
 */
void server_init_new_game(int my_game_id) {
    pthread_mutex_lock(&mutex_game);

    if(is_game_id_ok(my_game_id) == OK) {
        init_new_game(&games[my_game_id]);
    }

    pthread_mutex_unlock(&mutex_game);
}

/*
 * Handles the critical section and copies players turn word to buffer.
 */
void server_get_turn_word(int my_game_id, int player_id, char* buffer) {
    pthread_mutex_lock(&mutex_game);

    if(is_game_id_ok(my_game_id) == OK && (player_id == PLAYER_1 || player_id == PLAYER_2)) {
        strcpy(buffer, games[my_game_id].players[player_id].turn_word);
    }

    pthread_mutex_unlock(&mutex_game);
}

/*
 * Stores the current value of game_started flag in variable.
 * Critical section handled.
 */
void server_get_game_started(int my_game_id, int *variable) {
	pthread_mutex_lock(&mutex_game_started);

	if(is_game_id_ok(my_game_id) == OK) {
        *variable = get_game_flag(&(games[my_game_id].flags), GAME_STARTED_FLAG);
    }

	pthread_mutex_unlock(&mutex_game_started);
}

/*
 * Handles the critical section and calls a start_game()
 * function from the game.h.
 */
void server_start_game(int my_game_id) {
	pthread_mutex_lock(&mutex_game_started);

    if(is_game_id_ok(my_game_id) == OK) {
        start_game(&games[my_game_id]);
    }

	pthread_mutex_unlock(&mutex_game_started);
}

/*
 * Handles the critical section and calls set_winner() and
 * end_game() functions from the game.h.
 */
void server_set_winner(int my_game_id, int winner){
    pthread_mutex_lock(&mutex_winner);

    if (is_game_id_ok(my_game_id) == OK) {
        set_winner(&games[my_game_id], winner);
        end_game(&games[my_game_id]);
    }

    pthread_mutex_unlock(&mutex_winner);
}

/*
 * Handles the critical section and stores the winner of the game
 * to variable. If noone has won the game, stores -1 to variable.
 */
void server_get_winner(int my_game_id, int* variable) {
    pthread_mutex_lock(&mutex_winner);

    if(is_game_id_ok(my_game_id) == OK) {
        *variable = get_winner(&games[my_game_id]);
    }

    pthread_mutex_unlock(&mutex_winner);
}

/*
 * Handles the critical section and stores the TURN flag in the variable.
 * 0 = 1st player, 1 = 2nd player.
 */
void server_get_current_turn(int my_game_id, int* variable) {
    pthread_mutex_lock(&mutex_get_turn);

    if(is_game_id_ok(my_game_id) == OK) {
        *variable = get_game_flag(&(games[my_game_id].flags), TURN_FLAG);
    }

    pthread_mutex_unlock(&mutex_get_turn);
}

/*
 * Handles the critical section and if the game has already ended,
 * stores OK into variable.
 */
void server_is_game_end(int my_game_id, int *variable) {
    int i = 0;
    pthread_mutex_lock(&mutex_winner);

    if(is_game_id_ok(my_game_id) == OK) {
        i = get_game_flag(&(games[my_game_id].flags), GAME_ENDED_FLAG);
        if(i == 1) {
            *variable = OK;
        } else {
            *variable = 0;
        }
    }


    pthread_mutex_unlock(&mutex_winner);
}

/*
 * Handles the critical section and changes the turn.
 */
void server_switch_turn(int my_game_id) {
    int i = 0;
    pthread_mutex_lock(&mutex_get_turn);

    if(is_game_id_ok(my_game_id) == OK) {
        i = get_game_flag(&(games[my_game_id].flags), TURN_FLAG);
        switch_turn(&games[my_game_id], i);
    }

    pthread_mutex_unlock(&mutex_get_turn);
}

/*
 * Handles the critical section and stores the actual number
 * of initialized players in game to variable.
 */
void server_get_players_count(int my_game_id, int* variable) {
    pthread_mutex_lock(&mutex_game);

    if(is_game_id_ok(my_game_id) == OK) {
        *variable = get_players_count(&games[my_game_id]);
    }

    pthread_mutex_unlock(&mutex_game);
}

/*
 * This function stores the first free connection slot in the variable.
 * Critical section handled.
 */ 
void get_curr_conn(int *variable) {
	int i;
	pthread_mutex_lock(&mutex_curr_conn);

	for(i = 0; i < MAX_CONNECTIONS; i++) {
		if(connections[i] == NO_THREAD) {
			*variable = i;
			break;
		}
	}

    if(i == MAX_CONNECTIONS) {
        *variable = MAX_CONNECTIONS;
    }


	pthread_mutex_unlock(&mutex_curr_conn);
}

/*
 * Stores actual value of cleaner_index to the variable.
 */ 
void get_cleaner_index(int *variable) {
	pthread_mutex_lock(&mutex_cleaner_index);
	
	*variable = cleaner_index;
	
	pthread_mutex_unlock(&mutex_cleaner_index);
}

/*
 * Sets the cleaner_index value and wakes the cleaner thread.
 */ 
void clean_me(int thread_index) {
	/* add me to the cleaning queue */
	sem_wait(&cleaning_queue_sem);	
		
	cleaner_index = thread_index;
	
	/* wake up, Mr. Cleaner */
	sem_post(&cleaner_sem);	
}

/*
 * Sets the cleaner_index to -100 which means that the cleaning loop
 * will be stopped. No need to call sem_post() after this function.
 */ 
void shutdown_cleaner() {
	pthread_mutex_lock(&mutex_cleaner_index);
	
	cleaner_index = -100;
	sem_post(&cleaner_sem);
	
	pthread_mutex_unlock(&mutex_cleaner_index);
}

/*
 * Stores nick of the player to buffer.
 */
void get_player_nick(int my_game_id, int player, char* buffer) {
    if (player < 0 || player > 1) {
        return;
    }
    pthread_mutex_lock(&mutex_game);
    if(is_game_id_ok(my_game_id) == OK) {
        strcpy(buffer, games[my_game_id].players[player].nick);
    }

    pthread_mutex_unlock(&mutex_game);
}

/*
 * Handles the critical section and calls an end_game()
 * function from the game.h.
 * Also clears the nicks in game struct.
 */
void server_end_game(int my_game_id, int my_player) {
    pthread_mutex_lock(&mutex_game);

    if(is_game_id_ok(my_game_id) == OK) {
        end_game(&games[my_game_id]);
        leave_from_game(&games[my_game_id], my_player);
    }

    pthread_mutex_unlock(&mutex_game);
}





/*
 * =====================================================================
 * THREAD FUNCTIONS
 * =====================================================================
 */

/*
 * Thread function which will wait for signal to clean thread slots.
 */
void *cleaner(void *arg) {
	int thread_index;
	char log_msg[50];
	
	sdebug(SERVER_NAME, "Starting cleaning thread.\n");
	
	while(1) {
		/* wait for signal - conditional variable*/
		sem_wait(&cleaner_sem);
		
		/* clean */
		get_cleaner_index(&thread_index);
        if(thread_index >= 0 && thread_index <= MAX_CONNECTIONS) {
            sprintf(log_msg, "Cleaning player thread on index: %d.\n", thread_index);
            sdebug(SERVER_NAME, log_msg);
            if(connections[thread_index] == NO_THREAD) {
                sprintf(log_msg, "Player thread on index %d is already cleaned.\n",thread_index);
                sdebug(SERVER_NAME, log_msg);
            } else {
                pthread_join(connections[thread_index], NULL);
                connections[thread_index] = NO_THREAD;
            }
        } else if( thread_index >= -MAX_TIMER_THREADS && thread_index <= -1) {
            sprintf(log_msg, "Cleaning timer thread on index: %d.\n", -thread_index -1);
            sdebug(SERVER_NAME, log_msg);
            pthread_join(timer_threads[-thread_index -1], NULL);
            timer_threads[-thread_index -1] = NO_THREAD;
        } else {
            break;
        }
		
		/* call sem_post for the waiting queue */
		sem_post(&cleaning_queue_sem);
	}
	
	sdebug(SERVER_NAME, "Cleaning thread is shuting down.\n");
	
	return NULL;
}

/*
 * Logs a debug message.
 */
void debug_player_message(char* buffer, char * pattern, int player_num) {
    sprintf(buffer, pattern, player_num);
    sdebug(PLAYER_THREAD_NAME, buffer);
}
 
/*
 * Handles the critical section.
 * Calls function form nick_val to check nick.
 */  
void server_check_nick(int my_game_id, char *nick, char *err_msg, int* res)
{
    int check_res;
    pthread_mutex_lock(&mutex_game);

    check_res = check_nickname(nick, NULL, games[my_game_id].players);

    switch(check_res) {
//		case ERR_NICK_TOO_SHORT:
//			sprintf(err_msg,"Nick '%s' is too short.\n",nick);
//			return 0;
//		case ERR_NICK_TOO_LONG:
//			sprintf(err_msg,"Nick '%s' is too long.\n",nick);
//			return 0;
//		case ERR_FIRST_CHAR_INV:
//			sprintf(err_msg,"Nick '%s' starts with invalid character.\n",nick);
//			return 0;
//		case ERR_CONTAINS_INV_CHAR:
//			sprintf(err_msg,"Nick '%s' contains invalid characters.\n",nick);
//			return 0;
        case ERR_NICKNAME:
            sprintf(err_msg,"Nick '%s' is too short or contains bad characters.\n", nick);
            *res = ERR_NICKNAME;
        case ERR_NICK_EXISTS:
            sprintf(err_msg,"Nick '%s' already exists.\n",nick);
            *res = ERR_NICK_EXISTS;
    }

    *res = OK;

    pthread_mutex_unlock(&mutex_game);
}

/*
 * Waits for end turn to be received. Timeout should be very small, about 1 second.
 * For every non-end turn message timeout will be added to counter and when the counter
 * reaches MAX_TURN_WAITING_TIMEOUT, makes other player a winner and returns STOP_GAME_LOOP.
 *
 * Returns:
 * OK: Nick is received.
 * STOP_GAME_LOOP: error while receiving nick.
 */
int wait_for_end_turn(int my_game_id, int socket, int timeout, int my_player, int other_player, char* p1_tw, char* p2_tw) {
    Message message;
    char log_msg[255];
    int msg_status = 0;
    int end_turn_recv = 0;
    int i = 0;
    int cntr = 0;
    int send_msg = 0;

    while (end_turn_recv != OK && cntr < MAX_TURN_WAITING_TIMEOUT) {

        // receive message
        msg_status = recv_message(socket, &message, timeout);

        // handle connection errors
        switch(msg_status) {
            case MSG_TIMEOUT:
                cntr += timeout;
                continue;
            case CLOSED_CONNECTION:
                sprintf(log_msg, "Socket %d closed the connection while waiting for the end turn message.\n", socket);
                sdebug(PLAYER_THREAD_NAME,log_msg);
                server_set_winner(my_game_id,other_player);
                return STOP_GAME_LOOP;
        }

        // handle message errors
        if(msg_status != OK) {
            sprintf(log_msg, "Error (%d) while receiving message from socket %d.\n", msg_status, socket);
            serror(PLAYER_THREAD_NAME, log_msg);
            if (msg_status < GENERAL_ERR || msg_status >= ERR_LAST) {
                msg_status = GENERAL_ERR;
            }
            send_msg = send_err_msg(socket, msg_status);
            if(send_msg == CLOSED_CONNECTION) {
                sprintf(log_msg, "Socket %d closed the connection while waiting for the end turn message.\n", socket);
                sdebug(PLAYER_THREAD_NAME,log_msg);
                server_set_winner(my_game_id,other_player);
                return STOP_GAME_LOOP;
            }
            cntr += timeout;

        } else {
            // handle possible messages
            if (is_alive(&message) == OK) {
                // response to is alive message
                send_msg = send_ok_msg(socket);
                if(send_msg == CLOSED_CONNECTION) {
                    sprintf(log_msg, "Socket %d closed the connection while waiting for the end turn message.\n", socket);
                    sdebug(PLAYER_THREAD_NAME,log_msg);
                    server_set_winner(my_game_id,other_player);
                    return STOP_GAME_LOOP;
                }
                cntr += timeout;

            } else if (is_exit(&message) == OK) {
                // handle exit => other player wins
                sprintf(log_msg, "Player %d quit.\n", my_player);
                sdebug(PLAYER_THREAD_NAME,log_msg);
                server_set_winner(my_game_id, other_player);
                return STOP_GAME_LOOP;

            } else if (is_end_turn(&message) == OK) {
                // end turn message
                // copy turn word from message to provided variables
                for(i = 0; i < TURN_WORD_LENGTH; i++) {
                    p1_tw[i] = message.content[i];
                    p2_tw[i] = message.content[i+TURN_WORD_LENGTH];
                }
                sprintf(log_msg, "End turn message received: %s\n", message.content);
                sinfo(PLAYER_THREAD_NAME, log_msg);
                end_turn_recv = OK;

            } else {
                // something else
                send_msg = send_err_msg(socket, ERR_UNEXPECTED_MSG);
                if(send_msg == CLOSED_CONNECTION) {
                    sprintf(log_msg, "Socket %d closed the connection while waiting for the end turn message.\n", socket);
                    sdebug(PLAYER_THREAD_NAME,log_msg);
                    server_set_winner(my_game_id, other_player);
                    return STOP_GAME_LOOP;
                }
                cntr += timeout;
            }
        }
    }

    if (cntr >= MAX_TURN_WAITING_TIMEOUT) {
        sprintf(log_msg, "Max time for player's %d end turn message reached.\n", my_player);
        sdebug(PLAYER_THREAD_NAME,log_msg);
        server_set_winner(my_game_id, other_player);
        return STOP_GAME_LOOP;
    }

    return OK;
}


/*
 * Waits for nick to be received. Received and checked nick is stored to buffer.
 *
 * Returns:
 * OK: Nick is received and checked.
 * 0: error while receiving nick. The error will be handled in this function.
 */
int wait_for_nick(int my_game_id, int socket, char* buffer) {
    Message message;
    char log_msg[255];
    int msg_status = 0;
    int attempt = 0;
    int nick_ok = 0;
    int send_msg = 0;
    while (nick_ok != OK && attempt < MAX_NICK_ATTEMPTS) {
        msg_status = recv_message(socket, &message, MAX_NICK_TIMEOUT);

        // handle connection errors
        switch (msg_status) {
            case MSG_TIMEOUT:
                sprintf(log_msg, "Socket %d timed out while waiting for nick.\n", socket);
                serror(PLAYER_THREAD_NAME, log_msg);
                send_err_msg(socket, ERR_TIMEOUT);
                return 0;
            case CLOSED_CONNECTION:
                sprintf(log_msg, "Socket %d closed connection.\n", socket);
                return 0;
        }

        // handle message errors
        if(msg_status != OK) {
            sprintf(log_msg, "Error while receiving message from socket %d.\n", socket);
            serror(PLAYER_THREAD_NAME, log_msg);
            if(msg_status < GENERAL_ERR || msg_status >= ERR_LAST) {
                msg_status = GENERAL_ERR;
            }
            send_msg = send_err_msg(socket, msg_status);
            if(send_msg == CLOSED_CONNECTION) {
                sprintf(log_msg, "Socket %d closed connection.\n", socket);
                return 0;
            }
            attempt++;

        // handle messages
        } else {

            if(is_alive(&message) == OK) {
                // response to is alive message
                send_ok_msg(socket);
            } else if (is_exit(&message) == OK) {
                // exit received, disconnect
                sprintf(log_msg, "Socket %d quit.\n", socket);
                sinfo(PLAYER_THREAD_NAME, log_msg);
                return 0;

            } else if (is_nick(&message) != OK) {
                // something other than nick
                send_msg = send_err_msg(socket, ERR_UNEXPECTED_MSG);
                if(send_msg == CLOSED_CONNECTION) {
                    sprintf(log_msg, "Socket %d closed connection.\n", socket);
                    return 0;
                }

                attempt++;

            } else {
                // message is nick, check it
                sprintf(log_msg,"Nick received: %s\n",message.content);
                sinfo(PLAYER_THREAD_NAME,log_msg);
                // check nick
                server_check_nick(my_game_id, message.content, log_msg, &nick_ok);
                // nick validation failed
                if(nick_ok != OK) {
                    serror(PLAYER_THREAD_NAME, log_msg);

                    /* send error */
                    send_msg = send_err_msg(socket, nick_ok);
                    if(send_msg == CLOSED_CONNECTION) {
                        sprintf(log_msg, "Socket %d closed connection.\n", socket);
                        return 0;
                    }

                    attempt++;
                } else {

                    // nick is OK return OK
                    strcpy(buffer, message.content);
                    return OK;
                }

            }
        }
    } // while end

    if(attempt == MAX_NICK_ATTEMPTS) {
        sprintf(log_msg, "Maximum number of nick attempts reached for socket %d.\n", socket);
        send_err_msg(socket, ERR_TOO_MANY_ATTEMPTS);
    }

    return 0;
}

/*
 * Call when the game is to be ended adn the winner is set.
 */
void player_thread_end_game(int my_game_id, int socket, int my_player) {
    /* end game */
    char buffer[255];
    int winner;
    strcpy(buffer, "\0");
    server_get_winner(my_game_id, &winner);
    get_player_nick(my_game_id, winner, buffer);
    send_end_game_msg(socket, buffer);
    server_end_game(my_game_id, my_player);

}

/*
 * Sends a start turn message to player.
 *
 * Returns:
 *  OK: start turn message sent
 *  STOP_GAME_LOOP: error, game loop should be broken.
 */
int send_start_turn_to_player(int my_game_id, int socket, int my_player, int other_player) {
    int msg_status = 0;
    char log_msg[255];
    char p1tw[50];
    char p2tw[50];
    int tmp = 0;
    server_is_game_end(my_game_id, &tmp);

    if(tmp == OK) {
        return STOP_GAME_LOOP;
    } else {
        debug_player_message(log_msg, "Sending start turn message to player %d.\n", my_player);
        server_get_turn_word(my_game_id, PLAYER_1, p1tw);
        server_get_turn_word(my_game_id, PLAYER_2, p2tw);
        msg_status = send_start_turn_msg(socket, p1tw, p2tw);
//        debug_player_message(log_msg, "Start turn status: %d\n", msg_status);
        if(msg_status < 2) {
            serror(PLAYER_THREAD_NAME, "Error while sending start turn message.\n");
            //set the other player as winner
            server_set_winner(my_game_id, other_player);
            server_end_game(my_game_id, my_player);
            return  STOP_GAME_LOOP;
        }
    }

    return  OK;
}

/*
 * Sets clean me and closes the socket.
 * Also frees the argument structure.
 */
void clean_up(thread_arg *arg) {
    int tn = arg->thread_number;
    close(arg->socket);
    free(arg);
    clean_me(tn);
}

/*
 * Handles incoming messages while the thread is waiting for another player to start the game.
 *
 * Returns:
 * OK: message handled.
 * STOP_GAME_LOOP: if error occurs and thread should stop waiting.
 */
int handle_message_waiting(int socket, int timeout, int my_player) {
    int recv_status = 0;
    int msg_status = 0;
    Message received_message;
    char log_msg[255];

    recv_status = recv_message(socket, &received_message, timeout);
    switch (recv_status) {
        case OK:
            if(is_alive(&received_message) == OK) {
                msg_status = send_ok_msg(socket);
                if(msg_status == CLOSED_CONNECTION) {
                    sprintf(log_msg, "Player %d closed connection.\n", my_player);
                    sdebug(PLAYER_THREAD_NAME, log_msg);
                    return STOP_GAME_LOOP;
                }
            } else if(is_exit(&received_message) == OK) {
                sprintf(log_msg, "Player %d quit.\n", my_player);
                sdebug(PLAYER_THREAD_NAME, log_msg);
                return STOP_GAME_LOOP;
            } else {
                // send not my turn error
                msg_status = send_err_msg(socket, ERR_UNEXPECTED_MSG);
                if(msg_status == CLOSED_CONNECTION) {
                    sprintf(log_msg, "Player %d closed connection.\n", my_player);
                    sdebug(PLAYER_THREAD_NAME, log_msg);
                    return STOP_GAME_LOOP;
                }
            }
            break;

        case MSG_TIMEOUT:
            // send alive
            msg_status = send_alive_msg(socket);
            if(msg_status == CLOSED_CONNECTION) {
                sprintf(log_msg, "Player %d closed connection.\n", my_player);
                sdebug(PLAYER_THREAD_NAME, log_msg);
                return STOP_GAME_LOOP;
            }

            // receive ok
            recv_status = recv_message(socket, &received_message, ALIVE_TIMEOUT);
            if(recv_status != OK || !is_ok(&received_message)) {
                sprintf(log_msg, "Player %d isn't responding.\n", my_player);
                sdebug(PLAYER_THREAD_NAME, log_msg);
                send_err_msg(socket, ERR_MSG);
                return STOP_GAME_LOOP;
            }

            return OK;

        case CLOSED_CONNECTION:
            sprintf(log_msg, "Player %d closed connection.\n", my_player);
            sdebug(PLAYER_THREAD_NAME, log_msg);
            return STOP_GAME_LOOP;

        default:
            // send error back to client
            if(recv_status < 0) {
                if (recv_status >= GENERAL_ERR && recv_status <= ERR_LAST - 1) {
                    msg_status = send_err_msg(socket, recv_status);
                    if(msg_status == CLOSED_CONNECTION) {
                        sprintf(log_msg, "Player %d closed connection.\n", my_player);
                        sdebug(PLAYER_THREAD_NAME, log_msg);
                        return STOP_GAME_LOOP;
                    }
                    return OK;
                } else {
                    msg_status = send_err_msg(socket, GENERAL_ERR);
                    if(msg_status == CLOSED_CONNECTION) {
                        sprintf(log_msg, "Player %d closed connection.\n", my_player);
                        sdebug(PLAYER_THREAD_NAME, log_msg);
                        return STOP_GAME_LOOP;
                    }
                    return OK;
                }
            }

    }

    return OK;
}

/*
 * Handles incoming messages while thread is currently waiting for
 * its turn. If needed (due to connection error for example), sets the other
 * player as winner and ends the game.
 *
 * The timeout should be around 1s (maybe even smaller).
 *
 * Returns STOP_GAME_LOOP if the game loop should stop. Otherwise returns OK.
 */
int handle_message_waiting_turn(int my_game_id, int socket, int timeout, int my_player, int other_player) {
    int recv_status = 0;
    int msg_status = 0;
    char log_msg[255];
    Message received_message;

    recv_status = recv_message(socket, &received_message, timeout);
    switch (recv_status) {
        case OK:
            if(is_alive(&received_message) == OK) {
                msg_status = send_ok_msg(socket);
                if(msg_status == CLOSED_CONNECTION) {
                    sprintf(log_msg, "Player %d closed connection.\n", my_player);
                    sdebug(PLAYER_THREAD_NAME, log_msg);
                    return STOP_GAME_LOOP;
                }
            } else if (is_exit(&received_message) == OK) {
                // exit, other player wins
                server_set_winner(my_game_id, other_player);
                return STOP_GAME_LOOP;
            } else {
                // send not my turn error
                msg_status = send_err_msg(socket, ERR_NOT_MY_TURN);
                if(msg_status == CLOSED_CONNECTION) {
                    sprintf(log_msg, "Player %d closed connection.\n", my_player);
                    sdebug(PLAYER_THREAD_NAME, log_msg);
                    return STOP_GAME_LOOP;
                }
            }
            break;

        case MSG_TIMEOUT:
            // send alive
            msg_status = send_alive_msg(socket);
            if(msg_status == CLOSED_CONNECTION) {
                sprintf(log_msg, "Player %d closed connection.\n", my_player);
                sdebug(PLAYER_THREAD_NAME, log_msg);
                return STOP_GAME_LOOP;
            }

            // receive ok
            recv_status = recv_message(socket, &received_message, ALIVE_TIMEOUT);
            if(recv_status != OK || !is_ok(&received_message)) {
                sprintf(log_msg, "Player %d isn't responding.\n", my_player);
                sdebug(PLAYER_THREAD_NAME, log_msg);
                send_err_msg(socket, ERR_MSG);
                return STOP_GAME_LOOP;
            }

            return OK;

        case CLOSED_CONNECTION:
            server_set_winner(my_game_id, other_player);
            return STOP_GAME_LOOP;

        default:
            // send error back to client
            if(recv_status < 0) {
                if (recv_status >= GENERAL_ERR && recv_status <= ERR_LAST - 1) {
                    msg_status = send_err_msg(socket, recv_status);
                    if(msg_status == CLOSED_CONNECTION) {
                        sprintf(log_msg, "Player %d closed connection.\n", my_player);
                        sdebug(PLAYER_THREAD_NAME, log_msg);
                        return STOP_GAME_LOOP;
                    }
                    return OK;
                } else {
                    msg_status = send_err_msg(socket, GENERAL_ERR);
                    if(msg_status == CLOSED_CONNECTION) {
                        sprintf(log_msg, "Player %d closed connection.\n", my_player);
                        sdebug(PLAYER_THREAD_NAME, log_msg);
                        return STOP_GAME_LOOP;
                    }
                    return OK;
                }
            }

    }

    return OK;
}

void game_loop(int my_game_id, int socket, int my_player, int other_player) {
    char log_msg[255];
    int break_game_loop = 0;
    char tmp_p1_word[TURN_WORD_LENGTH];
    char tmp_p2_word[TURN_WORD_LENGTH];
    int msg_status = 0;
    int winner = 0;
    int turn_valid = 0;
    int game_loop_state = my_player == 0 ? WAIT_FOR_END_TURN : WAIT_FOR_MY_TURN;
    int tmp = 0;
    int end_game = 0;

    while (game_loop_state != BREAK_LOOP) {
        switch (game_loop_state) {
            case WAIT_FOR_END_TURN:
                break_game_loop = wait_for_end_turn(my_game_id, socket, WAITING_TIMEOUT, my_player, other_player, tmp_p1_word, tmp_p2_word);
                if(break_game_loop == STOP_GAME_LOOP) {
                    game_loop_state = BREAK_LOOP;
                } else {
                    game_loop_state = VALIDATE_TURN;
                }
                break;

            case VALIDATE_TURN:
                debug_player_message(log_msg, "Validating player %d turn.\n", my_player);
                server_validate_turn(my_game_id, my_player, tmp_p1_word, tmp_p2_word, &turn_valid);
                if (turn_valid != OK) {
                    debug_player_message(log_msg, "Turn of player %d is not valid, skipping it!\n", my_player);
                    msg_status = send_err_msg(socket, ERR_TURN);
                    if(msg_status == CLOSED_CONNECTION) {
                        debug_player_message(log_msg, "Player %d closed the connection.\n", my_player);
                        server_set_winner(my_game_id, other_player);
                        game_loop_state = STOP_GAME_LOOP;
                        continue;
                    }
                } else {
                    // turn valid -> update the player's stones
                    server_update_players_stones(my_game_id, tmp_p1_word, tmp_p2_word);
                    msg_status = send_ok_msg(socket);
                    if(msg_status == CLOSED_CONNECTION) {
                        debug_player_message(log_msg, "Player %d closed the connection.\n", my_player);
                        server_set_winner(my_game_id, other_player);
                        game_loop_state = STOP_GAME_LOOP;
                        continue;
                    }
                }
                game_loop_state = CHECK_WINNING_COND;
                break;

            case CHECK_WINNING_COND:
                server_check_winning_conditions(my_game_id, &winner);
                if(winner != OK) {
                    server_set_winner(my_game_id, winner == P1_WINS ? 0 : 1);
                    debug_player_message(log_msg, "Player %d wins the game!\n", winner);
                    game_loop_state = BREAK_LOOP;
                } else {
                    game_loop_state = SWITCH_TURN;
                }
                break;

            case WAIT_FOR_MY_TURN:
                server_get_current_turn(my_game_id, &tmp);
                end_game = 0;
                if(tmp != my_player) {
                    debug_player_message(log_msg, "Player %d is waiting for his turn.\n", my_player);
                    while (tmp != my_player) {
                        // handle incoming messages
                        handle_message_waiting_turn(my_game_id, socket, WAITING_TIMEOUT, my_player, other_player);

                        // check that the game haven't ended yet
                        server_is_game_end(my_game_id, &end_game);
                        if(end_game == OK) {
                            break;
                        }

                        // check my turn
                        server_get_current_turn(my_game_id, &tmp);
                    }
                    if(end_game == OK) {
                        game_loop_state = BREAK_LOOP;
                    } else {
                        game_loop_state = START_TURN;
                    }
                } else {
                    game_loop_state = START_TURN;
                }
                break;

            case START_TURN:
                debug_player_message(log_msg, "Player %d starts his turn.\n", my_player);
                break_game_loop = send_start_turn_to_player(my_game_id, socket, my_player, other_player);
                if(break_game_loop == STOP_GAME_LOOP) {
                    game_loop_state = BREAK_LOOP;
                } else {
                    game_loop_state = WAIT_FOR_END_TURN;
                }
                break;

            case SWITCH_TURN:
                server_switch_turn(my_game_id);
                game_loop_state = WAIT_FOR_MY_TURN;

                break;
        }
    }
}

/*
 * A thread for one player. At first, nickname will be checked, then 
 * the thread waits for another player to be registered and the game begins.
 */
void *player_thread(void *arg) {
	
	char log_msg[255];
    char p1name[10];
    char p2name[10];
	
	char buffer[MAX_TXT_LENGTH + 1];
    thread_arg* args = ((thread_arg *)arg);
	int msg_status = 0;
	int socket = args->socket;
	int thread_num = args->thread_number;
    uint32_t addr = args->addr;
    int port = args->port;
	int tmp = 0;
    int my_game_id = -1;

    /*
	 * Index in the array of players.
	 */
    int my_player;
    int other_player;

	sprintf(log_msg, "Starting new thread with id=%d.\n",thread_num);
	sinfo(PLAYER_THREAD_NAME, log_msg);

    // wait for nick
    // TODO: pass all nicks to validation
    tmp = wait_for_nick(my_game_id, socket, buffer);
    if(tmp != OK) {
        clean_up(args);
        return NULL;
    }

    /*
     * Nickname passed validation,
     * register a new player.
     *
     * Wait for the second thread.
     */
    sdebug(PLAYER_THREAD_NAME, "Nick validation ok.\n");

    // send message to player that the nick is ok
    msg_status = send_ok_msg(socket);
    if (msg_status != OK) {
        sprintf(log_msg, "Error occurred while sending OK message to socket %d.\n", socket);
        serror(PLAYER_THREAD_NAME, log_msg);
        send_err_msg(socket, msg_status);
        clean_up(args);
        return NULL;
    }

    // get game, also initializes my_player
    get_free_game_bind_player(&my_game_id, &my_player, buffer, addr, port, socket);
    if(my_game_id == NO_GAME_SLOT_FREE) {
        serror(PLAYER_THREAD_NAME,"Server full, sorry.\n");
        send_err_msg(socket, ERR_SERVER_FULL);
        clean_up(args);
        return NULL;
    }
    sprintf(log_msg, "Game %d selected for player %s. Player position: %d.\n", my_game_id, buffer, my_player);
    sinfo(PLAYER_THREAD_NAME, log_msg);
    other_player = my_player == PLAYER_1 ? PLAYER_2 : PLAYER_1;
    server_print_player(my_game_id, my_player, log_msg, 1);
    sinfo(PLAYER_THREAD_NAME, log_msg);


    if(my_player == PLAYER_2) {
        /* one thread is already in queue - wake it up and lets play */
        sinfo(PLAYER_THREAD_NAME, "THE GAME HAS STARTED!\n");
        server_init_new_game(my_game_id);
        server_start_game(my_game_id);
    } else {
        // no thread has finished validation yet, wait for another thread
        sinfo(PLAYER_THREAD_NAME, "Waiting for other player.\n");

        // while there are not two players in the game, wait and keep receiving messages.
        while (tmp < 2) {
            // handle incoming messages
            msg_status = handle_message_waiting(socket, WAITING_TIMEOUT, my_player);
            if(msg_status == STOP_GAME_LOOP) {
                server_leave_from_game(my_game_id, my_player);
                clean_up(args);
                return NULL;
            }

            // update the count of players in current game
            server_get_players_count(my_game_id, &tmp);
        }
    }

    // send start game message
    get_player_nick(my_game_id, PLAYER_1, p1name);
    get_player_nick(my_game_id, PLAYER_2, p2name);
    send_start_game_msg(socket, p1name, p2name);

    // whole game loop handled inside this function
    game_loop(my_game_id, socket, my_player, other_player);

    player_thread_end_game(my_game_id, socket, my_player);

	sinfo(PLAYER_THREAD_NAME,"End of thread.\n");

    clean_up(args);
	return NULL;
} 






/*
 * =====================================================================
 * MAIN FUNCTION AND HELPER FUNCTIONS
 * =====================================================================
 */

void print_help() {
    printf("+================+\n");
    printf("|  SENET SERVER  |\n");
    printf("+================+\n\n");
    printf("Possibe paramaters:\n");
    printf("===================\n");
    printf("help: Display this help and quit.\n");
    printf("port <port>: Specify the port server will be listening on. Must be 49152 <= port <= 65535. 65000 is used by default.\n");
    printf("ip   <ip>: String which specifies the ip address server will be listening on. If not valid, INADDR_ANY is used.\n");
}

/*
 * Converts argv to number and stores it to port.
 * If the conversion goes wrong or the port isn't in range, default value
 * is used.
 */
void load_port(char* argv, int* port) {
    char log_msg[100];
    *port = (int)strtol(argv, NULL , 10);
    if (*port < 49152 || *port > 65535) {
        sprintf(log_msg, "Provided port value %s is out of possible range, using default %d.\n", argv, SRV_PORT);
        sdebug(SERVER_NAME, log_msg);
        *port = SRV_PORT;
    }
}

/*
 * Converts argv to ip addres.
 * If the conversion fails, INADDR_ANY is used.
 */
void load_ip(char* argv, struct in_addr* addr) {
    char log_msg[100];
    if(inet_aton(argv, addr) == 0) {
        sprintf(log_msg, "Address %s is not valid. Using INADDR_ANY instead.\n", argv);
        serror(SERVER_NAME, log_msg);

        addr->s_addr = htonl(INADDR_ANY);
    }
}

/*
 * Loads data from arguments and return 1 if the program should exist afterwards.
 */
int load_arguments(int argc, char* argv[], int* port, struct in_addr* addr) {
    int cntr = 1;
    char log_msg[100];

    while (cntr < argc) {
        if(strcmp(argv[cntr], HELP_PARAM_NAME) == 0) {
            // print help
            print_help();
            return 1;
        } else if (strcmp(argv[cntr], PORT_PARM_NAME) == 0) {
            // load port
            cntr++;
            load_port(argv[cntr], port);
            cntr++;
        } else if (strcmp(argv[cntr], IP_PARAM_NAME) == 0) {
            // load ip
            cntr++;
            load_ip(argv[cntr], addr);
            cntr++;
        } else {
            // unknown parameter
            sprintf(log_msg, "Unknown parameter %s.\n", argv[cntr]);
            sdebug(SERVER_NAME, log_msg);
            print_help();
            return 1;
        }
    }

    return 0;
}

/*
 * Initializes:
 * connections array,
 * timer threads array,
 * games array
 */
void initialize() {
    int tmp = 0;

    /* init game */
//    reset_game(&game);

    /* init timer threads */
    for (tmp = 0; tmp < MAX_TIMER_THREADS; tmp++) {
        timer_threads[tmp] = NO_THREAD;
    }

    /* init connections */
    for (tmp = 0; tmp < MAX_CONNECTIONS; tmp++) {
        connections[tmp] = NO_THREAD;
    }

    /* init games */
    for (tmp = 0; tmp < MAX_GAMES; tmp++) {
        server_reset_game(tmp);
        games[tmp].id = tmp;
    }
}

/*
 * Main function.
 */ 
int main(int argc, char *argv[])
{
	int sock = 0;
    int incoming_sock = 0;
	int optval = 0;
    struct sockaddr_in addr, incoming_addr;
    struct in_addr ip_addr;
    unsigned int incoming_addr_len = 0;
    pthread_t cleaner_thread = NO_THREAD;
    int thread_err = 0;
    char log_msg[255];
    int tmp_curr_conn = 0;
    int port = SRV_PORT;
    thread_arg* player_thread_arg;

    // initialize arrays
    initialize();

    /* load arguments */
    ip_addr.s_addr = htonl(INADDR_ANY);
    if(load_arguments(argc, argv, &port, &ip_addr) == 1) {
        return 0;
    }


	printf("\n\n");
	printf("==========================\n");
	printf("   I am a Senet server.\n");
	printf("  Connect on port %d\n",port);
	printf("==========================\n\n\n");

	/* create socket */
	sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (sock < 0) {
		serror("server","Error while creating a new socket.");
		return 1;
	}

	/* set reusable flag */
	optval = 1;
	setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &optval, sizeof(optval));

	/* prepare inet address */
	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_port = htons((uint16_t )port);
	addr.sin_addr = ip_addr; /* listen on all interfaces */
	if (bind(sock, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        sprintf(log_msg, "Error while binding a new listener: %s.\n", strerror(errno));
		serror(SERVER_NAME, log_msg);
		return 1;
	}

	if (listen(sock, 10) < 0) {
        sprintf(log_msg, "Error while listening: %s.\n", strerror(errno));
        serror(SERVER_NAME, log_msg);
		return 1;
	}
	
	if (!init_ms()) {
		return 1;
	}
	
	/*
	 * Start the cleaning thread
	 */ 
	if(pthread_create(&cleaner_thread, NULL, cleaner, NULL)) {
		serror(SERVER_NAME, "Error while initializing the cleaner thread.\n");
		return 1;
	}

	/* listening loop - wait for players*/
	while (1) 
	{	
		//new connection
		incoming_addr_len = sizeof(incoming_addr);
    	incoming_sock = accept(sock, (struct sockaddr*)&incoming_addr, 
                      &incoming_addr_len);
		if (incoming_sock < 0)
		{
			close(sock);
			serror("server","Error while accepting a new connection.\n");
		}
		sprintf(log_msg, "Connection from %s:%i\n",
	        	inet_ntoa(incoming_addr.sin_addr),
	        	ntohs(incoming_addr.sin_port)
		);
		sinfo("server",log_msg);
		get_curr_conn(&tmp_curr_conn);
		if(tmp_curr_conn >= MAX_CONNECTIONS) {
			sinfo(SERVER_NAME,"Server can't accept any new connections.\n");
            send_err_msg(incoming_sock, ERR_SERVER_FULL);
			continue;
		}

        sprintf(log_msg, "Creating a new player thread on index %d.\n", tmp_curr_conn);
        sinfo(SERVER_NAME, log_msg);
        player_thread_arg = malloc(sizeof(thread_arg));
        player_thread_arg->thread_number = tmp_curr_conn;
        player_thread_arg->socket = incoming_sock;
        player_thread_arg->addr = incoming_addr.sin_addr.s_addr;
        player_thread_arg->port = incoming_addr.sin_port;
		thread_err = pthread_create(&connections[tmp_curr_conn], NULL, player_thread,(void *)player_thread_arg);
		if(thread_err) {
            free(player_thread_arg);
			sprintf(log_msg, "Error: %s\n",strerror(thread_err));
			serror(SERVER_NAME,log_msg);
            connections[tmp_curr_conn] = NO_THREAD;
            break;
		}
	}
	
	/* signal cleaner*/
	
	/* wait for cleaner */
	shutdown_cleaner();
	pthread_join(cleaner_thread, NULL);
	return 0;
}
