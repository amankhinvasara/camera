import matplotlib.pyplot as plt 
import numpy as np
import json

d = { rate: list() for rate in [0.05,0.25,0.5,1.0,3.0]}

# fname = "measure_recOK"


fname = "../resubmission_data/distributions/no_drop/spatial"


temp=""
with open(fname) as f:
    for line in f:
        if "}{" in line or "num_serv" in line:
            temp+="}"
            a = json.loads(temp)
            if "run_num" in line:
                temp = ""
            else:
                temp = "{"

            ir_ratio = a['ir_ratio']
            if fname=="measure_recOK":
                values = a['algorithmMetric']['max_rec_OK_sizes']
                d[ir_ratio].append(np.average(values))
            elif fname=="RA_diff":
                RA = a['networkMetric']['RAe2eMsgSizeTotal']
                reg = a['networkMetric']['e2eMsgSizeTotal']
                d[ir_ratio].append(reg/RA)
            else:
                raise IndexError
        else:
            temp+=line

    # print(f"temp is {temp}")
    a = json.loads(temp)
    ir_ratio = a['ir_ratio']
    if fname=="measure_recOK":
        values = a['algorithmMetric']['max_rec_OK_sizes']
        d[ir_ratio].append(np.average(values))
    elif fname=="RA_diff":
        RA = a['networkMetric']['RAe2eMsgSizeTotal']
        reg = a['networkMetric']['e2eMsgSizeTotal']
        d[ir_ratio].append(reg/RA)
    else:
        raise IndexError


for rate,trials in d.items():
    print(f"{rate}: ave: {np.average(trials)}, stddev: {np.std(trials)}")
# print(d)