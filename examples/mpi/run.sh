#!/bin/sh
$XLEARNING_HOME/bin/xl-submit \
   --app-type "mpi" \
   --app-name "mpi_demo" \
   --files demo \
   --launch-cmd "./demo" \
   --worker-memory 5G \
   --worker-cores 2 \
   --worker-num 3 \
   --queue default \
