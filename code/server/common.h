#ifndef __COMMON_H
#define __COMMON_H

#define SRV_PORT		65000
#define MAX_TXT_LENGTH		1000
#define MAX_PLAYERS		2    

void err(char *msg);
int send_txt(int sock, char *txt);
int recv_txt(int sock);
void send_greetings(int sock);

#endif
