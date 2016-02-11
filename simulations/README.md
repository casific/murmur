Simulator for Rangzen
===================

This is the simulator used to evaluate Rangzen, a delay-tolerant mesh-network
system for message broadcast without infrastructure. Rangzen's design and our
analytical and simulation results about it are under submission to ACM CCS '14.

The simulator is written in Java and uses MASON
(http://cs.gmu.edu/~eclab/projects/mason/) - an awesome multi-agent simulation
toolkit developed at George Mason University.

## Quickstart
The project includes a prebuilt .jar which you should be able to run out of the
box:

    git clone https://github.com/denovogroup/rangzen-simulator.git              
    cd rangzen-simulator
    java -jar prebuilt/simulator.jar

This will run the GUI interface to the simulator using the synthetic (fake) data
provided with this application and setting some defaults for configurable parameters.
Read "Input data" below for information on where to get real data and how to use
it with the simulator, read "parameters" for the simulation parameters you can
change, and read "Building the simulator" if you want to change and rebuild
the simulator itself.

## Input data (mobility traces and social network data)
We've included some synthetic data so that the simulator runs out of the box.
This data is fake though - it's not anyone's actual movement patterns or social
network. The (real, anonymized) data we used in the paper comes from a several
sources and studies. Several of the datasets are available through CRAWDAD
(http://www.crawdad.org/), which is a cool archive of wireless related datasets
based at Dartmouth College. If you want to replicate our simulations, sign up
with CRAWDAD and grab the datasets we used, like the epfl/mobility
(http://www.crawdad.org/epfl/mobility/) dataset of cab mobility in San
Francisco.

## Building the simulator
The simulator ships with a prebuilt .jar that can reproduce the results in our
paper. If you want to modify the simulator and rebuild it, read on.

The simulator's source is designed to be built using Buck, so install that first.
Instructions are provided in the "Dependencies" section.

Once you've got Buck, you should be able to do:
  
    git clone https://github.com/denovogroup/rangzen-simulator.git
    cd rangzen-simulator
    buck run proximitySimulation
    
This should build and run the GUI version of the simulator with some reasonable
default settings and using the sample dataset we provide. All the source code
is located in the java/org/denovogroup/rangzen/simulation/ directory.

## Dependencies

### Buck
The simulator application is designed to be built using Buck
(https://github.com/facebook/buck), Facebook's open source build system for
Android/Java projects. In this case, we use only the functionality required to
build Java, since the simulator isn't an Android app.

To build Buck, do the following:

    git clone https://github.com/facebook/buck.git
    cd buck
    ant
    ./bin/buck --help
