#!/bin/bash

paths=("readme.md" "gitAddAll.sh" "doc/*" "code/server/*.c" "code/server/*.h" "code/server/core/*.h" "code/server/core/*.c" "code/server/common/*.h" "code/server/common/*.c" "code/server/SConstruct" "code/client/src/*" "code/client/pom.xml")

for item in ${paths[*]}
do
	printf "git add --all %s\n" $item
	git add --all $item
done


