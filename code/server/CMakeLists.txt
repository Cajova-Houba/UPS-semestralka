cmake_minimum_required(VERSION 3.6)
project(server)

set(CMAKE_CXX_STANDARD 11)

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

set(SOURCE_FILES
        common/common.c
        common/common.h
        common/message.c
        common/message.h
        common/nick_val.c
        common/nick_val.h
        common/seneterror.h
        common/slog.c
        common/slog.h
        core/player.h
        core/server.c core/player.c core/game.h core/game.c core/parallel.h core/parallel.c core/queue.h core/queue.c)

add_executable(server ${SOURCE_FILES})
target_link_libraries(server Threads::Threads)
