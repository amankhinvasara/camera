import sys
import json 
import numpy as np
import matplotlib.pyplot as plt 
import matplotlib.ticker as mticker

# CUT_FIRST = True
CUT_FIRST = False

# NORMALIZE = True
# NORMALIZE = False

# MODE = "waitTimes"
MODE = "e2eMsgTotal"
# MODE = "e2eMsgSizeTotal"
# MODE = "h2hMsgTotal"
# MODE = "h2hMsgSizeTotal"

param = sys.argv[1]

try:
    NORMALIZE = True if sys.argv[2]=="norm" else False
except:
    NORMALIZE = False

param_options = ["cr","n","drop"]

param_display = {
    "cr":"Concurrent Requesters",
    "n" : "System Size",
    "drop":"Message Drop Rate"
}

param_json = {
    "cr":"conc_requesters",
    "n" : "N",
    "drop":"msg_drop_rate"
}

plt.figure(figsize=(6.4,2.1))
plt.tight_layout()
plt.subplots_adjust(bottom=0.21)

temp=""
d = {}
slows = 0

with open(f"../concurrent/short/{param}.txt") as f:
    for line in f:
        if "}{" in line or "num_serv" in line:
            temp+="}"
            a = json.loads(temp)
            if "run_num" in line:
                temp = ""
            else:
                temp = "{"
            lst = a['algorithmMetric']['waitTimes']
            if a['algorithmMetric']['slows']!=0:
                slows+=1
            if MODE == "waitTimes":
                norm_ave = sum(lst)/(len(lst))/1000
            else:
                # norm_ave = a['networkMetric'][MODE]
                norm_ave = (a['networkMetric'][MODE])/a['N']/len(lst)
                

            
            cr = a[param_json[param]]
            if NORMALIZE:
                norm_ave /= cr
            if cr in d:
                d[cr].append(norm_ave)
            else:
                d[cr] = [norm_ave]
    
        else:
            temp+=line

    # print(f"temp is {temp}")
    a = json.loads(temp)
    if "run_num" in line:
        temp = ""
    else:
        temp = "{"
    lst = a['algorithmMetric']['waitTimes']
    if a['algorithmMetric']['slows']!=0:
        slows+=1
    if MODE == "waitTimes":
        norm_ave = sum(lst)/(len(lst))/1000
    else:
        # norm_ave = a['networkMetric'][MODE]
        norm_ave = (a['networkMetric'][MODE])/a['N']/len(lst)
        
    cr = a[param_json[param]]
    if NORMALIZE:
        norm_ave /= cr
    if cr in d:
        d[cr].append(norm_ave)
    else:
        d[cr] = [norm_ave]

# print(sorted(d[1024]))
mins = []
maxs = []
_25ths = []
_75ths = []
inds = []
aves = []
for key, value in d.items():
    st = sorted(value)
    mins.append(st[0])
    maxs.append(st[-1])
    _25ths.append(st[len(st)//4])
    _75ths.append(st[3*(len(st)//4)])
    inds.append(key)
    aves.append(sum(st)/len(st))

thick = 10
thin = 4

if param=="drop":
    thick/=200
    thin/=200
elif param=="cr":
    thick/=2
    thin/=2

if CUT_FIRST:
    inds.pop(0)
    maxs.pop(0)
    _75ths.pop(0)
    _25ths.pop(0)
    mins.pop(0)
    aves.pop(0)

# print(maxs[-3:])

inds = np.array(inds)
maxs = np.array(maxs)
_75ths = np.array(_75ths)
_25ths = np.array(_25ths)
mins = np.array(mins)
aves = np.array(aves)

plt.bar(inds,maxs-_75ths,width=thin,bottom=_75ths)
plt.bar(inds,_75ths-_25ths,width=thick,bottom=_25ths)
plt.bar(inds,_25ths-mins,width=thin,bottom=mins)
# plt.scatter(inds,aves)
plt.plot(inds,aves,'-o',color="red")

# print(inds)
# print(aves)

# plt.ylim(ymin=1 if param=="drop" else 0)
if MODE=="e2eMsgTotal" and param in ["cr","n"]:
    plt.ylim(ymin=3)
elif param=="drop":
    plt.ylim(ymin=1)
else:
    plt.ylim(ymin=0)

if param=="drop":
    plt.yscale('log')
    # plt.xticks(inds)
    ax = plt.gca()
    ax.yaxis.set_major_formatter(mticker.ScalarFormatter())
    ax.ticklabel_format(axis='y',style="plain")
# else:
# if param=="cr":
#     plt.xscale('log')
plt.xlabel(param_display[param])
lab_suffix = "(secs)" if MODE=="waitTimes" else ""

mode_display = {
    "waitTimes" : "Wait Times",
    "e2eMsgTotal" : "End-to-End Message Count"
}

if NORMALIZE:
    plt.ylabel(f"{mode_display[MODE]} (Normalized) {lab_suffix}")
else:
    plt.ylabel(f"{mode_display[MODE]} {lab_suffix}")

plt.title(f"{mode_display[MODE]} Against {param_display[param]}")
plt.show()
plt.savefig(f"../img/test.png")

print(f"slows: {slows}")
print(aves)