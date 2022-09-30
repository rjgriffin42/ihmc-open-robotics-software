import numpy as np
import sys
import matplotlib.pyplot as plt


#----------------SIMPLE PROMP------------------------------------------

#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
n_demos=10
demo = []
for i in range(1,n_demos+1):
    demos_pathname = 'demo' + str(i) + '.csv'
    demo.append(np.genfromtxt(demos_pathname,delimiter=',', dtype = float))
mean = np.genfromtxt('mean.csv',delimiter=',', dtype = float)
variance = np.genfromtxt('variance.csv',delimiter=',', dtype = float)

mean_handX = np.array([row[0] for row in mean])
std_handX= np.array([row[0] for row in variance])
mean_handY = np.array([row[1] for row in mean])
std_handY = np.array([row[1] for row in variance])
mean_handZ = np.array([row[2] for row in mean])
std_handZ = np.array([row[2] for row in variance])

fig = plt.figure()
colorX ='red'
colorY ='yellowgreen'
colorZ ='cornflowerblue'
democolorX='darkred'
democolorY='darkgreen'
democolorZ='midnightblue'

plt.plot(mean_handX,colorX,linewidth=4.0)
plt.fill_between(np.linspace(0, mean_handX.size, num=mean_handX.size), mean_handX - std_handX, mean_handX + std_handX, color=colorX, alpha=0.2)
plt.plot(mean_handY,colorY,linewidth=4.0)
plt.fill_between(np.linspace(0, mean_handY.size, num=mean_handY.size), mean_handY - std_handY, mean_handY + std_handY, color=colorY, alpha=0.2)
plt.plot(mean_handZ,colorZ,linewidth=4.0)
plt.fill_between(np.linspace(0, mean_handZ.size, num=mean_handZ.size), mean_handZ - std_handZ, mean_handZ + std_handZ, color=colorZ, alpha=0.2)
for i in range(0,n_demos):
    plt.plot(demo[i][:,0],democolorX)
    plt.plot(demo[i][:,1],democolorY)
    plt.plot(demo[i][:,2],democolorZ)
plt.xlabel('#samples')  
plt.ylabel('right hand [m]')
plt.grid(True)

