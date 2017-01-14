#ifndef SERVER_GAME_H
#define SERVER_GAME_H

#include "player.h"

#define MAX_PLAYERS				2

/*
 * ======================
 * CONTROL VARIABLES
 * ======================
 */

/*
 * Two players.
 */
Player players[MAX_PLAYERS];

/*
 * Flag indicating that the game has started.
 */
int game_started = 0;

/*
 * A flag indicating the end of game. If the flag is raised, it's a signal for the
 * thread that it should quit.
 */
int end_of_game;

/*
 * Variable indicating whose turn is now.
 * Either 0 or 1.
 */
int turn;

/*
 * A flag indicating that we're waiting for player 1 to reconnect.
 */
int waiting_for_p1;

/*
 * A flag indicating that we're waiting for player 2 to reconnect.
 */
int waiting_for_p2;

/*
 * ========================
 * FUNCTIONS
 * ========================
 */


/*
 * Compares the my_player value with turn value and either
 * returns 1 if it's my turn or 0 if not.
 */
int is_my_turn(int my_player);

/*
 * Checks the new turn words against the old turn words, and returns 1 if the turn was valid.
 *
 * turn indicates which player has moved his pawns. 0 = 1st player, 1 = 2nd player.
 */
int validate_turn(char* p1_old_tw, char* p2_old_tw, char* p1_new_tw, char* p2_new_tw, int turn);

/*
 * Checks the winning conditions and returns:
 * -1: no winner
 *  0: player 1 wins
 *  1: player 2 wins
 */
int check_winning_conditions(char* p1_turn_word, char* p2_turn_word);

#endif //SERVER_GAME_H
