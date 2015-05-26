#!/bin/bash

for node in `cat slaves`
do
  echo $node:
  ssh $node shutdown -h -P now
done

