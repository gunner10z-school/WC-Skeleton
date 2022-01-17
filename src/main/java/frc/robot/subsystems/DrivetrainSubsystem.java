package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.controller.PIDController;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lib.RobotType;

import static frc.robot.Constants.*;

public class DrivetrainSubsystem extends SubsystemBase {
    PIDController encoderPID;
    SpeedControllerGroup leftMotors; // With more than one motor on each side, use  a SpeedControllerGroup
    SpeedControllerGroup rightMotors;
    private final Encoder rightEncoder = new Encoder(rightEncoderChannelA, rightEncoderChannelB, false, Encoder.EncodingType.k2X);;
    private final Encoder leftEncoder = new Encoder(leftEncoderChannelA, leftEncoderChannelB, true, Encoder.EncodingType.k2X);

    private final DifferentialDrive drive; // DifferentialDrive manages steering based off of inputted power values
    public static double maxDriverSpeed = speedScale;

    public DrivetrainSubsystem(RobotType robotType) {
        // Ports are defined in Constants
        switch(robotType) { // OFFICIAL Drivetrain utilizes TalonFX's, which are integrated into the motors themselves.
            case JANKBOT: // JANKBOT utilizes VictorSPXs as its motor controllers
                leftMotors = new SpeedControllerGroup(new WPI_VictorSPX(leftMotor1Port), new WPI_VictorSPX(leftMotor2Port));
                rightMotors = new SpeedControllerGroup(new WPI_VictorSPX(rightMotor1Port), new WPI_VictorSPX(rightMotor2Port));
                break;
            case KITBOT: // KITBOT utilizes TalonSRXs as its motor controllers
                leftMotors = new SpeedControllerGroup(new WPI_TalonSRX(leftMotor1Port), new WPI_TalonSRX(leftMotor2Port));
                rightMotors = new SpeedControllerGroup(new WPI_TalonSRX(rightMotor1Port), new WPI_TalonSRX(rightMotor2Port));
                break;
        }

        leftMotors.setInverted(robotType.isInverted());
        rightMotors.setInverted(robotType.isInverted());
        drive = new DifferentialDrive(leftMotors, rightMotors);

        drive.setDeadband(0.2); // Scales joystick values. 0.02 is the default
        drive.setMaxOutput(speedScale);

        leftEncoder.setDistancePerPulse(6.0 * 0.0254 * Math.PI / 2048); // 6 inch wheel, to meters, 2048 ticks
        rightEncoder.setDistancePerPulse(6.0 * 0.0254 * Math.PI / 2048); // 6 inch wheel, to meters, 2048 ticks
        encoderPID = new PIDController(9, 0, 0);
    }

    public void tankDrive(double leftSpeed, double rightSpeed) {
        int leftSign = leftSpeed >= 0 ? 1 : -1; // Checks leftSpeed and gathers whether it is negative or positive
        int rightSign = rightSpeed >= 0 ? 1 : -1; // Checks rightSpeed and gathers whether it is negative or positive

        double leftPower = ((speedScale - minDrivePower) * Math.abs(leftSpeed) + minDrivePower) * leftSign;
        double rightPower = ((speedScale - minDrivePower) * Math.abs(rightSpeed) + minDrivePower) * rightSign;

        drive.tankDrive(leftPower, rightPower); // Calls WPILib DifferentialDrive method tankDrive(LSpeed, RSpeed)
    }

    public void setMaxOutput(double maxOutput) {
        drive.setMaxOutput(maxOutput);
    }

    public void arcadeDrive(double xSpeed, double zRotation) {
        // Account for changes in turning when the forward direction changes, if it doesn't work use the one above
        drive.arcadeDrive(xSpeed * maxDriverSpeed,
                maxDriverSpeed < 0 ? zRotation * maxDriverSpeed : -zRotation * maxDriverSpeed);
    }

    public void resetLeftEncoder() {
        leftEncoder.reset();
    }

    public void resetRightEncoder() {
        rightEncoder.reset();
    }

    public double getRightEncoderDistance() {
        SmartDashboard.putNumber("Right Enc", rightEncoder.getDistance());
        return rightEncoder.getDistance(); // Scaled to imperial from setDistancePerPulse
    }

    public double getLeftEncoderDistance() {
        SmartDashboard.putNumber("Left Enc", leftEncoder.getDistance());
        return leftEncoder.getDistance(); // Scaled to imperial from setDistancePerPulse
    }

    public double leftEncoderCorrection(double encoderSetPoint){
        encoderPID.setSetpoint(encoderSetPoint);
        System.out.println(encoderPID.calculate(getRightEncoderDistance()-getLeftEncoderDistance()));
        return encoderPID.calculate(getRightEncoderDistance()-getLeftEncoderDistance());
    }

    public double rightEncoderCorrection(double encoderSetPoint){
        encoderPID.setSetpoint(encoderSetPoint);
        System.out.println( -encoderPID.calculate(getRightEncoderDistance()-getLeftEncoderDistance()));
        return -encoderPID.calculate(getRightEncoderDistance()-getLeftEncoderDistance());
    }

    public double calculateTrapezoid(PIDController pid, long startTime, double maxSpeed, double trapezoidTime) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime <= trapezoidTime) {
            return pid.calculate(getRightEncoderDistance()) >= 0 ?
                    Math.min(pid.calculate(getRightEncoderDistance()) * (elapsedTime / trapezoidTime), maxSpeed) :
                    Math.max(pid.calculate(getRightEncoderDistance()) * (elapsedTime / trapezoidTime), -maxSpeed);
        } else {
            return pid.calculate(getRightEncoderDistance()) >= 0 ?
                    Math.min(pid.calculate(getRightEncoderDistance()), maxSpeed) :
                    Math.max(pid.calculate(getRightEncoderDistance()), -maxSpeed);
        }
    }

    @Override
    public void periodic() {
        // SubsystemBase native method
    }
}

