cp train.conf train_real.conf
chmod 777 train_real.conf
echo "num_machines = $LIGHTGBM_NUM_MACHINE" >> train_real.conf
echo "local_listen_port = $LIGHTGBM_LOCAL_LISTEN_PORT" >> train_real.conf
./LightGBM/lightgbm config=train_real.conf
