#!/bin/bash

paths=("readme.md" "gitAddAll.sh" "doc/*" "code/server/*.c" "code/server/*.h" "code/server/core/*.h" "code/server/core/*.c" "code/server/common/*.h" "code/server/common/*.c" "code/server/SConstruct" "code/server/CMakeLists.txt" "code/client/src/*" "code/client/pom.xml" "code/client/*.sh")

for item in ${paths[*]}
do
	printf "git add --all %s\n" $item
	git add --all $item
done


