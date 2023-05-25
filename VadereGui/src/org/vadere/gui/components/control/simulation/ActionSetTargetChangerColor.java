package org.vadere.gui.components.control.simulation;

import java.awt.*;
import javax.swing.*;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;

public class ActionSetTargetChangerColor extends ActionSetColor {

  public ActionSetTargetChangerColor(
      String name, SimulationModel<? extends DefaultSimulationConfig> model, JPanel coloredPanel) {
    super(name, model, coloredPanel);
  }

  @Override
  protected void saveColor(Color color) {
    model.config.setTargetChangerColor(color);
  }
}
