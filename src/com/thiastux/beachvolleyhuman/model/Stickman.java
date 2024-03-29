/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thiastux.beachvolleyhuman.model;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;

import java.util.HashMap;

/**
 *
 * @author mathias
 */
public class Stickman {

    private int animationIndex;

    public final float TORSO_HEIGHT = 4f;
    private final float TORSO_RADIUS = 1.5f;
    private final float PELVIS_HEIGHT = 2f;
    private final float PELVIS_RADIUS = 1.0f;
    public final float ULEG_LENGTH = 4f;
    public final float LLEG_LENGTH = 3f;
    private final float ULEG_RADIUS = 0.5f;
    private final float LLEG_RADIUS = 0.4f;
    private final float UARM_LENGTH = 2.6f;
    private final float LARM_LENGTH = 2.8f;
    public final float UARM_RADIUS = 0.4f;
    private final float LARM_RADIUS = 0.35f;
    private final float HAND_WIDTH = 0.4f;
    private final float HAND_LENGTH = 0.6f;
    private final float HAND_THICKNESS = 0.1f;
    private final float HEAD_RADIUS = 1f;
    private final float EYE_RADIUS = 0.08f;
    public final float SHOULDER_WIDTH = TORSO_RADIUS + UARM_RADIUS;

    private HashMap<Integer, Spatial> skeletonMap;
    private Spatial pelvisBone;

    public RigidBodyControl getrHandControl() {
        return rHandControl;
    }

    private RigidBodyControl rHandControl;

    public Stickman(Node rootNode, HashMap<Integer, Spatial> map, AssetManager assetManager, BulletAppState bulletAppState) {

        this.skeletonMap = map;
        
        Quaternion vertRotQuat = new Quaternion().fromAngles((float) Math.toRadians(90), 0f, 0f);

        //Torso
        Cylinder torsoMesh = new Cylinder(20, 20, TORSO_RADIUS, TORSO_HEIGHT, true);
        Geometry torsoGeometry = new Geometry("Box", torsoMesh);
        torsoGeometry.setLocalRotation(vertRotQuat);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.LightGray);
        mat.setColor("Diffuse", ColorRGBA.LightGray);
        torsoGeometry.setMaterial(mat);

        Sphere torsoJointMesh = new Sphere(50, 50, TORSO_RADIUS);
        Geometry torsoJointGeometry = new Geometry("TorsoJoint", torsoJointMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.LightGray);
        mat.setColor("Diffuse", ColorRGBA.LightGray);
        torsoJointGeometry.setMaterial(mat);

