/*
 * Copyright (c) 2014, De Novo Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.denovogroup.rangzen.simulation;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.display.Console;
import sim.engine.SimState;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;
import sim.util.gui.SimpleColorMap;

import java.awt.*;
import javax.swing.*;

public class MessagePropagationSimulationWithGUI extends GUIState {
    public Display2D display;
    public JFrame displayFrame;
    private ContinuousPortrayal2D spacePortrayal = new ContinuousPortrayal2D();
    private NetworkPortrayal2D socialPortrayal = new NetworkPortrayal2D();

    public MessagePropagationSimulationWithGUI() { 
      super(new MessagePropagationSimulation(System.currentTimeMillis()));
    }
    
    public MessagePropagationSimulationWithGUI(SimState state) { 
      super(state);
     }

    public void setupPortrayals() {
      MessagePropagationSimulation sim = (MessagePropagationSimulation) state;

      // Display the mobility of the people.
      spacePortrayal.setField(sim.space);
      // spacePortrayal.setPortrayalForAll(new OvalPortrayal2D(5));

      // Display social edges between them.
      socialPortrayal.setField(new SpatialNetwork2D(sim.space, sim.socialNetwork));
      socialPortrayal.setPortrayalForAll(new SimpleEdgePortrayal2D());

      // Details, details.
      display.reset();    // reschedule the displayer
      display.setBackdrop(Color.white);
      display.repaint();  // redraw the display
    }

    public void start() {
      super.start();      
      setupPortrayals();  // set up our portrayals
    }
    public void load(SimState state) {
      super.load(state);      
      setupPortrayals();  // set up our portrayals for the new SimState model
    }

    public void init(Controller c) {
      super.init(c);

      // MessagePropagationSimulation sim = (MessagePropagationSimulation) state;
      // display = new Display2D(sim.width, sim.height, this);
      // display.setClipping(false);
      // displayFrame = display.createFrame();
      // c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
      // displayFrame.setVisible(true);
      // display.attach(socialPortrayal, "Social");
      // display.attach(spacePortrayal, "Space");  // attach the portrayals
    }

    public static void main(String[] args) {
      // new MessagePropagationSimulationWithGUI().createController();
      // MessagePropagationSimulationWithGUI mpsGUI = new MessagePropagationSimulationWithGUI();
      // Console c = new Console(mpsGUI);
      // c.setVisible(true);
    }

    public static String getName() { 
      return "Message Propagation Simulation"; 
    }
    
    public static Object getInfo() {
      return "Simulation of Message Propagation in Rangzen."; 
    }



}
