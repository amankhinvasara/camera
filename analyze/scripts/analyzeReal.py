import json 
import matplotlib.pyplot as plt 
import numpy as np
import matplotlib.ticker as mticker
import sys

MODE = "waitTimes"
# MODE = "e2eMsgTotal"
# MODE = "e2eMsgSizeTotal"
# MODE = "h2hMsgTotal"
# MODE = "h2hMsgSizeTotal"

param = sys.argv[1]
batched = sys.argv[2]
param_options = ["ir_ratio","msg_drop_rate","N","churn_ratio"]

param_display = {
    "ir_ratio" : "IA:D ratio",
    "msg_drop_rate" : "Drop rate",
    "N":"System Size",
    "churn_ratio" : "Churn Forward Rate"
}

if param not in param_options:
    print("incorrect param, options are:")
    print(param_options)
    exit()

def graphline(temporal,spatial,param=param):

    colors = {
        "Poisson,uniform" : "red",
        "Poisson,zipfian" : "orange",
        "Weibull,zipfian" : "blue",
        "Weibull,uniform" : "green"
    }

    abbrev = {
        "P" : "Poisson",
        "W" : "Weibull"
    }

    folders = {
        "ir_ratio":"ratio_outputs",
        "msg_drop_rate": "drop_outputs",
        "N" : "N_outputs",
        "churn_ratio" : "low_delay"
    }

    batched_terms = {
        "batch" : "distr_batched",
        "Nbatch" : "distr_nonbatched",
        "churn" : "churn"
    }

    temp=""
    d = {}
        
    with open(f"../{batched_terms[batched]}{'/5delay_rerun' if param!='churn_ratio' else ''}/{folders[param]}/{temporal}_{spatial}.txt") as f:
        for line in f:
            if "}{" in line or "num_serv" in line:
                temp+="}"
                a = json.loads(temp)
                if "run_num" in line:
                    temp = ""
                else:
                    temp = "{"
                lst = a['algorithmMetric']['waitTimes']
                n = int(a['N'])
                if MODE == "waitTimes":
                    if param == "churn_ratio":
                        norm_ave = sum(lst[:50])/50
                    else:
                        norm_ave = sum(lst)/(len(lst))
                    norm_ave/=1000 # conversion to seconds
                else:
                    norm_ave = a['networkMetric'][MODE] / n / len(lst)
                
                cr = float(a[param])
                if cr in d:
                    d[cr].append(norm_ave)
                else:
                    d[cr] = [norm_ave]
        
            else:
                temp+=line

        a = json.loads(temp)
        temp = ""
        lst = a['algorithmMetric']['waitTimes']
        n = int(a['N'])
        if MODE == "waitTimes":
            if param == "churn_ratio":
                norm_ave = sum(lst[:50])/50
            else:
                norm_ave = sum(lst)/(len(lst))
            norm_ave/=1000 # ms to seconds
        else:
            norm_ave = a['networkMetric'][MODE] / n / len(lst)


        cr = float(a[param])
        if cr in d:
            d[cr].append(norm_ave)
        else:
            d[cr] = [norm_ave]
        
    # print(sorted(d[1024]))
    # mins = []
    # maxs = []
    # _25ths = []
    # _75ths = []
    inds = []
    aves = []
    for key, value in d.items():
        st = sorted(value)
        # mins.append(st[0])
        # maxs.append(st[-1])
        # _25ths.append(st[len(st)//4])
        # _75ths.append(st[3*(len(st)//4)])
        if param=="churn_ratio":
            inds.append(3/(5*key))
        else:
            inds.append(key)
        aves.append(sum(st)/len(st))

    # thick = 10
    # thin = 4

    # print(maxs[-3:])

    inds = np.array(inds)
    # maxs = np.array(maxs)
    # _75ths = np.array(_75ths)
    # _25ths = np.array(_25ths)
    # mins = np.array(mins)
    aves = np.array(aves)

    # plt.bar(inds,maxs-_75ths,width=thin,bottom=_75ths)
    # plt.bar(inds,_75ths-_25ths,width=thick,bottom=_25ths)
    # plt.bar(inds,_25ths-mins,width=thin,bottom=mins)
    # plt.plot(inds,aves)
    # plt.scatter(inds,aves,label=f"{temporal}_{spatial}")
    keystring = f"{abbrev[temporal]},{spatial}"
    if temporal=="W":
        lstyle = "dashed"
        linewidth=2
    else:
        lstyle = "solid"
        linewidth = 3
    if temporal=="W" and spatial=="zipfian":
        print(inds)
        print(aves)
    plt.plot(inds,aves,'-o',label=keystring,color=f"{colors[keystring]}",)
    # plt.plot(inds,aves,label=keystring,color=f"{colors[keystring]}",linestyle=lstyle,linewidth=linewidth)
    # plt.xlabel(inds)


for temporal in ["P","W"]:
    for spatial in ["uniform","zipfian"]:
        try:
            graphline(temporal,spatial)
        except FileNotFoundError:
            print(f"No data for {temporal},{spatial}")
# graphline("P","zipfian")

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
if param=="ir_ratio" and MODE!="waitTimes":
    plt.ylim(ymax = 4.9)



# plt.xlabel('N')
# if NORMALIZE:
#     plt.ylabel(f"{MODE} (Normalized)")
# else:
    # plt.ylabel(f"{MODE}")

mode_display = {
    "waitTimes" : "Wait Times",
    "e2eMsgTotal" : "End-to-End Message Count"
}


plt.ylabel(f'{mode_display[MODE]} {"(secs)" if MODE=="waitTimes" else ""}')
plt.xlabel(param_display[param])
plt.legend()
plt.title(f"{mode_display[MODE]} Against {param_display[param]}")
plt.show()
# plt.savefig(f"../img/test2.png")
plt.savefig(f"../img/test.png")

# print(f"slows: {slows}")