        Cylinder pelvisMesh = new Cylinder(20, 20, TORSO_RADIUS, PELVIS_HEIGHT, true);
        Geometry pelvisGeometry = new Geometry("Pelvis", pelvisMesh);
        pelvisGeometry.setLocalRotation(vertRotQuat);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.LightGray);
        mat.setColor("Diffuse", ColorRGBA.LightGray);
        pelvisGeometry.setMaterial(mat);

        //Eyes and Head
        Sphere eyeMesh = new Sphere(20, 20, EYE_RADIUS);
        Geometry rEye = new Geometry("rEye", eyeMesh);
        rEye.setLocalTranslation(-(HEAD_RADIUS / 3), 0f, 0f);
        Geometry lEye = new Geometry("lEye", eyeMesh);
        lEye.setLocalTranslation((HEAD_RADIUS / 3), 0f, 0f);
        mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Black);
        rEye.setMaterial(mat);
        lEye.setMaterial(mat);

        Sphere headMesh = new Sphere(20, 20, HEAD_RADIUS);
        Geometry headGeometry = new Geometry("Box", headMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Cyan);
        mat.setColor("Diffuse", ColorRGBA.Cyan);
        headGeometry.setMaterial(mat);

        /**
         * Upper arms
         */
        //Right Upper Arm
        Cylinder rUArmMesh = new Cylinder(50, 50, UARM_RADIUS, UARM_LENGTH, true);
        Geometry rUArmGeometry = new Geometry("rUArmGeometry", rUArmMesh);
        rUArmGeometry.setLocalRotation(vertRotQuat);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Green);
        mat.setColor("Diffuse", ColorRGBA.Green);
        rUArmGeometry.setMaterial(mat);

        //Right Shoulder
        Sphere rShoulderMesh = new Sphere(50, 50, UARM_RADIUS);
        Geometry rShoulderGeometry = new Geometry("Box", rShoulderMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Green);
        mat.setColor("Diffuse", ColorRGBA.Green);
        rShoulderGeometry.setMaterial(mat);

        //Left Upper Arm
        Cylinder lUArmMesh = new Cylinder(50, 50, UARM_RADIUS, UARM_LENGTH, true);
        Geometry lUArmGeometry = new Geometry("Box", lUArmMesh);
        lUArmGeometry.setLocalRotation(vertRotQuat);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Red);
        mat.setColor("Diffuse", ColorRGBA.Red);
        lUArmGeometry.setMaterial(mat);

        //Left Shoulder
        Sphere lShoulderMesh = new Sphere(50, 50, UARM_RADIUS);
        Geometry lShoulderGeometry = new Geometry("Box", lShoulderMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Red);
        mat.setColor("Diffuse", ColorRGBA.Red);
        lShoulderGeometry.setMaterial(mat);

        /**
         * Lower arms
         */
        //Right Lower Arm
        Cylinder rLArmMesh = new Cylinder(50, 50, LARM_RADIUS, LARM_LENGTH, true);
        Geometry rLArmGeometry = new Geometry("rLArmGeometry", rLArmMesh);
        rLArmGeometry.setLocalRotation(vertRotQuat);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Blue);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        rLArmGeometry.setMaterial(mat);

        //Right Elbow
        Sphere rElbowMesh = new Sphere(50, 50, LARM_RADIUS);
        Geometry rElbowGeometry = new Geometry("Box", rElbowMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Blue);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        rElbowGeometry.setMaterial(mat);

        //Left Lower Arm
        Cylinder lLArmMesh = new Cylinder(50, 50, LARM_RADIUS, LARM_LENGTH, true);
        Geometry lLArmGeometry = new Geometry("Box", lLArmMesh);
        lLArmGeometry.setLocalRotation(vertRotQuat);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Orange);
        mat.setColor("Diffuse", ColorRGBA.Orange);
        lLArmGeometry.setMaterial(mat);

        //Left Elbow
        Sphere lElbowMesh = new Sphere(50, 50, LARM_RADIUS);
        Geometry lElbowGeometry = new Geometry("Box", lElbowMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Orange);
        mat.setColor("Diffuse", ColorRGBA.Orange);
        lElbowGeometry.setMaterial(mat);

        /**
         * Hands
         */
        //Right wrist
        Sphere rWristMesh = new Sphere(50, 50, LARM_RADIUS);
        Geometry rWristGeometry = new Geometry("Box", rWristMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Green);
        mat.setColor("Diffuse", ColorRGBA.Green);
        rWristGeometry.setMaterial(mat);

        //Right hand
        Box rHandMesh = new Box(HAND_THICKNESS, HAND_LENGTH * 2, HAND_WIDTH * 2);
        Geometry rHandGeometry = new Geometry("rHandGeometry", rHandMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Green);
        mat.setColor("Diffuse", ColorRGBA.Green);
        rHandGeometry.setMaterial(mat);


        //Left wrist
        Sphere lWristMesh = new Sphere(50, 50, LARM_RADIUS);
        Geometry lWristGeometry = new Geometry("Box", lWristMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Red);
        mat.setColor("Diffuse", ColorRGBA.Red);
        lWristGeometry.setMaterial(mat);

        //Left hand
        Box lHandMesh = new Box(HAND_THICKNESS, HAND_LENGTH, HAND_WIDTH);
        Geometry lHandGeometry = new Geometry("Box", lHandMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Red);
        mat.setColor("Diffuse", ColorRGBA.Red);
        lHandGeometry.setMaterial(mat);

        /**
         * Legs
         */
        //Upper legs
        Sphere rHipMesh = new Sphere(20, 20, ULEG_RADIUS);
        Geometry rHipGeometry = new Geometry("RHip", rHipMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Green);
        mat.setColor("Diffuse", ColorRGBA.Green);
        rHipGeometry.setMaterial(mat);

        Cylinder rULegMesh = new Cylinder(50, 50, ULEG_RADIUS, ULEG_LENGTH, true);
        Geometry rULegGeometry = new Geometry("Box", rULegMesh);
        rULegGeometry.setLocalRotation(vertRotQuat);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Green);
        mat.setColor("Diffuse", ColorRGBA.Green);
        rULegGeometry.setMaterial(mat);

        Sphere lHipMesh = new Sphere(20, 20, ULEG_RADIUS);
        Geometry lHipGeometry = new Geometry("RHip", lHipMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Red);
        mat.setColor("Diffuse", ColorRGBA.Red);
        lHipGeometry.setMaterial(mat);

        Cylinder lULegMesh = new Cylinder(50, 50, ULEG_RADIUS, ULEG_LENGTH, true);
        Geometry lULegGeometry = new Geometry("Box", lULegMesh);
        lULegGeometry.setLocalRotation(vertRotQuat);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Red);
        mat.setColor("Diffuse", ColorRGBA.Red);
        lULegGeometry.setMaterial(mat);

        //Lower legs
        Sphere rKneeMesh = new Sphere(20, 20, LLEG_RADIUS);
        Geometry rKneeGeometry = new Geometry("RHip", rKneeMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Blue);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        rKneeGeometry.setMaterial(mat);
        
        
        Cylinder rLLegMesh = new Cylinder(50, 50, LLEG_RADIUS, LLEG_LENGTH, true);
        Geometry rLLegGeometry = new Geometry("Box", rLLegMesh);
        rLLegGeometry.setLocalRotation(vertRotQuat);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Blue);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        rLLegGeometry.setMaterial(mat);

        
        Sphere lKneeMesh = new Sphere(20, 20, LLEG_RADIUS);
        Geometry lKneeGeometry = new Geometry("RHip", lKneeMesh);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Orange);
        mat.setColor("Diffuse", ColorRGBA.Orange);
        lKneeGeometry.setMaterial(mat);

        Cylinder lLLegMesh = new Cylinder(50, 50, LLEG_RADIUS, LLEG_LENGTH, true);
        Geometry lLLegGeometry = new Geometry("Box", lLLegMesh);        
        lLLegGeometry.setLocalRotation(vertRotQuat);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Orange);
        mat.setColor("Diffuse", ColorRGBA.Orange);
        lLLegGeometry.setMaterial(mat);
        
        Node torsoNode = new Node("Torso");
        Node shouldersNode = new Node("Shoulders");
        Node rightShoulderNode = new Node("RightShoulder");
        Node rightElbowNode = new Node("RightElbow");
        Node rightWristNode = new Node("RightWrist");
        Node leftShoulderNode = new Node("LeftShoulder");
        Node leftElbowNode = new Node("LeftElbow");
        Node leftWristNode = new Node("LeftWrist");
        Node headNode = new Node("Head");
        Node eyesNode = new Node("Eyes");
        Node pelvisNode = new Node("Pelvis");
        Node rightHipNode = new Node("RightHip");
        Node rightKneeNode = new Node("RightKnee");
        Node leftHipNode = new Node("LeftHip");
        Node leftKneeNode = new Node("LeftKnee");
        
        torsoNode.attachChild(shouldersNode);
        torsoNode.attachChild(headNode);
        headNode.attachChild(eyesNode);
        shouldersNode.attachChild(rightShoulderNode);
        rightShoulderNode.attachChild(rightElbowNode);
        rightElbowNode.attachChild(rightWristNode);
        shouldersNode.attachChild(leftShoulderNode);
        leftShoulderNode.attachChild(leftElbowNode);
        leftElbowNode.attachChild(leftWristNode);
        
        pelvisNode.attachChild(rightHipNode);
        rightHipNode.attachChild(rightKneeNode);
        pelvisNode.attachChild(leftHipNode);
        leftHipNode.attachChild(leftKneeNode);
        pelvisBone = pelvisNode;
        
        torsoNode.attachChild(torsoJointGeometry);
        torsoNode.attachChild(torsoGeometry);
        torsoGeometry.setLocalTranslation(0, TORSO_HEIGHT/2, 0);
        skeletonMap.put(0, torsoNode);
        
        headNode.attachChild(headGeometry);
        headNode.setLocalTranslation(0, TORSO_HEIGHT+HEAD_RADIUS, 0);
        eyesNode.attachChild(lEye);
        eyesNode.attachChild(rEye);
        eyesNode.setLocalTranslation(0, HEAD_RADIUS/2, HEAD_RADIUS*0.8f);
        skeletonMap.put(1, headNode);
        
        shouldersNode.setLocalTranslation(0, 3.5f, 0);
        
        rightShoulderNode.attachChild(rShoulderGeometry);
        rightShoulderNode.attachChild(rUArmGeometry);
        rightShoulderNode.setLocalTranslation(-SHOULDER_WIDTH, 0, 0);
        rUArmGeometry.setLocalTranslation(0, -UARM_LENGTH/2, 0);
        skeletonMap.put(2, rightShoulderNode);
        
        rightElbowNode.attachChild(rElbowGeometry);
        rightElbowNode.attachChild(rLArmGeometry);
        rightElbowNode.setLocalTranslation(0, -UARM_LENGTH, 0);
        rLArmGeometry.setLocalTranslation(0, -LARM_LENGTH/2, 0);
        skeletonMap.put(3, rightElbowNode);

        rightWristNode.attachChild(rWristGeometry);
        rightWristNode.attachChild(rHandGeometry);
        rightWristNode.setLocalTranslation(0, -LARM_LENGTH, 0);
        rHandGeometry.setLocalTranslation(0, -HAND_LENGTH, 0);
        skeletonMap.put(4, rightWristNode);
        skeletonMap.put(13, rHandGeometry);
        
        leftShoulderNode.attachChild(lShoulderGeometry);
        leftShoulderNode.attachChild(lUArmGeometry);
        leftShoulderNode.setLocalTranslation(SHOULDER_WIDTH, 0, 0);
        lUArmGeometry.setLocalTranslation(0, -UARM_LENGTH/2, 0);
        skeletonMap.put(5, leftShoulderNode);
                
        leftElbowNode.attachChild(lElbowGeometry);
        leftElbowNode.attachChild(lLArmGeometry);
        leftElbowNode.setLocalTranslation(0, -UARM_LENGTH, 0);
        lLArmGeometry.setLocalTranslation(0, -LARM_LENGTH/2, 0);
        skeletonMap.put(6, leftElbowNode);

        leftWristNode.attachChild(lWristGeometry);
        leftWristNode.attachChild(lHandGeometry);
        leftWristNode.setLocalTranslation(0, -LARM_LENGTH, 0);
        lHandGeometry.setLocalTranslation(0, -HAND_LENGTH, 0);
        skeletonMap.put(7, leftWristNode);
        
        pelvisNode.attachChild(pelvisGeometry);
        pelvisNode.setLocalTranslation(0, -PELVIS_HEIGHT/2, 0);
        
        rightHipNode.attachChild(rHipGeometry);
        rightHipNode.attachChild(rULegGeometry);
        rightHipNode.setLocalTranslation(-(PELVIS_RADIUS+ULEG_RADIUS)/2, -PELVIS_HEIGHT/2, 0);
        rULegGeometry.setLocalTranslation(0, -ULEG_LENGTH/2, 0);
        skeletonMap.put(8, rightHipNode);
                
        rightKneeNode.attachChild(rKneeGeometry);
        rightKneeNode.attachChild(rLLegGeometry);
        rightKneeNode.setLocalTranslation(0, -ULEG_LENGTH, 0);
        rLLegGeometry.setLocalTranslation(0, -LLEG_LENGTH/2, 0);
        skeletonMap.put(9, rightKneeNode);
        
        leftHipNode.attachChild(lHipGeometry);
        leftHipNode.attachChild(lULegGeometry);
        leftHipNode.setLocalTranslation((PELVIS_RADIUS+ULEG_RADIUS)/2, -PELVIS_HEIGHT/2, 0);
        lULegGeometry.setLocalTranslation(0, -ULEG_LENGTH/2, 0);
        skeletonMap.put(10, leftHipNode);        
        
        leftKneeNode.attachChild(lKneeGeometry);
        leftKneeNode.attachChild(lLLegGeometry);
        leftKneeNode.setLocalTranslation(0, -ULEG_LENGTH, 0);
        lLLegGeometry.setLocalTranslation(0, -LLEG_LENGTH/2, 0);
        skeletonMap.put(11, leftKneeNode);

        HullCollisionShape rHandCollShape = new HullCollisionShape(rHandGeometry.getMesh());
        rHandControl = new RigidBodyControl(rHandCollShape, 0.5f);
        rHandControl.setKinematic(true);
        rHandControl.setRestitution(1f);
        rHandGeometry.addControl(rHandControl);

        HullCollisionShape rForearmCollShape = new HullCollisionShape(rLArmGeometry.getMesh());
        RigidBodyControl rForearmControl = new RigidBodyControl(rForearmCollShape, 1.87f);
        rForearmControl.setKinematic(true);
        rForearmControl.setRestitution(0.5f);
        rLArmGeometry.addControl(rForearmControl);

        HullCollisionShape rArmCollShape = new HullCollisionShape(rUArmGeometry.getMesh());
        RigidBodyControl rArmControl = new RigidBodyControl(rArmCollShape, 3.25f);
        rArmControl.setKinematic(true);
        rArmControl.setRestitution(0.5f);
        rUArmGeometry.addControl(rArmControl);

        bulletAppState.getPhysicsSpace().add(rHandControl);
        bulletAppState.getPhysicsSpace().add(rForearmControl);
        bulletAppState.getPhysicsSpace().add(rArmControl);

        rootNode.attachChild(torsoNode);

        rootNode.attachChild(pelvisNode);

    }

    public void updateModelBonePosition(Quaternion animationQuaternion, int boneIndex) {
        Spatial bone = skeletonMap.get(boneIndex);
        bone.setLocalRotation(animationQuaternion);
    }
    
    public void rotateLegs(Quaternion torsoQuaternion){
        //Vector3f torsoVector3f = torsoQuaternion.getRotationColumn(2).normalizeLocal();
        //Quaternion pelvisQuaternion = new Quaternion().fromAngles(0, -torsoVector3f.angleBetween(Vector3f.UNIT_Z), 0);
        double heading;
        float qx = torsoQuaternion.getX();
        float qy = torsoQuaternion.getY();
        float qz = torsoQuaternion.getZ();
        float qw = torsoQuaternion.getW();
        double test = qx * qy + qz * qw;
        if (test > 0.400) { // singularity at north pole
            //heading = 2 * Math.atan2(qx, qw);
            //Quaternion pelvisQuaternion = new Quaternion(new float[]{0f, (float) heading, 0f});
            //pelvisBone.setLocalRotation(pelvisQuaternion.normalizeLocal());
            return;
        }
        if (test < -0.400) { // singularity at south pole
            //heading = -2 * Math.atan2(qx, qw);
            //Quaternion pelvisQuaternion = new Quaternion(new float[]{0f, (float) heading, 0f});
            //pelvisBone.setLocalRotation(pelvisQuaternion.normalizeLocal());
            return;
        }
        double sqx = qx * qx;
        double sqy = qy * qy;
        double sqz = qz * qz;
        heading = Math.atan2(2 * qy * qw - 2 * qx * qz, 1 - 2 * sqy - 2 * sqz);
        Quaternion pelvisQuaternion = new Quaternion(new float[]{0f, (float) heading, 0f});
        pelvisBone.setLocalRotation(pelvisQuaternion.normalizeLocal());
    }

    public void setShadowMode(RenderQueue.ShadowMode shadowMode) {
        for (Spatial bone : skeletonMap.values()) {
            bone.setShadowMode(shadowMode);
        }
    }

    public void animateBone(int boneIndex, int axisIndex, boolean counterClockWise) {
        if (counterClockWise) {
            animationIndex++;
        } else {
            animationIndex--;
        }
        Quaternion rot = new Quaternion();
        switch (axisIndex) {
            case 0:
                rot = new Quaternion(new float[]{(float) Math.toRadians(animationIndex % 360), 0f, 0f});
                break;
            case 1:
                rot = new Quaternion(new float[]{0f, (float) Math.toRadians(animationIndex % 360), 0f});
                break;
            case 2:
                rot = new Quaternion(new float[]{0f, 0f, (float) Math.toRadians(animationIndex % 360)});
                break;
        }
        //rHandControl.setPhysicsRotation(rot);
        skeletonMap.get(boneIndex).setLocalRotation(rot);
    }

    public void rotateBone(int boneIndex, int axisIndex, double degrees) {
        Quaternion rot = new Quaternion();
        switch (axisIndex) {
            case 0:
                rot = new Quaternion(new float[]{(float) Math.toRadians(degrees), 0f, 0f});
                break;
            case 1:
                rot = new Quaternion(new float[]{0f, (float) Math.toRadians(degrees), 0f});
                break;
            case 2:
                rot = new Quaternion(new float[]{0f, 0f, (float) Math.toRadians(degrees)});
                break;
        }
        Quaternion prevQuat = skeletonMap.get(boneIndex).getLocalRotation();
        skeletonMap.get(boneIndex).setLocalRotation(rot.mult(prevQuat));
    }

    public Quaternion getBoneLocation(int boneIndex) {
        return skeletonMap.get(boneIndex).getWorldRotation();
    }
}
