

#include "game.h"
#include "../common/slog.h"

/*
 * Resets all flags.
 */
void init_new_game(Game_struct* game, int first_player) {
    game->winner = -1;
    unset_game_flag(&(game->flags), GAME_STARTED_FLAG);
    unset_game_flag(&(game->flags), GAME_ENDED_FLAG);
    unset_game_flag(&(game->flags), WAITING_P1_FLAG);
    unset_game_flag(&(game->flags), WAITING_P2_FLAG);
    unset_game_flag(&(game->flags), WINNER_FLAG);
    if(first_player == 0) {
        unset_game_flag(&(game->flags), TURN_FLAG);
    } else {
        set_game_flag(&(game->flags), TURN_FLAG);
    }
}

/*
 * Sets a flag to 1.
 */
void set_game_flag(int* flags, int flag_index) {
    *flags = *flags | (1 << flag_index);
}

/*
 * Sets a flag to 0.
 */
void unset_game_flag(int* flags, int flag_index) {
    *flags = *flags & (~(1 << flag_index));
}

/*
 * Returns the value of the flag.
 */
int get_game_flag(int* flags, int flag_index) {
    // mask flags to get the value and then shift it so it's either 1 or 0
    return (*flags & (1 << flag_index)) >> flag_index;
}

/*
 * Sets the game_started flag to 1.
 */
void start_game(Game_struct* game) {
    set_game_flag(&(game->flags), GAME_STARTED_FLAG);
    unset_game_flag(&(game->flags), GAME_ENDED_FLAG);
}

/*
 * Sets the game_started flag to 0, game_ended to 1 and clears waiting flags.
 */
void end_game(Game_struct* game) {
    unset_game_flag(&(game->flags), GAME_STARTED_FLAG);
    unset_game_flag(&(game->flags), WAITING_P1_FLAG);
    unset_game_flag(&(game->flags), WAITING_P2_FLAG);
    set_game_flag(&(game->flags), GAME_ENDED_FLAG);
}

/*
 * If at least one of the waiting flags is set to 1,
 * returns 1, otherwise returns 0.
 */
int is_game_waiting(Game_struct* game) {
    if (get_game_flag(&(game->flags), WAITING_P1_FLAG) == 1 ) {
        return 1;
    }

    if (get_game_flag(&(game->flags), WAITING_P2_FLAG) == 1 ) {
        return 1;
    }

    return 0;
}

/*
 * Compares the my_player value with turn value and either
 * returns 1 if it's my turn or 0 if not.
 */
int is_my_turn(Game_struct* game, int my_player) {
    int turn = get_game_flag(&(game->flags), TURN_FLAG);

    return (turn == my_player) ? 1 : 0;
}

/*
 * Switches turn = sets the turn flag to opposite value.
 */
void switch_turn(Game_struct* game, int current_turn) {
    if(current_turn == 0) {
        set_game_flag(&(game->flags), TURN_FLAG);
    } else {
        unset_game_flag(&(game->flags), TURN_FLAG);
    }
}

/*
 * If the game ended flag is set, returns 1, otherwise 0.
 */
int is_end_of_game(Game_struct* game) {
    return get_game_flag(&(game->flags), GAME_ENDED_FLAG);
}

/*
 * If the player is 0 or 1, sets the winner variable and
 * the winner flag.
 */
void set_winner(Game_struct* game, int player) {
    if(player == 0 || player == 1) {
        set_game_flag(&(game->flags), WINNER_FLAG);
        game->winner = player;
    } else {
        unset_game_flag(&(game->flags), WINNER_FLAG);
    }
}

/*
 * If the winner flag is set, returns the value of winner variable.
 * Otherwise returns -1.
 */
int get_winner(Game_struct* game) {
    int is_winner = get_game_flag(&(game->flags), WINNER_FLAG);
    if(is_winner == 1) {
        return game->winner;
    } else {
        return -1;
    }
}

/*
 * Checks the new turn words against the old turn words, and returns 1 if the turn was valid.
 *
 * turn indicates which player has moved his pawns. 0 = 1st player, 1 = 2nd player.
 */
