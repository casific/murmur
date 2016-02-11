import os

trials = 40

filebase = '400nodes/sybil'


try:
    os.system("buck build proximitySimulationNoGUI")
except:
    exit()

#for i in range(trials):
#    os.system("java -jar -Xms6G buck-out/gen/apps/simulation/proximitySimulationNoGUI.jar -p > results/"+filebase+"/sybilPopular"+str(i)+".json")
#    os.system("java -jar -Xms6G buck-out/gen/apps/simulation/proximitySimulationNoGUI.jar -r > results/"+filebase+"/sybilAverage"+str(i)+".json")
#    os.system("java -jar -Xms6G buck-out/gen/apps/simulation/proximitySimulationNoGUI.jar -u > results/"+filebase+"/sybilUnpopular"+str(i)+".json")
#    os.system("java -jar -Xms6G buck-out/gen/apps/simulation/proximitySimulationNoGUI.jar -a > results/"+filebase+"/sybilSybil"+str(i)+".json")


os.system("parallel --max-procs 8 java -jar -Xms4G buck-out/gen/apps/simulation/proximitySimulationNoGUI.jar {1} '>' results/400nodes/sybil{1}{2}.json :::: flags.txt :::: numbers.txt")
