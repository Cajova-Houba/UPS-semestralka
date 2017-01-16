#ifndef __SENET_LOGGER_H
#define __SENET_LOGGER_H

#include <stdio.h>
#include <time.h>

/*
 * Possible names to use for log messages.
 */ 
#define SERVER_NAME				"server"
#define PLAYER_THREAD_NAME	 	"server - player thread"
#define COMMON_NAME				"common"
#define MESSAGE_NAME			"message"
#define TIMER_THREAD_NAME       "timer"

void sinfo(char *name, char *msg);
void sdebug(char *name, char *msg);
void serror(char *name, char *msg);

#endif
