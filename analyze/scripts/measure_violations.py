import json 
import matplotlib.pyplot as plt 
import numpy as np
import matplotlib.ticker as mticker
import sys

WORST_COUNT = "worstVioCount"
TIME_RATIO = "vioTimeRatio"
PAIRS = "unsafePairs"

def getVio(temporal = "weibull" , spatial="zipfian"):


    temp=""
    d = {
        WORST_COUNT: {},
        TIME_RATIO: {},
        PAIRS: {}
    }
        
    # with open(f"../{batched_terms[batched]}{'/5delay_rerun' if param!='churn_ratio' else ''}/{folders[param]}/{temporal}_{spatial}.txt") as f:
    with open(f"../resubmission_data/RA_violations/{temporal}_{spatial}.txt") as f:
        for line in f:
            if "}{" in line or "num_serv" in line:
                temp+="}"
                a = json.loads(temp)
                if "run_num" in line:
                    temp = ""
                else:
                    temp = "{"
                droprate = a['msg_drop_rate']
                worst = float(a['qualityMetric'][WORST_COUNT])
                time = float(a['qualityMetric'][TIME_RATIO])
                pairs = float(a['qualityMetric'][PAIRS])
                if droprate in d[WORST_COUNT]:
                    d[WORST_COUNT][droprate].append(worst)
                    d[TIME_RATIO][droprate].append(time)
                    d[PAIRS][droprate].append(pairs)
                else:
                    d[WORST_COUNT][droprate] = [worst]
                    d[TIME_RATIO][droprate] = [time]
                    d[PAIRS][droprate] = [pairs]        
            else:
                temp+=line

        a = json.loads(temp)
        temp = ""

        droprate = a['msg_drop_rate']
        worst = float(a['qualityMetric'][WORST_COUNT])
        time = float(a['qualityMetric'][TIME_RATIO])
        pairs = float(a['qualityMetric'][PAIRS])
        if droprate in d[WORST_COUNT]:
            d[WORST_COUNT][droprate].append(worst)
            d[TIME_RATIO][droprate].append(time)
            d[PAIRS][droprate].append(pairs)
        else:
            d[WORST_COUNT][droprate] = [worst]
            d[TIME_RATIO][droprate] = [time]
            d[PAIRS][droprate] = [pairs]    
            

    for rate in sorted(d[WORST_COUNT].keys()):
        worsts = np.array(d[WORST_COUNT][rate])
        times = np.array(d[TIME_RATIO][rate])
        pairs = np.array(d[PAIRS][rate])
        print(f"{rate}: worsts - {np.average(worsts)}, timeratio - {np.average(times)}, pairs - {np.average(pairs)}")
        # print("max worst is ", np.max(worsts))

    return

    inds = np.array(inds)
    aves = np.array(aves)


    keystring = f"{temporal},{spatial}"
    if temporal=="weibull":
        lstyle = "dashed"
        linewidth=2
    else:
        lstyle = "solid"
        linewidth = 3
    if temporal=="weibull" and spatial=="zipfian":
        print(inds)
        print(aves)
    plt.plot(inds,aves,'-o',label=keystring,color=f"{colors[keystring]}",)
    # plt.plot(inds,aves,label=keystring,color=f"{colors[keystring]}",linestyle=lstyle,linewidth=linewidth)
    # plt.xlabel(inds)



getVio()
exit(0)
# # for temporal in ["exponential"]:
# for temporal in ["exponential","weibull"]:
#     for spatial in ["uniform","zipfian"]:
#         try:
#             graphline(temporal,spatial)
#         except FileNotFoundError:
#             print(f"No data for {temporal},{spatial}")
# # graphline("P","zipfian")

if param=="msg_drop_rate":
    plt.yscale("log")
    plt.ylim(ymin=1)
    ax = plt.gca()
    ax.yaxis.set_major_formatter(mticker.ScalarFormatter())
    ax.ticklabel_format(axis='y',style="plain")
else:
    plt.ylim(ymin=0)
    # pass

# plt.xscale("log")
if param=="ir_ratio" and MODE=="e2eMsgTotal":
    plt.ylim(ymax = 4.9)
if param == "N" and MODE=="e2eMsgTotal":
    plt.ylim(ymax = 4)


mode_display = {
    "waitTimes" : "Wait Times",
    "e2eMsgTotal" : "End-to-End Message Count",
    "e2eMsgSizeTotal" : "End-to-End Message Size"
}


plt.ylabel(f'{mode_display[MODE]} {"(secs)" if MODE=="waitTimes" else ""}')
plt.xlabel(param_display[param])
plt.legend()
plt.title(f"{mode_display[MODE]} Against {param_display[param]}")
plt.show()
# plt.savefig(f"../img/test2.png")
plt.savefig(f"../img/test.png")

# print(f"slows: {slows}")