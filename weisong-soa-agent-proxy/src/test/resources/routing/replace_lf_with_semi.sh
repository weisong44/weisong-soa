#!/bin/sh

if [ $# -ne 1 ]; then
  echo
  echo "Usage:"
  echo "    replace_lf_with_semi.sh <file>"
  echo
  exit
fi

echo routing.config=`cat $1 | tr '\n' ';'`
echo
