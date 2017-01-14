#ifndef __SENET_PLAYER_H
#define __SENET_PLAYER_H

#define     TURN_WORD_LENGTH        5

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <arpa/inet.h>

/*
 * Structure representing a player.
 */
typedef struct {
    int id;
    char nick[10];
    int socket;
    uint32_t addr;
    int port;
    char turn_word[TURN_WORD_LENGTH+1];
} Player;

/*
 * Initializes a new player and stores it in the player variable.
 * Generates a new set of stones for player.
 * If the second_player = 0, stones will be generated from position
 * 2.
 */
void initialize_player(Player* player, int id, char* nick, int socket, int second_player, uint32_t addr, int port);

/*
 * Generates a set of stones for new player.
 *
 * If the second_player = 0, stones will be generated from position
 * 2.
 *
 */
void generate_new_stones(Player* player, int second_player);

/*
 * Prints the player to the buffer.
 */
void print_player(Player *p, char *buffer, int new_line);

/*
 * Updates the player's stones.
 */
void update_players_stones(Player *p, char *new_stones);

#endif
