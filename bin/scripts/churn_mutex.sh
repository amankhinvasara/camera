if [ ! -f 'run.sh' ]; then
  echo 'run.sh does not exist. Please run inside the bin/ folder'
  exit
fi


num_run=100
# num_run=50
num_server=256
config_name="config.json"
churn_ratios=(0.01 0.05 0.1 0.5)
s_distros=("uniform")
# s_distros=("zipfian" "uniform")
t_distros=("exponential")
# t_distros=("exponential" "weibull")


# clear existing flags
rm ../../Inconsistency-on-Medley/bin/membership/flag
rm ../../Inconsistency-on-Medley/bin/membership/done
set -eu


# for spatial in ${s_distros[@]}; do
#     # toggle btwn uniform and zipfian spatial distro
#     tmp="$(mktemp)"
#     jq --arg spatial "$spatial" '.spatial_distro = $spatial' $config_name > "$tmp"
#     mv "$tmp" $config_name
#     for time in ${t_distros[@]}; do 
#         jq --arg time "$time" '.time_distro = $time' $config_name > "$tmp"
#         mv "$tmp" $config_name
#         echo "" > "../analyze/outputs/${time}_${spatial}.txt"
#         for (( i=1; i<=$num_run; i++ )); do
#             for churn_rate in ${churn_ratios[@]}; do
#                 # modify the churn ratio in our config
#                 trap 'kill 0' SIGINT
                
#                 tmp="$(mktemp)"
#                 jq --arg churn_rate "$churn_rate" '.churn_ratio = $churn_rate' $config_name > "$tmp"
#                 mv "$tmp" $config_name

#                 cd ../../Inconsistency-on-Medley/bin/
#                 tmp="$(mktemp)"
#                 jq --arg churn_rate "$churn_rate" '.churn_ratio = $churn_rate' $config_name > "$tmp"
#                 mv "$tmp" $config_name

#                 # ./run.sh $config_name & # run churn traces through medley
#                 nice -n 3 bash -c "./run.sh ${config_name}" & # run churn traces through medley
#                 cd ../../LeaderElection/bin/
#                 nice -n 9 bash -c "./run.sh ${config_name}" & # run camera
#                 # ./run.sh $config_name & # run camera 
#                 wait
#                 cat stats.json >> "../analyze/outputs/${time}_${spatial}.txt"                
#             done
#         done
#     done
# done


time="weibull"
spatial="zipfian"
# echo "" > "../analyze/outputs/${time}_${spatial}.txt"
for (( i=1; i<=$num_run; i++ )); do
    # for churn_rate in ${churn_ratios[@]}; do
        # modify the churn ratio in our config
        trap 'kill 0' SIGINT
        
        # tmp="$(mktemp)"
        # jq --arg churn_rate "$churn_rate" '.churn_ratio = $churn_rate' $config_name > "$tmp"
        # mv "$tmp" $config_name

        cd ../../Inconsistency-on-Medley/bin/
        # tmp="$(mktemp)"
        # jq --arg churn_rate "$churn_rate" '.churn_ratio = $churn_rate' $config_name > "$tmp"
        # mv "$tmp" $config_name

        # ./run.sh $config_name & # run churn traces through medley
        echo $i                
        nice -n 3 bash -c "./run.sh ${config_name}" & # run churn traces through medley
        cd ../../LeaderElection/bin/
        nice -n 9 bash -c "./run.sh ${config_name}" & # run camera
        # ./run.sh $config_name & # run camera 
        wait
        cat stats.json >> "../analyze/outputs/${time}_${spatial}.txt"
    # done
done
