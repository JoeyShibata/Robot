// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.revrobotics.CANSparkLowLevel.MotorType;
import com.revrobotics.CANSparkMax;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import java.util.function.DoubleSupplier;

public class Climb extends SubsystemBase {
  private CANSparkMax winchRight =
      new CANSparkMax(Constants.Climb.rightCANID, MotorType.kBrushless);
  private CANSparkMax winchLeft = new CANSparkMax(Constants.Climb.leftCANID, MotorType.kBrushless);
  private double speed;

  private DigitalInput winchLimitLeft = new DigitalInput(Constants.Climb.winchLimitLeft);
  private DigitalInput winchLimitRight = new DigitalInput(Constants.Climb.winchLimitRight);

  /** Creates a new Climb. */
  public Climb() {}

  @Override
  public void periodic() {
    SmartDashboard.putNumber("Left Climb Position", winchLeft.getEncoder().getPosition());
    SmartDashboard.putNumber("Right Climb Position", winchRight.getEncoder().getPosition());
    SmartDashboard.putBoolean("Right Climb Limit", winchLimitRight.get());
    SmartDashboard.putBoolean("Left Climb Limit", winchLimitLeft.get());
  }

  // This stops the left and right winches respectively.
  public void stopWinchLeft() {
    winchLeft.set(0);
  }

  public void stopWinchRight() {
    winchRight.set(0);
  }

  public void setWinch(double speed) {
    double convertedSpeed = speed * Constants.Climb.motorSpeedFactor;
    this.speed = speed;
    if (winchLimitLeft.get() || convertedSpeed <= 0) {
      winchLeft.set(convertedSpeed);
    } else {
      stopWinchLeft();
      winchLeft.getEncoder().setPosition(0);
    }
    if (winchLimitRight.get() || convertedSpeed <= 0) {
      winchRight.set(convertedSpeed);
    } else {
      stopWinchRight();
      winchRight.getEncoder().setPosition(0);
    }
  }

  public Command setWinchCommand(DoubleSupplier speed) {
    return this.run(() -> setWinch(speed.getAsDouble()));
  }
}
