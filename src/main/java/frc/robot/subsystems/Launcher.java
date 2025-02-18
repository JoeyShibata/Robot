// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.revrobotics.CANSparkBase.ControlType;
import com.revrobotics.CANSparkFlex;
import com.revrobotics.CANSparkLowLevel.MotorType;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkAbsoluteEncoder;
import com.revrobotics.SparkAbsoluteEncoder.Type;
import com.revrobotics.SparkPIDController;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.Launch.LaunchPosition;
import frc.robot.Robot;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Launcher extends SubsystemBase {
  /** Creates a new Launcher. */
  private CANSparkFlex launcher =
      new CANSparkFlex(Constants.Launch.launcherID, MotorType.kBrushless);

  private SparkPIDController launcherController;
  private RelativeEncoder launcherEncoder;

  private CANSparkMax angle = new CANSparkMax(Constants.Launch.angleID, MotorType.kBrushless);
  private SparkPIDController angleController;
  private SparkAbsoluteEncoder angleEncoder;

  private boolean tuningPIDS = false;

  private LaunchPosition goalPosition, curPosition;

  private Timer switchTimer;

  public Launcher() {

    angleController = angle.getPIDController();
    launcherController = launcher.getPIDController();

    launcherEncoder = launcher.getEncoder();
    angleEncoder = angle.getAbsoluteEncoder(Type.kDutyCycle);

    launcherController.setFeedbackDevice(launcherEncoder);
    angleController.setFeedbackDevice(angleEncoder);

    launcherEncoder.setVelocityConversionFactor(Constants.Launch.launcherConversionFactor);
    angleEncoder.setPositionConversionFactor(Constants.Launch.angleConversionFactor);

    setPIDsDefault();
    showPIDs();
  }

  private void showPIDs() {
    SmartDashboard.putNumber("Launch P", launcherController.getP());
    SmartDashboard.putNumber("Launch I", launcherController.getI());
    SmartDashboard.putNumber("Launch D", launcherController.getD());
    SmartDashboard.putNumber("Launch Velo", getCurrentVelocity());

    SmartDashboard.putNumber("Angle P", angleController.getP());
    SmartDashboard.putNumber("Angle I", angleController.getI());
    SmartDashboard.putNumber("Angle D", angleController.getD());
    SmartDashboard.putNumber("Angle Position", angleEncoder.getPosition());
  }

  private void setPIDsDefault() {
    updateLauncherPIDs(
        Constants.Launch.launcherP, Constants.Launch.launcherI, Constants.Launch.launcherD);
    updateAnglePIDs(Constants.Launch.angleP, Constants.Launch.angleI, Constants.Launch.angleD);
  }

  private void updateLauncherPIDs(double p, double i, double d) {
    launcherController.setP(p);
    launcherController.setI(i);
    launcherController.setD(d);
  }

  private void updateAnglePIDs(double p, double i, double d) {
    angleController.setP(p);
    angleController.setI(i);
    angleController.setD(d);
  }

  private void updatePIDFromDashboard(String keyWord) {
    double p = SmartDashboard.getNumber(keyWord + " P", 0);
    double i = SmartDashboard.getNumber(keyWord + " I", 0);
    double d = SmartDashboard.getNumber(keyWord + " D", 0);

    String desiredMethod = "update" + keyWord + "PIDs";
    Class<?>[] paramTypes = {double.class, double.class, double.class};
    Method method;
    try {
      method = this.getClass().getMethod(desiredMethod, paramTypes);
    } catch (SecurityException e) {
      return;
    } catch (NoSuchMethodException e) {
      return;
    }
    try {
      method.invoke(this, p, i, d);
    } catch (IllegalArgumentException e) {
      return;
    } catch (IllegalAccessException e) {
      return;
    } catch (InvocationTargetException e) {
      return;
    }
  }

  public void setLaunchVelocity(double velocity) {
    launcherController.setReference(velocity, ControlType.kVelocity);
  }

  public double getCurrentVelocity() {
    return launcherEncoder.getVelocity();
  }

  public void setLauncherSpeed(double speed) {
    launcher.set(speed);
  }

  public boolean isWithinVeloPercentage(double percent, double targetVelo) {
    double currentPercent = getCurrentVelocity() / targetVelo;
    return (1 - percent <= currentPercent && currentPercent <= 1 + percent);
  }

  public boolean readyToLaunch(double targetVelo) {
    if (Robot.isSimulation()) {
      return true;
    }
    return isWithinVeloPercentage(Constants.Launch.allowedVeloPercent, targetVelo);
  }

  public void setLaunchPosition(LaunchPosition launchPosition) {
    goalPosition = launchPosition;
  }

  public LaunchPosition getLaunchPosition() {
    return curPosition;
  }

  public void switchAngle() {
    boolean isFar = (goalPosition == LaunchPosition.FAR);
    goalPosition = (isFar ? LaunchPosition.CLOSE : LaunchPosition.FAR);
  }

  public void togglePIDTuning() {
    tuningPIDS = !tuningPIDS;
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
    if (tuningPIDS) {
      updatePIDFromDashboard("Launcher");
      updatePIDFromDashboard("Angle");
    }
    if (goalPosition == LaunchPosition.FAR) {
      angleController.setReference(Constants.Launch.farLaunchPosition, ControlType.kPosition);
    } else {
      angleController.setReference(Constants.Launch.closeLaunchPosition, ControlType.kPosition);
    }
  }
}
