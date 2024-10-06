
if [ ! -f 'run.sh' ]; then
  echo 'run.sh does not exist. Please run inside the bin/ folder'
  exit
fi

num_run=500
config_name="config.json"

# clear existing flags
rm ../../../ECE499/Inconsistency-on-Medley/bin/membership/flag
rm ../../../ECE499/Inconsistency-on-Medley/bin/membership/done

set -ea

for (( i=1; i<=$num_run; i++ )); do    
    trap 'kill 0' SIGINT

    cd ../../../ECE499/Inconsistency-on-Medley/bin
    # ./run.sh $config_name & # run churn traces through medley
    nice -n 3 bash -c "./run.sh ${config_name}" & # run churn traces through medley
    cd ../../../camera_debug/camera/bin/
    nice -n 9 bash -c "./run.sh ${config_name}" & # run camera
    # ./run.sh $config_name & # run camera 
    wait
    cat stats.json >> "../analyze/outputs/check_violations.txt"
done