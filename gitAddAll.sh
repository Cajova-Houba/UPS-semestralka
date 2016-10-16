#!/bin/bash

paths=("readme.md" "gitAddAll.sh" "doc/*" "code/server/*.c" "code/server/*.h" "code/server/SConstruct")

for item in ${paths[*]}
do
	printf "git add --all %s\n" $item
	git add --all $item
done


