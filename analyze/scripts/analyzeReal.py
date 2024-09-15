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
        "exponential,uniform" : "red",
        "exponential,zipfian" : "orange",
        "weibull,zipfian" : "blue",
        "weibull,uniform" : "green"
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
        
    # with open(f"../{batched_terms[batched]}{'/5delay_rerun' if param!='churn_ratio' else ''}/{folders[param]}/{temporal}_{spatial}.txt") as f:
    tfname = f"../resubmission_data/distributions/{param}/{temporal}_{spatial}.txt"
    print(tfname)
    with open(tfname) as f:
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
                    # norm_ave = a['networkMetric'][MODE] / len(lst)
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
            # norm_ave = a['networkMetric'][MODE] / len(lst)


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

def graphlineWP():
    inds = []
    aves = []
    for line in open("../resubmission_data/distributions/N/wireless_paxos.txt").readlines():
        comma = line.find(",")
        inds.append(int(line[:comma]))
        aves.append(float(line[comma+1:]))
    inds = np.array(inds); aves = np.array(aves)
    plt.plot(inds,aves,'-o',label="Wireless Paxos",color="brown")
    
    slope, intercept = np.polyfit(inds, aves, 1)
    print(slope, intercept)
    x_extrapolate = np.linspace(inds[1], 37)  
    y_extrapolate = slope * x_extrapolate + intercept
    plt.plot(x_extrapolate, y_extrapolate, '--')
    plt.ylim(ymax=y_extrapolate[-1])



plt.figure(figsize=(6.4,3.8))
plt.tight_layout()
plt.subplots_adjust(bottom=0.15)


# for temporal in ["exponential"]:
for temporal in ["exponential","weibull"]:
    for spatial in ["uniform","zipfian"]:
        try:
            graphline(temporal,spatial)
        except FileNotFoundError:
            print(f"No data for {temporal},{spatial}")
if True:  ## graph wireless paxos line?
    graphlineWP()
    # plt.xscale('log')




if param=="msg_drop_rate":
    plt.yscale("log")
    plt.ylim(ymin=1)
    ax = plt.gca()
    ax.yaxis.set_major_formatter(mticker.ScalarFormatter())
    ax.ticklabel_format(axis='y',style="plain")
else:
    # pass
    plt.ylim(ymin=0)
    # pass

# plt.xscale("log")
if param in ["N","ir_ratio"] and MODE=="e2eMsgTotal":
    plt.ylim(ymax = 4, ymin=2)
elif param == "churn_ratio" and MODE=="e2eMsgTotal":
    plt.ylim(ymax = 5, ymin = 3)


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