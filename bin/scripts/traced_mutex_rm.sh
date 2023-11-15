if [ ! -f 'run.sh' ]; then
  echo 'run.sh does not exist. Please run inside the bin/ folder'
  exit
fi

set -e

# num_run=2
num_run=100
medley_run=10 #every medley_run runs, we want to rerun medley for a new trace
num_server=256
config_name="config.json"
topologies=("grid" "random" "cluster")
# drop_rates=(0.05 0.1 0.15 0.2 0.35)
num_servers=(32 49 64 128 256 512)
# num_servers=(32 49 64 128 256 512 1024)
s_distros=("zipfian" "uniform")
t_distros=("exponential" "weibull")


# # drop rate sliding
# # random 256
# # ia:d ratio 1
# for spatial in ${s_distros[@]}; do
#     # toggle btwn uniform and zipfian spatial distro
#     tmp="$(mktemp)"
#     jq --arg spatial "$spatial" '.spatial_distro = $spatial' $config_name > "$tmp"
#     mv "$tmp" $config_name
#     for distro in ${t_distros[@]}; do 
#         trace_path="../../YCSB/analyze/traces/${distro}.txt"
#         tmp="$(mktemp)"
#         jq --arg trace_path "$trace_path" '.trace_file = $trace_path' $config_name > "$tmp"
#         mv "$tmp" $config_name
#         echo "" > "../analyze/outputs/${distro}_${spatial}.txt"

#         for drop_rate in ${drop_rates[@]}; do
#             # modify the drop rate
#             tmp="$(mktemp)"
#             jq --arg drop_rate "$drop_rate" '.msg_drop_rate = $drop_rate' $config_name > "$tmp"
#             mv "$tmp" $config_name
#             for (( i=1; i<=$num_run; i++ )); do
#                 if (( ($i-1) % $medley_run == 0 )); then
#                     cd ../../Inconsistency-on-Medley/bin/
#                     tmp="$(mktemp)"
#                     jq --arg drop_rate "$drop_rate" '.msg_drop_rate = $drop_rate' $config_name > "$tmp"
#                     mv "$tmp" $config_name
#                     nice -n 3 bash -c "./run.sh ${config_name}" # run medley for about 30 secs (end time 60k)
#                     cd ../../LeaderElection/bin/
#                     # echo "drop_rate: ${drop_rate}, run_num: ${i}"
#                 fi
#                 ./run.sh $config_name
#                 nice -n 3 bash -c "./run.sh ${config_name}"
#                 cat stats.json >> "../analyze/outputs/${distro}_${spatial}.txt"
#             done
#         done
#     done
# done


# N sliding
# drop rate 0.05
# ia:d ratio 1
# for spatial in ${s_distros[@]}; do
#     # toggle btwn uniform and zipfian spatial distro
#     tmp="$(mktemp)"
#     jq --arg spatial "$spatial" '.spatial_distro = $spatial' $config_name > "$tmp"
#     mv "$tmp" $config_name
#     for time in ${t_distros[@]}; do
#         jq --arg time "$time" '.time_distro = $time' $config_name > "$tmp"
#         mv "$tmp" $config_name
#         echo "" > "../analyze/outputs/${time}_${spatial}.txt"
#         for num_serv in ${num_servers[@]}; do
#           # modify N
#           tmp="$(mktemp)"
#           jq --arg num_serv "$num_serv" '.num_servers = $num_serv' $config_name > "$tmp"
#           top_file="topo/random${num_serv}.txt"
#           jq --arg top_file "$top_file" '.topology_file = $top_file' "$tmp" > $config_name

#           for (( i=1; i<=$num_run; i++ )); do
#               if (( ($i-1) % $medley_run == 0 )); then
#               cd ../../Inconsistency-on-Medley/bin/

#               tmp="$(mktemp)"
#               jq --arg num_serv "$num_serv" '.num_server = $num_serv' $config_name > "$tmp"
#               top_file="topo/random${num_serv}.txt"
#               jq --arg top_file "$top_file" '.coordinate_path = $top_file' "$tmp" > $config_name
#               rp="random${num_serv}_route.txt"
#               jq --arg rp "$rp" '.routing_path = $rp' $config_name > "$tmp"
#               mv "$tmp" $config_name

#               # ./run.sh $config_name # run medley for about 30 secs (end time 60k)
#               nice -n 3 bash -c "./run.sh ${config_name}" # run medley for about 30 secs (end time 60k)
#               cd ../../LeaderElection/bin/
#               # echo "num_serv: ${num_serv}, run_num: ${i}"
#               fi
#               # ./run.sh $config_name
#               nice -n 3 bash -c "./run.sh ${config_name}"
#               cat stats.json >> "../analyze/outputs/${time}_${spatial}.txt"
#           done
#         done
#     done
# done

# interarrival ratio sliding
# num servers 256
# random256 topo
#drop 0.05
# ratios=(0.05 0.25 0.5 1.0 3.0)
# for spatial in ${s_distros[@]}; do
#     # toggle btwn uniform and zipfian spatial distro
#     tmp="$(mktemp)"
#     jq --arg spatial "$spatial" '.spatial_distro = $spatial' $config_name > "$tmp"
#     mv "$tmp" $config_name
#     for time in ${t_distros[@]}; do
#         jq --arg time "$time" '.time_distro = $time' $config_name > "$tmp"
#         mv "$tmp" $config_name
#         echo "" > "../analyze/outputs/${time}_${spatial}.txt"
#         for ratio in ${ratios[@]}; do
#             # modify the interarrival ratio
#             tmp="$(mktemp)"
#             jq --arg ratio "$ratio" '.ir_ratio = $ratio' $config_name > "$tmp"
#             mv "$tmp" $config_name
#             for (( i=1; i<=$num_run; i++ )); do
#                 nice -n 3 bash -c "./run.sh ${config_name}"
#                 cat stats.json >> "../analyze/outputs/${time}_${spatial}.txt"
#             done
#         done
#     done
# done


# Compare camera to RA with no inconsistency (no drops)
# num servers 256
# random256 topo
# drop 0.0
for spatial in ${s_distros[@]}; do
    # toggle btwn uniform and zipfian spatial distro
    tmp="$(mktemp)"
    jq --arg spatial "$spatial" '.spatial_distro = $spatial' $config_name > "$tmp"
    mv "$tmp" $config_name
    for time in ${t_distros[@]}; do
        jq --arg time "$time" '.time_distro = $time' $config_name > "$tmp"
        mv "$tmp" $config_name
        echo "" > "../analyze/outputs/${time}_${spatial}.txt"
          for (( i=1; i<=$num_run; i++ )); do
              nice -n 3 bash -c "./run.sh ${config_name}"
              cat stats.json >> "../analyze/outputs/${time}_${spatial}.txt"
          done
    done
done
