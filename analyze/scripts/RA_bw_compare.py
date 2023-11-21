import json 
import matplotlib.pyplot as plt 
import numpy as np
import matplotlib.ticker as mticker
import sys


def getPair(temporal,spatial):
    temp=""
    with open(f"../resubmission_data/distributions/no_drop/{temporal}_{spatial}.txt") as f:
        RAs = []
        normals = []
        for line in f:
            if "}{" in line or "num_serv" in line:
                temp+="}"
                a = json.loads(temp)
                if "run_num" in line:
                    temp = ""
                else:
                    temp = "{"
                
                
                RA = a['networkMetric']['RAe2eMsgSizeTotal']
                normal = a['networkMetric']['e2eMsgSizeTotal']
                RAs.append(RA)
                normals.append(normal) 
            else:
                temp+=line

        a = json.loads(temp)
        RA = a['networkMetric']['RAe2eMsgSizeTotal']
        normal = a['networkMetric']['e2eMsgSizeTotal']
        RAs.append(RA)
        normals.append(normal)

        RAs = np.array(RA)
        normals = np.array(normals)

        print(f"{temporal}_{spatial}:",np.average(RAs)/1e6,np.average(normals)/1e6,np.average(normals/RAs))



# for temporal in ["exponential"]:
for temporal in ["exponential","weibull"]:
    for spatial in ["uniform","zipfian"]:
        try:
            getPair(temporal,spatial)
        except FileNotFoundError:
            print(f"No data for {temporal},{spatial}")