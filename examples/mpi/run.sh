#!/bin/sh
$HBOX_HOME/bin/hbox-submit \
   --app-type "mpi" \
   --app-name "mpi_demo" \
   --files demo \
   --worker-memory 5G \
   --worker-cores 2 \
   --worker-num 3 \
   --queue default \
   ./demo
