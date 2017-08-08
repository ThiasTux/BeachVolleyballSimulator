package com.thiastux.beachvolleyhuman.model;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;

public class Ball {

    public final float BALL_RADIUS = 1f;

    public Ball(Node rootNode, AssetManager assetManager) {

        Sphere ballMesh = new Sphere(20, 20, BALL_RADIUS);
        Geometry ballGeometry = new Geometry("Sphere", ballMesh);
        Material ballMat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        ballMat.setBoolean("UseMaterialColors", true);
        ballMat.setColor("Ambient", ColorRGBA.Orange);
        ballMat.setColor("Diffuse", ColorRGBA.Orange);
        ballGeometry.setMaterial(ballMat);

        Node ballNode = new Node("BallNode");
        ballNode.setLocalTranslation(-5, 5, 0);



        ballNode.attachChild(ballGeometry);

        rootNode.attachChild(ballNode);

    }
}
