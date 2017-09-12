#!/bin/bash

file=$1
for i in $(cat $file | tr "," "\n"); do
	echo $i
done

