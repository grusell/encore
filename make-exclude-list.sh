#!/usr/bin/env bash
for f in $(jar tf app/build/libs/app-0.1.6-SNAPSHOT.jar | egrep 'BOOT-INF/lib/.*[.]jar')
do
    echo $f | sed -re 's&BOOT-INF/lib/(.*)-([0-9.]*([.](Final|RELEASE))?)[.]jar&\1-*.jar&g'
done

