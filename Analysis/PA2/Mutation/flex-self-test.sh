#!/bin/bash

## Copyright (C) 2005 Alexandru Salcianu <salcianu@alum.mit.edu>
## Licensed under the terms of the GNU GPL; see COPYING for details.
## $Id: flex-self-test.sh,v 1.1 2005-09-30 20:05:11 salcianu Exp $

echo Flex analyzing its own purity\; the analysis will take many hours\!

PKHOME=`dirname $0`
ANALYSIS_CODE_PATH=$PKHOME/purity.jar:$PKHOME/aux/jpaul.jar:$PKHOME/aux/jutil.jar

if [ ! -f $PKHOME/purity.jar ]; then
    PKHOME=$PKHOME/../../..
    if [ ! -f $PKHOME/Main/Purity.java ]; then
	echo Confused about the current directory\;
	echo This script should be called only as part of the purity kit
	echo or in the Harpoon/Code development source tree
    fi
    ANALYSIS_CODE_PATH=$PKHOME:$PKHOME/Support/jpaul.jar:$PKHOME/Support/jutil.jar:$PKHOME/Support/reflect-thunk.jar:$PKHOME/Support/cpvm.jar:$PKHOME/Support/glibj-0.08-extra.jar:$PKHOME/Support/glibj-0.08.zip
fi


rm -f $PKHOME/out.txt

java -ea -Xss16m -Xmx768m \
-cp ${ANALYSIS_CODE_PATH} \
harpoon.Main.Purity \
harpoon.Main.Purity \
${ANALYSIS_CODE_PATH} \
2>&1 | tee $PKHOME/out.txt