int validate_turn(char* p1_old_tw, char* p2_old_tw, char* p1_new_tw, char* p2_new_tw, int turn) {
    int i = 0;
    int j = 0;
    int p1_new[TURN_WORD_LENGTH / 2];
    int p2_new[TURN_WORD_LENGTH / 2];
    int tmp = 0;
    int tmp2 = 0;

    // convert to int
    while(i < TURN_WORD_LENGTH) {
        tmp = 0;
        tmp2 = 0;
        tmp = 10*(p1_new_tw[i] - '0');
        tmp2 = 10*(p2_new_tw[i] - '0');
        i++;

        tmp += p1_new_tw[i] - '0';
        tmp2 += p2_new_tw[i] - '0';
        i++;
        p1_new[j] = tmp;
        p2_new[j] = tmp2;

        j++;
    }

    sinfo(COMMON_NAME, "Conversion ok.\n");

    // check the field numbers

    for (i = 0; i < TURN_WORD_LENGTH/2; i++) {
        if(!(p1_new[i] >=1 && p1_new[i] <= 31) ) {
            return ERR_TURN;
        }

        if(!(p2_new[i] >=1 && p2_new[i] <= 31) ) {
            return ERR_TURN;
        }
    }
    sinfo(COMMON_NAME, "Field numbers ok.\n");

    for(i = 0; i < TURN_WORD_LENGTH/2; i++)  {
        for(j = 0; j < TURN_WORD_LENGTH/2; j++) {
            if(p1_new[i] == p2_new[j] && p1_new[i] != 31) {
                return ERR_TURN;
            }

            if(i != j && (
                    (p1_new[i] == p1_new[j] && p1_new[i] != 31) ||
                    (p2_new[i] == p2_new[j] && p2_new[i] != 31)
                 )
              ) {
                return ERR_TURN;
            }
        }
    }
    sinfo(COMMON_NAME, "Same numbers ok.\n");

    return OK;
}

/*
 * Checks the winning conditions and returns:
 *  OK: no winner
 *  P1_WINS
 *  P2_WINS
 */
int check_winning_conditions(char* p1_turn_word, char* p2_turn_word) {
    int win = P1_WINS;
    int i = 0;
    int j = 0;
    int p1_new[TURN_WORD_LENGTH / 2];
    int p2_new[TURN_WORD_LENGTH / 2];
    int tmp = 0;
    int tmp2 = 0;

    // convert to int
    while(i < TURN_WORD_LENGTH) {
        tmp = 0;
        tmp2 = 0;
        tmp = 10*(p1_turn_word[i] - '0');
        tmp2 = 10*(p2_turn_word[i] - '0');
        i++;

        tmp += p1_turn_word[i] - '0';
        tmp2 += p2_turn_word[i] - '0';
        i++;
        p1_new[j] = tmp;
        p2_new[j] = tmp2;

        j++;
    }

    // check the first player
    for (i = 0; i < TURN_WORD_LENGTH/2; ++i) {
        if(p1_new[i] != 31) {
            win = P2_WINS;
            break;
        }
    }

    // check the second player
    if (win == P2_WINS) {
        for (j = 0; j < TURN_WORD_LENGTH/2; ++j) {
            if(p2_new[j] != 31) {
                win = OK;
                break;
            }
        }
    }

    return win;
}

/*
 * Prints game flags to buffer.
 */
void print_flags(char *buffer, Game_struct *game) {
    int gef = get_game_flag(&(game->flags), GAME_ENDED_FLAG);
    int gsf = get_game_flag(&(game->flags), GAME_STARTED_FLAG);
    int w1f = get_game_flag(&(game->flags), WAITING_P1_FLAG);
    int w2f = get_game_flag(&(game->flags), WAITING_P2_FLAG);
    int tf = get_game_flag(&(game->flags), TURN_FLAG);

    sprintf(buffer, "Flags: game_ended: %d, game_started: %d, waiting_for_p1: %d, waiting_for_p2: %d, turn: %d\n",
                    gef,gsf,w1f,w2f,tf);
}

