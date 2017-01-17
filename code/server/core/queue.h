#ifndef SERVER_QUEUE_H
#define SERVER_QUEUE_H

#include "player.h"

#define MAX_QUEUE_SIZE      9

/*
 * Node in a player queue.
 */
typedef struct Node {
    struct Player player;
    struct Node* next;
};

/*
 * Player queue.
 */
typedef struct Queue{
    struct Node* first;
    struct Node* last;
};

/*
 * Returns the number of elements in queue.
 */
int get_queue_size(struct Queue *queue);

/*
 * Inserts the player to the end of the queue.
 * If the queue is full, returns -1. Otherwise 0.
 */
int insert_to_queue(struct Queue* queue, struct Player player);

/*
 * Removes the queue->first from queue and returns it.
 * If the queue is empty, returns null.
 */
void remove_from_queue(struct Queue *queue, struct Player *player);

/*
 * Frees all the nodes in queue and sets queue->first and last to NULL.
 */
void clear_queue(struct Queue* queue);

#endif //SERVER_QUEUE_H
