#ifndef __NICK_VAL_H
#define __NICK_VAL_H

#include <stdio.h>
#include <string.h>

#include "common.h"
#include "seneterror.h"
#include "../core/player.h"

#define MIN_NICK_LENGTH			3
#define MAX_NICK_LENGTH			8

/* check nick results */
#define NICK_OK					0

/*
 * Checks, if the length is > 0 and <= MAX_NICK_LENGTH.
 * If the length < MIN_NICK_LENGTH, 1 will be returned.
 * If the length is > MAX_LENGTH, 2 will be returned.
 * If the length is ok, 0 will be returned.
 * 
 * It is expected that the nickname is '\0' terminated.
 */
int check_length(char *nickname);

/*
 * Checks, if the nickanme consist only of allowe characters
 * Those are: A-Za-z and 0-9 on position > 0.
 * 
 * It is expected that the length has been already checked.
 * 
 * If the nickname is ok, 0 will be returned.
 * If the first character is invalid, 1 will be returned.
 * If any onther character is invalid, 2 will be returned.
 */
int check_characters(char* nickname);

/*
 * Checks if the nickname already exist.
 * If the nickname doesn't exist yet, 
 * 0 will be returned.
 * 
 * It is expected that length and character has already been checked.
 * It is expected that the length of players is MAX_PLAYERS.
 */
int check_nick_duplicity(char *nickname, struct Player *players);

/*
 * Checks the nickname for length, allowed chars and duplicity.
 * It is possible to specify buffer for error message.
 * It is expected that the nick is '\0' terminated.
 * Return values:
 * 		NICK_OK - nick ok
 * 		ERR_NICK_TOO_SHORT - nick length is < MIN_NICK_LENGTH
 * 		ERR_NICK_TOO_LONG - nick length is > MAX_NICK_LENGTH
 * 		ERR_FIRT_CHAR_INV - nick starts with invalid char
 * 		ERR_CONTAINS_INV_CHAR - nick contains invalid chars
 * 		ERR_NICK_EXISTS - nick already exists
 */ 
int check_nickname(char* nickname, char *errmsg, struct Player* players);

#endif
