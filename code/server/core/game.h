#ifndef SERVER_GAME_H
#define SERVER_GAME_H

#include "player.h"
#include "../common/seneterror.h"

#define MAX_PLAYERS				2

/* Flag indexes */
/*
 * Flag indicating that the game has started.
 */
#define GAME_STARTED_FLAG       0

/*
 * A flag indicating the end of game. If the flag is raised, it's a signal for the
 * thread that it should quit.
 */
#define GAME_ENDED_FLAG         1

/*
 * A flag indicating that we're waiting for player 1 to reconnect.
 */
#define WAITING_P1_FLAG         2

/*
 * A flag indicating that we're waiting for player 2 to reconnect.
 */
#define WAITING_P2_FLAG         3

/*
 * Variable indicating whose turn is now.
 * Either 0 or 1.
 */
#define TURN_FLAG               4

/*
 * Flag indicating that someone has won the game.
 * Actual number of winner is stored in winner variable.
 */
#define WINNER_FLAG             5

/*
 * ======================
 * CONTROL VARIABLES
 * ======================
 */

/*
 * Game structure. Contains both players and game flags.
 */
typedef struct {
    Player players[MAX_PLAYERS];
    int flags;
    int winner;
} Game_struct;


/*
 * ========================
 * FUNCTIONS
 * ========================
 */


/*
 * Resets all flags.
 * And sets turn flag accordingly to first_player.
 */
void init_new_game(Game_struct* game, int first_player);

/*
 * Sets a flag to 1.
 */
void set_game_flag(int* flags, int flag_index);

/*
 * Sets a flag to 0.
 */
void unset_game_flag(int* flags, int flag_index);

/*
 * Returns the value of the flag.
 */
int get_game_flag(int* flags, int flag_index);

/*
 * Sets the game_started flag to 1, game_ended to 0.
 */
void start_game(Game_struct* game);

/*
 * Sets the game_started flag to 0, game_ended to 1 and clears waiting flags.
 */
void end_game(Game_struct* game);

/*
 * Compares the my_player value with turn value and either
 * returns 1 if it's my turn or 0 if not.
 */
int is_my_turn(Game_struct*game, int my_player);

/*
 * If at least one of the waiting flags is set to 1,
 * returns 1, otherwise returns 0.
 */
int is_game_waiting(Game_struct* game);

/*
 * Switches turn = sets the turn flag to opposite value.
 */
void switch_turn(Game_struct* game, int current_turn);

/*
 * If the game ended flag is set, returns 1, otherwise 0.
 */
int is_end_of_game(Game_struct* game);

/*
 * If the player is 0 or 1, sets the winner variable and
 * the winner flag.
 */
void set_winner(Game_struct* game, int player);

/*
 * If the winner flag is set, returns the value of winner variable.
 * Otherwise returns -1.
 */
int get_winner(Game_struct* game);

/*
 * Checks the new turn words against the old turn words, and returns 1 if the turn was valid.
 *
 * turn indicates which player has moved his pawns. 0 = 1st player, 1 = 2nd player.
 */
int validate_turn(char* p1_old_tw, char* p2_old_tw, char* p1_new_tw, char* p2_new_tw, int turn);

/*
 * Checks the winning conditions and returns:
 *  OK: no winner
 *  P1_WINS
 *  P2_WINS
 */
int check_winning_conditions(char* p1_turn_word, char* p2_turn_word);

/*
 * Prints game flags to buffer.
 */
void print_flags(char *buffer, Game_struct *game);

#endif //SERVER_GAME_H
